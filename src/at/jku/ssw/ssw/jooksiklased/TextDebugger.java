package at.jku.ssw.ssw.jooksiklased;

import static at.jku.ssw.ssw.jooksiklased.Message.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.sun.jdi.ArrayReference;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
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
	private int countBreakpoints = 0;
	private boolean terminate = false;

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
		if (countBreakpoints == 0) {
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
					System.out.println(e);

					if (e instanceof BreakpointEvent) {
						vm.suspend();
						BreakpointEvent be = (BreakpointEvent) e;

						print(HIT_BREAKPOINT, be.thread(), be.location()
								.method(), be.location().lineNumber(), 0);

						// printVars(be.thread().frame(0));
						countBreakpoints--;
						return;
					} else if (e instanceof VMDisconnectEvent) {
						terminate = true;
					} else {
						System.out.println(e);
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
			req.addClassFilter("Test");
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
						vm.resume();
						curThread.resume();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setBreakpoint(Location loc) {
		final Method method = loc.method();
		print(SET_BREAKPOINT, method, loc.lineNumber());

		try {
			// if pc is already at breakpoint
			if (curThread.frame(0).location().equals(loc)) {
				print(HIT_BREAKPOINT, curThread, method, loc.lineNumber(), 0);
			} else {
				EventRequestManager reqManager = vm.eventRequestManager();
				BreakpointRequest req = reqManager.createBreakpointRequest(loc);
				req.enable();
				countBreakpoints++;
			}
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

	private static void printVars(StackFrame frame) {
		try {
			Iterator<LocalVariable> iter = frame.visibleVariables().iterator();
			while (iter.hasNext()) {
				LocalVariable v = (LocalVariable) iter.next();
				System.out.print(v.name() + ": " + v.type().name() + " = ");
				printValue(frame.getValue(v));
				System.out.println();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void printValue(Value val) {
		if (val instanceof IntegerValue) {
			System.out.print(((IntegerValue) val).value() + " ");
		} else if (val instanceof StringReference) {
			System.out.print(((StringReference) val).value() + " ");
		} else if (val instanceof ArrayReference) {
			Iterator<Value> iter = ((ArrayReference) val).getValues()
					.iterator();
			while (iter.hasNext()) {
				printValue((Value) iter.next());
			}
		} else {
			throw new UnsupportedOperationException();
		}
	}

	private static void printLocation(Location loc) {
		System.out.println(loc.lineNumber() + ", " + loc.codeIndex());
	}

	private void perform(final String cmd) throws InterruptedException {
		StringTokenizer st = new StringTokenizer(cmd, " ");

		switch (st.nextToken()) {
		case "run":
			run();
			break;

		case "cont":
			cont();
			break;

		case "print":
		case "dump":
		case "threads":
		case "thread":
		case "where":
			throw new UnsupportedOperationException();

		case "stop":
			String className = null;
			String methodName = null;
			int lineNumber = -1;

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

			try {
				// if VM is already loaded
				if (isLoaded()) {

					// find class
					final List<ReferenceType> classes = vm
							.classesByName(className);

					assert classes.size() == 1;
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
					print(DEFER_BREAKPOINT, className, methodName);
					pendingOperations.add(cmd);
				}
			} catch (Exception e) {
				e.printStackTrace();
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
			// debugger.methodEntry();
			debugger.ui();
		} catch (IOException | IllegalConnectorArgumentsException e) {
			e.printStackTrace();
		}
	}
}