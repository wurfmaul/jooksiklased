package at.jku.ssw.ssw.jooksiklased;

import static at.jku.ssw.ssw.jooksiklased.Message.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.ByteValue;
import com.sun.jdi.CharValue;
import com.sun.jdi.ClassObjectReference;
import com.sun.jdi.DoubleValue;
import com.sun.jdi.FloatValue;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.LongValue;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ShortValue;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.Connector.Argument;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.StepRequest;
import com.sun.jdi.request.VMDeathRequest;
import com.sun.tools.jdi.ObjectReferenceImpl;

@SuppressWarnings("unused")
public class TextDebugger {
	/**
	 * If an operation requires an unloaded class to be loaded, the operations
	 * can be stored in this list in order to perform immediately after class
	 * loading.
	 */
	private final Queue<String> pendingOperations;
	private final VirtualMachine vm;
	private final EventRequestManager reqManager;

	private ThreadReference curThread;
	private int setBreakpoints = 0;
	private int hitBreakpoints = 0;
	private boolean terminate = false;
	private String debuggee = null;

	private TextDebugger() throws IOException,
			IllegalConnectorArgumentsException {

		// make space for pending operations
		pendingOperations = new ConcurrentLinkedQueue<>();

		// find attaching connector
		AttachingConnector con = null;
		Iterator<Connector> iter = Bootstrap.virtualMachineManager()
				.allConnectors().iterator();
		while (iter.hasNext()) {
			Connector x = (Connector) iter.next();
			if (x.name().equals("com.sun.jdi.SocketAttach"))
				con = (AttachingConnector) x;
		}

		// configure connector
		Map<String, Argument> args = con.defaultArguments();
		((Connector.Argument) args.get("port")).setValue("8000");

		// Establish VirtualMachine
		vm = con.attach(args);

		// Establish Request Manager
		reqManager = vm.eventRequestManager();

		vm.suspend();

		// find current thread
		for (ThreadReference t : vm.allThreads()) {
			if (t.name().equals("main")) {
				curThread = t;
				break;
			}
		}
	}

	/**
	 * This method continues to next breakpoint if the VM is started.
	 */
	private void cont() {
		if (!isLoaded()) {
			print(VM_NOT_RUNNING);
			return;
		}

		// if there are no more breakpoints
		if (setBreakpoints == hitBreakpoints) {
			vm.dispose();
		} else {
			curThread.resume();
			vm.resume();
		}

		EventQueue q = vm.eventQueue();

		try {
			while (true) {
				EventSet events = q.remove();
				EventIterator iter = events.eventIterator();
				while (iter.hasNext()) {
					Event e = iter.nextEvent();
					if (e instanceof BreakpointEvent) {
						vm.suspend();
						BreakpointEvent be = (BreakpointEvent) e;

						print(HIT_BREAKPOINT, be.thread().name(), be.location()
								.method(), be.location().lineNumber(), 0);

						hitBreakpoints++;
						return;
					} else if (e instanceof VMDisconnectEvent) {
						// tell UI to terminate
						terminate = true;
					} else {
						// unclassified event
						// System.out.println(e);
						vm.resume();
					}
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (VMDisconnectedException e) {
			print(EXIT);
		}
	}

	/**
	 * This method starts the VM and stops at first method entry.
	 */
	private void run() {
		if (isLoaded()) {
			print(VM_RUNNING);
			return;
		}

		try {
			MethodEntryRequest req = reqManager.createMethodEntryRequest();
			if (debuggee != null)
				req.addClassFilter(debuggee);
			req.addThreadFilter(curThread);
			req.enable();

			vm.resume();
			curThread.resume();
			EventQueue q = vm.eventQueue();

			while (true) {
				EventSet events = q.remove();
				EventIterator iter = events.eventIterator();
				while (iter.hasNext()) {
					Event e = iter.nextEvent();
					if (e instanceof MethodEntryEvent) {
						vm.suspend();
						while (pendingOperations.size() > 0) {
							perform(pendingOperations.remove());
						}
						return;
					} else {
						// System.out.println(e);
						vm.resume();
						curThread.resume();
					}
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void setBreakpoint(Location loc) {
		final Method method = loc.method();
		print(SET_BREAKPOINT, method, loc.lineNumber());

		try {
			if (curThread.frame(0).location().equals(loc)) {
				// pc is already at breakpoint
				print(HIT_BREAKPOINT, curThread.name(), method,
						loc.lineNumber(), 0);
				hitBreakpoints++;
			} else {
				EventRequestManager reqManager = vm.eventRequestManager();
				BreakpointRequest req = reqManager.createBreakpointRequest(loc);
				req.enable();
			}
			setBreakpoints++;
		} catch (IncompatibleThreadStateException e) {
			e.printStackTrace();
		}
	}

	private void stepOver(ThreadReference thread) {
		try {
			StepRequest req = reqManager.createStepRequest(thread,
					StepRequest.STEP_LINE, StepRequest.STEP_OVER);
			req.addClassFilter("*Test"); // create step requests only in class
											// Test
			req.addCountFilter(1); // create step event after 1 step
			req.enable();
			vm.resume();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static String valueToString(Value val) {
		if (val instanceof BooleanValue) {
			return ((BooleanValue) val).value() + "";
		} else if (val instanceof ByteValue) {
			return ((ByteValue) val).value() + "";
		} else if (val instanceof CharValue) {
			return ((CharValue) val).value() + "";
		} else if (val instanceof DoubleValue) {
			return ((DoubleValue) val).value() + "";
		} else if (val instanceof FloatValue) {
			return ((FloatValue) val).value() + "";
		} else if (val instanceof IntegerValue) {
			return ((IntegerValue) val).value() + "";
		} else if (val instanceof LongValue) {
			return ((LongValue) val).value() + "";
		} else if (val instanceof ShortValue) {
			return ((ShortValue) val).value() + "";
		} else if (val instanceof StringReference) {
			return ((StringReference) val).value() + "";
		} else if (val instanceof ArrayReference) {
			final StringBuilder sb = new StringBuilder();
			final ArrayReference arr = (ArrayReference) val;
			sb.append("instance of " + arr.type().name() + "(id="
					+ arr.uniqueID() + ")\n  +-> [");
			Iterator<Value> iter = arr.getValues().iterator();
			while(iter.hasNext()) {
				sb.append(valueToString(iter.next()));
				if (iter.hasNext())
					sb.append(", ");
			}
			sb.append("]");
			return sb.toString();
		} else if (val instanceof ObjectReferenceImpl) {
			final ObjectReferenceImpl obj = (ObjectReferenceImpl) val;
			return "instance of " + obj.type().name() + "(id=" + obj.uniqueID()
					+ ")";
		} else {
			throw new UnsupportedOperationException(val.getClass().getName());
		}
	}

	private static void printLocation(Location loc) {
		System.out.println(loc.lineNumber() + ", " + loc.codeIndex());
	}

	private void perform(final String cmd) throws InterruptedException {
		StringTokenizer st = new StringTokenizer(cmd, " ");
		String className = null;
		String methodName = null;
		int lineNumber = -1;

		switch (st.nextToken()) {
		case "run":
			run();
			// fall through unless there were no breakpoints hit
			if (hitBreakpoints > 0)
				break;

		case "cont":
			cont();
			break;

		case "print":
			try {
				className = st.nextToken(".");
				StackFrame curFrame = curThread.frame(0);
				if (st.hasMoreTokens()) {
					// fields
					final String varName = st.nextToken().trim();
					for (LocalVariable var : curFrame.visibleVariables()) {
						String value = valueToString(curFrame.getValue(var));
						print(VAR, var.typeName(), var.name(), value);
					}

				} else {
					// locals
					final String varName = className.trim();
					LocalVariable var = curFrame.visibleVariableByName(varName);
					if (var != null) {
						String value = valueToString(curFrame.getValue(var));
						print(VAR, var.typeName(), var.name(), value);
					} else {
						print(UNKNOWN, varName);
					}
				}
			} catch (AbsentInformationException e) {
				e.printStackTrace();
			} catch (IncompatibleThreadStateException e) {
				e.printStackTrace();
			} catch (NoSuchElementException e) {
				print(INVALID_CMD, cmd);
			}
			break;
		case "locals":
		case "dump":
		case "threads":
		case "thread":
		case "where":
			throw new UnsupportedOperationException();

		case "stop":

			try {
				switch (st.nextToken()) {
				case "in": // e.g. "stop in MyClass.main"
					className = st.nextToken(".").trim();
					methodName = st.nextToken().trim();
					break;

				case "at": // e.g. "stop at MyClass:22"
					className = st.nextToken(":").trim();
					lineNumber = Integer.parseInt(st.nextToken());
					break;

				default:
					throw new UnsupportedOperationException();
				}

				// if VM is already loaded
				if (isLoaded()) {

					// find class
					final List<ReferenceType> classes = vm
							.classesByName(className);

					assert classes.size() > 0 : className + " not found";
					assert classes.size() <= 1;

					final ReferenceType clazz = classes.get(0);

					if (methodName == null) {
						// find method from line number
						assert lineNumber != -1;

						// find location
						final List<Location> locs = clazz
								.locationsOfLine(lineNumber);
						assert locs.size() == 1;
						setBreakpoint(locs.get(0));

					} else {
						// find method from method name
						assert methodName != null;

						final List<Method> methods = clazz
								.methodsByName(methodName);
						assert methods.size() <= 1;

						if (methods.size() == 1) {
							Method method = methods.get(0);

							// get first executable line
							final List<Location> locs = method
									.allLineLocations();
							assert locs.size() > 0;
							setBreakpoint(locs.get(0));
						} else {
							Message.print(Message.NO_METHOD, className,
									methodName, methodName, className);
						}
					}

				} else {
					// class is not yet loaded
					if (methodName == null)
						print(DEFER_BREAKPOINT_LOC, className, lineNumber);
					else
						print(DEFER_BREAKPOINT, className, methodName);

					pendingOperations.add(cmd);
				}
			} catch (AbsentInformationException e) {
				e.printStackTrace();
			} catch (NoSuchElementException e) {
				print(INVALID_CMD, cmd);
			}
			break;

		case "clear":
		case "step":
		case "next":
		case "catch":
		case "ignore":
			throw new UnsupportedOperationException("catch / ignore");

		default:
			// TODO list of commands
			System.out.println("Usage:");
			System.out.print("run / cont / print / dump / threads / ");
			System.out.print("thread / where / stop / clear / step / ");
			System.out.println("next / catch / ignore");
		}

		if (className != null)
			debuggee = className.trim();

		if (st.hasMoreTokens()) {
			System.err.print("Warning: No use for arguments: ");
			System.err.print(st.nextToken());
			while (st.hasMoreTokens())
				System.err.print(", " + st.nextToken());
			System.err.println();
		}
	}

	/**
	 * True if VM is loaded, false otherwise.
	 */
	private boolean isLoaded() {
		try {
			return curThread.frameCount() > 0;
		} catch (IncompatibleThreadStateException e) {
			System.err.println("isLoaded() threw "
					+ e.getClass().getSimpleName());
		}
		return false;
	}

	private int ui() {
		final BufferedReader in = new BufferedReader(new InputStreamReader(
				System.in));
		int retValue = 0;
		String cmd;

		while (!terminate) {
			try {
				System.out.print("> ");
				cmd = in.readLine().trim();
				switch (cmd) {
				case "quit":
				case "exit":
				case "q":
					vm.dispose();
					return 0;
				default:
					perform(cmd);
				}
			} catch (IOException | InterruptedException e) {
				retValue = -1;
				break;
			}
		}
		try {
			in.close();
		} catch (IOException e) {
		}
		return retValue;
	}

	public static void main(String[] arguments) {
		try {
			TextDebugger debugger = new TextDebugger();
			debugger.ui();
		} catch (IOException | IllegalConnectorArgumentsException e) {
			e.printStackTrace();
		}
	}
}