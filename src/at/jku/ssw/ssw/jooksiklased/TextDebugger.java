package at.jku.ssw.ssw.jooksiklased;

import static at.jku.ssw.ssw.jooksiklased.Message.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import com.sun.jdi.Field;
import com.sun.jdi.FloatValue;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.LongValue;
import com.sun.jdi.Method;
import com.sun.jdi.Mirror;
import com.sun.jdi.ObjectReference;
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
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.Connector.Argument;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.VMStartException;
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
	private final VirtualMachine vm;

	private Queue<String> pendingOperations;
	private EventRequestManager reqManager;
	private ThreadReference curThread;
	private int setBreakpoints = 0;
	private int hitBreakpoints = 0;
	private Location firstBreakpoint = null;
	private boolean terminate = false;
	private String debuggee = null;

	/**
	 * The attaching debugger
	 * 
	 * @throws IOException
	 * @throws IllegalConnectorArgumentsException
	 */
	public TextDebugger() throws IOException,
			IllegalConnectorArgumentsException {

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
		((Argument) args.get("port")).setValue("8000");

		// Establish VirtualMachine
		vm = con.attach(args);

		init();
	}

	/**
	 * The launching debugger
	 * 
	 * @param debuggee
	 * @throws IOException
	 * @throws IllegalConnectorArgumentsException
	 * @throws VMStartException
	 */
	public TextDebugger(final String debuggee) throws IOException,
			IllegalConnectorArgumentsException, VMStartException {

		this.debuggee = debuggee;

		// establish connection
		LaunchingConnector con = Bootstrap.virtualMachineManager()
				.defaultConnector();
		Map<String, Argument> args = con.defaultArguments();
		((Argument) args.get("main")).setValue(debuggee);
		vm = con.launch(args);

		init();
	}

	private void setBreakpoint(Location loc) {
		final Method method = loc.method();
		print(SET_BREAKPOINT, method, loc.lineNumber());

		try {
			if (curThread.frame(0).location().equals(loc)) {
				// pc is already at breakpoint
				firstBreakpoint = loc;
				hitBreakpoints++;
			}
			EventRequestManager reqManager = vm.eventRequestManager();
			BreakpointRequest req = reqManager.createBreakpointRequest(loc);
			req.enable();
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

	/**
	 * This method continues to next breakpoint if the VM is started.
	 */
	private void performCont() {
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

	private void performFields(final String className) {
		final ReferenceType clazz = findClass(className);
		for (Field f : clazz.visibleFields()) {
			if (f.isStatic()) {
				String value = valueToString(clazz.getValue(f));
				print(VAR, f.typeName(), f.name(), value);
			} else {
				print(FIELD, f.typeName(), f.name());
			}
		}
	}

	private void performLocals() throws IncompatibleThreadStateException,
			AbsentInformationException {

		final StackFrame curFrame = curThread.frame(0);
		for (LocalVariable var : curFrame.visibleVariables()) {
			String value = valueToString(curFrame.getValue(var));
			print(VAR, var.typeName(), var.name(), value);
		}
	}

	private void performPrintField(final String className,
			final String fieldName) {

		ReferenceType clazz = findClass(className);
		Field f = clazz.fieldByName(fieldName);
		if (f.isStatic())
			print(VAR, f.typeName(), f.name(), clazz.getValue(f));
		else
			print(NO_FIELD, fieldName, className);
	}

	private void performPrintLocal(final String varName)
			throws IncompatibleThreadStateException, AbsentInformationException {

		final StackFrame curFrame = curThread.frame(0);
		final LocalVariable var = curFrame.visibleVariableByName(varName);
		if (var != null) {
			final String value = valueToString(curFrame.getValue(var));
			print(VAR, var.typeName(), var.name(), value);
		} else {
			print(UNKNOWN, varName);
		}
	}

	/**
	 * This method starts the VM and stops at first method entry.
	 */
	private void performRun() {
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

			boolean exit = false;

			while (!exit) {
				EventSet events = q.remove();
				EventIterator iter = events.eventIterator();
				while (iter.hasNext()) {
					Event e = iter.nextEvent();
					if (e instanceof MethodEntryEvent) {
						vm.suspend();
						while (pendingOperations.size() > 0) {
							perform(pendingOperations.remove());
						}
						exit = true;
						break;
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

		if (firstBreakpoint != null) {
			print(HIT_BREAKPOINT, curThread.name(), firstBreakpoint.method(),
					firstBreakpoint.lineNumber(), 0);
		} else {
			performCont();
		}
	}

	/**
	 * Find location by class name and method name and set breakpoint.
	 * 
	 * @param className
	 * @param lineNumber
	 */
	private void performStop(String className, int lineNumber) {
		assert lineNumber != -1;

		// find location
		try {
			List<Location> locs = findClass(className).locationsOfLine(
					lineNumber);
			assert locs.size() == 1;
			setBreakpoint(locs.get(0));
		} catch (AbsentInformationException e) {
			e.printStackTrace(); // EMPTY_CLASS ?
		}
	}

	/**
	 * Find location by class name and line number and set breakpoint.
	 * 
	 * @param className
	 * @param methodName
	 */
	private void performStop(String className, String methodName) {
		assert methodName != null;

		final List<Method> methods = findClass(className).methodsByName(
				methodName);
		assert methods.size() <= 1;

		if (methods.size() == 1) {
			Method method = methods.get(0);

			// get first executable line
			try {
				List<Location> locs = method.allLineLocations();
				assert locs.size() > 0;
				setBreakpoint(locs.get(0));
			} catch (AbsentInformationException e) {
				e.printStackTrace(); // METHOD_EMPTY ?
			}
		} else {
			Message.print(Message.NO_METHOD, className, methodName, methodName,
					className);
		}
	}

	/**
	 * True if VM is loaded, false otherwise.
	 */
	private boolean isLoaded() {
		try {
			return curThread.frameCount() > 0;
		} catch (IncompatibleThreadStateException e) {
		}
		return true;
	}

	private ReferenceType findClass(final String className) {
		final List<ReferenceType> classes = vm.classesByName(className);

		assert classes.size() > 0 : className + " not found";
		assert classes.size() <= 1;

		return classes.get(0);
	}

	/**
	 * Initializes fields that re necessary for both constructors.
	 */
	private void init() {
		// make space for pending operations
		pendingOperations = new ConcurrentLinkedQueue<>();

		// Establish Request Manager
		reqManager = vm.eventRequestManager();

		// redirect IO streams
		Process proc = vm.process();
		new Redirection(proc.getErrorStream(), System.err).start();
		new Redirection(proc.getInputStream(), System.out).start();

		vm.suspend();

		// find current thread
		for (ThreadReference t : vm.allThreads()) {
			if (t.name().equals("main")) {
				curThread = t;
				break;
			}
		}
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
			} catch (IOException e) {
				retValue = -1;
				break;
			} catch (VMDisconnectedException e) {
				break;
			}
		}
		try {
			in.close();
		} catch (IOException e) {
		}
		return retValue;
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
			final ArrayReference arr = (ArrayReference) val;
			final String type = arr.type().name();
			final StringBuilder sb = new StringBuilder();
			sb.append("instance of " + type + "(id=" + arr.uniqueID() + ")");
			// print elements
//			sb.append("\n  +-> [");
//			Iterator<Value> iter = arr.getValues().iterator();
//			while (iter.hasNext()) {
//				sb.append(valueToString(iter.next()));
//				if (iter.hasNext())
//					sb.append(", ");
//			}
//			sb.append("]");
			return sb.toString();
		} else if (val instanceof ObjectReferenceImpl) {
			final ObjectReferenceImpl obj = (ObjectReferenceImpl) val;
			return "instance of " + obj.type().name() + "(id=" + obj.uniqueID()
					+ ")";
		} else {
			throw new UnsupportedOperationException(val.getClass().getName());
		}
	}

	public void perform(final String cmd) {
		StringTokenizer st = new StringTokenizer(cmd, " .");
		String className = null;
		String methodName = null;
		int lineNumber = -1;
		StackFrame curFrame;

		try {
			switch (st.nextToken()) {
			case "run":
				performRun();
				break;

			case "cont":
				performCont();
				break;

			case "print":
				className = st.nextToken();
				if (st.hasMoreTokens()) {
					performPrintField(className.trim(), st.nextToken().trim());
				} else {
					performPrintLocal(className.trim());
				}
				break;

			case "locals":
				performLocals();
				break;

			case "fields":
				performFields(st.nextToken().trim());
				break;

			case "dump":
			case "threads":
			case "thread":
			case "where":
				throw new UnsupportedOperationException();

			case "stop":
				switch (st.nextToken()) {
				case "in": // e.g. "stop in MyClass.main"
					className = st.nextToken().trim();
					methodName = st.nextToken().trim();
					if (isLoaded()) {
						performStop(className, methodName);
					} else {
						print(DEFER_BREAKPOINT, className, methodName);
						pendingOperations.add(cmd);
					}
					break;

				case "at": // e.g. "stop at MyClass:22"
					className = st.nextToken(":").trim();
					lineNumber = Integer.parseInt(st.nextToken());
					if (isLoaded()) {
						performStop(className, lineNumber);
					} else {
						print(DEFER_BREAKPOINT_LOC, className, lineNumber);
						pendingOperations.add(cmd);
					}
					break;

				default:
					throw new UnsupportedOperationException();
				}
				break;

			case "clear":
			case "step":
			case "next":
			case "catch":
			case "ignore":
				throw new UnsupportedOperationException("catch / ignore");

			default:
				print(USAGE);
			}
		} catch (AbsentInformationException e) {
			e.printStackTrace();
		} catch (IncompatibleThreadStateException e) {
			e.printStackTrace();
		} catch (NoSuchElementException e) {
			print(INVALID_CMD, cmd);
		}

		if (className != null)
			debuggee = className.trim();

		if (st.hasMoreTokens()) {
			StringBuilder sb = new StringBuilder();
			while (st.hasMoreTokens()) {
				sb.append(st.nextToken());
				sb.append(" ");
			}
			print(TOO_MANY_ARGS, sb.toString().trim());
		}
	}

	public static void main(String[] args) {
		try {
			TextDebugger debugger;
			if (args.length == 0)
				debugger = new TextDebugger();
			else
				debugger = new TextDebugger(args[0]);
			debugger.ui();
		} catch (IOException | IllegalConnectorArgumentsException e) {
			e.printStackTrace();
		} catch (VMStartException e) {
			e.printStackTrace();
		}
	}
}