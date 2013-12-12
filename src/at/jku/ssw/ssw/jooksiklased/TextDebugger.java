package at.jku.ssw.ssw.jooksiklased;

import static at.jku.ssw.ssw.jooksiklased.Message.BREAKPOINT_NOT_FOUND;
import static at.jku.ssw.ssw.jooksiklased.Message.DEFER_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.EXIT;
import static at.jku.ssw.ssw.jooksiklased.Message.FIELD;
import static at.jku.ssw.ssw.jooksiklased.Message.HIT_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.INVALID_CMD;
import static at.jku.ssw.ssw.jooksiklased.Message.LIST_BREAKPOINTS;
import static at.jku.ssw.ssw.jooksiklased.Message.NO_FIELD;
import static at.jku.ssw.ssw.jooksiklased.Message.REMOVE_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.SET_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.STEP;
import static at.jku.ssw.ssw.jooksiklased.Message.TOO_MANY_ARGS;
import static at.jku.ssw.ssw.jooksiklased.Message.TRACE;
import static at.jku.ssw.ssw.jooksiklased.Message.UNKNOWN;
import static at.jku.ssw.ssw.jooksiklased.Message.USAGE;
import static at.jku.ssw.ssw.jooksiklased.Message.VAR;
import static at.jku.ssw.ssw.jooksiklased.Message.VM_NOT_RUNNING;
import static at.jku.ssw.ssw.jooksiklased.Message.VM_RUNNING;
import static at.jku.ssw.ssw.jooksiklased.Message.print;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Stack;
import java.util.StringTokenizer;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.ByteValue;
import com.sun.jdi.CharValue;
import com.sun.jdi.DoubleValue;
import com.sun.jdi.Field;
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
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.VMStartException;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMStartEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.MethodExitRequest;
import com.sun.jdi.request.StepRequest;
import com.sun.tools.jdi.ObjectReferenceImpl;

/**
 * Main class of debugger. Contains the main loop of the interface
 * 
 * @author Wolfgang Küllinger (0955711)
 * 
 */
public class TextDebugger {
	/** The environment for the debuggee. */
	private final VirtualMachine vm;
	/** The central request manager of the given vm. */
	private EventRequestManager reqManager;
	/** A reference to the current thread. */
	private ThreadReference curThread;

	/** Stack of currently active methods. */
	private Stack<Method> methodStack;
	/** Breakpoints that were not yet set because vm is not loaded. */
	private Queue<Breakpoint> pendingBreakpoints;
	/** Flag for the main loop, whether to exit at next run or not. */
	private boolean terminate = false;
	/** The name of the debuggee class. */
	private String debuggee = null;

	/**
	 * The attaching debugger needs the debuggee to be run itself using a
	 * specific port (which is here 8000 by default).
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
	 * The launching debugger does not need the debuggee to be started by
	 * itself, but starts the vm itself.
	 * 
	 * @param debuggee
	 *            The name of the class which is about to be debugged.
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

	/**
	 * Set a new breakpoint at given location.
	 * 
	 * @param loc
	 *            Location to set breakpoint at.
	 */
	private void setBreakpoint(final Location loc) {
		final Method method = loc.method();
		print(SET_BREAKPOINT, method, loc.lineNumber());
		reqManager.createBreakpointRequest(loc).enable();
	}

	/**
	 * Enable possibility to step by one.
	 */
	private void setStep() {
		final StepRequest req = reqManager.createStepRequest(curThread,
				StepRequest.STEP_LINE, StepRequest.STEP_OVER);
		req.addCountFilter(1);
		req.enable();
	}

	/**
	 * Deletes a given breakpoint from list of pending breakpoints or from the
	 * request manager's list.
	 * 
	 * @param breakpoint
	 */
	private void performClear(final Breakpoint breakpoint) {
		if (!isLoaded()) {
			// find within pending breakpoints
			if (pendingBreakpoints.remove(breakpoint)) {
				print(REMOVE_BREAKPOINT, breakpoint);
			} else {
				print(BREAKPOINT_NOT_FOUND, breakpoint);
			}
			return;
		}

		// find set breakpoint
		for (BreakpointRequest req : reqManager.breakpointRequests()) {
			final String className = req.location().declaringType().name();
			final String methodName = req.location().method().name();
			final int lineNumber = req.location().lineNumber();

			if (className.equals(breakpoint.className)
					&& (methodName.equals(breakpoint.methodName) || lineNumber == breakpoint.lineNumber)) {
				reqManager.deleteEventRequest(req);
				print(REMOVE_BREAKPOINT, breakpoint);
				return;
			}
		}

		// not found
		print(BREAKPOINT_NOT_FOUND, breakpoint);
	}

	/**
	 * This method continues to next breakpoint or step event if the VM is
	 * started.
	 */
	private void performCont() {
		if (!isLoaded()) {
			print(VM_NOT_RUNNING);
			return;
		}

		// let threads run
		curThread.resume();
		vm.resume();

		// listen for events
		try {
			while (true) {
				final EventSet events = vm.eventQueue().remove();
				final EventIterator iter = events.eventIterator();
				while (iter.hasNext()) {
					Event e = iter.nextEvent();
					if (e instanceof BreakpointEvent) {
						final BreakpointEvent be = (BreakpointEvent) e;
						final Location loc = be.location();
						print(HIT_BREAKPOINT, be.thread().name(), loc.method(),
								loc.lineNumber(), loc.codeIndex());
						return;
					} else if (e instanceof StepEvent) {
						final StepEvent se = (StepEvent) e;
						final Location loc = se.location();
						print(STEP, ((StepEvent) e).thread().name(),
								loc.method(), loc.lineNumber(), loc.codeIndex());
						// delete old step
						reqManager.deleteEventRequest(se.request());
						return;
					} else if (e instanceof VMDisconnectEvent) {
						// tell UI to terminate
						terminate = true;
						break;
					} else if (e instanceof MethodEntryEvent) {
						// push entered method on method stack
						final MethodEntryEvent mee = (MethodEntryEvent) e;
						methodStack.push(mee.method());

						// do not resume vm if a breakpoint is reached
						boolean isBreakpoint = true;
						for (BreakpointRequest bpr : reqManager
								.breakpointRequests()) {
							if (bpr.location().equals(mee.location())) {
								isBreakpoint = false;
							}
						}
						if (isBreakpoint) {
							vm.resume();
						}

					} else if (e instanceof MethodExitEvent) {
						// pop exited method from method stack
						final Method lastMet = methodStack.pop();
						assert ((MethodExitEvent) e).method().equals(lastMet);
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
	 * Reads fields from given class if vm is loaded.
	 * 
	 * @param className
	 *            Name of class of which the fields are to be printed.
	 */
	private void performFields(final String className) {
		final ReferenceType clazz = findClass(className);
		for (Field f : clazz.visibleFields()) {
			if (f.isStatic()) {
				String value = valueToString(clazz.getValue(f), false);
				print(VAR, f.typeName(), f.name(), value);
			} else {
				print(FIELD, f.typeName(), f.name());
			}
		}
	}

	/**
	 * Reads visible local variables at certain position, depending on the
	 * current frame.
	 */
	private void performLocals() {
		try {
			final StackFrame curFrame = curThread.frame(0);
			for (LocalVariable var : curFrame.visibleVariables()) {
				String value = valueToString(curFrame.getValue(var), false);
				print(VAR, var.typeName(), var.name(), value);
			}
		} catch (IncompatibleThreadStateException e) {
			e.printStackTrace();
		} catch (AbsentInformationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Prints a list of all set breakpoints.
	 */
	private void performPrintBreakpoints() {
		final StringBuilder sb = new StringBuilder();

		final Iterator<BreakpointRequest> iter = reqManager
				.breakpointRequests().iterator();
		while (iter.hasNext()) {
			final Location loc = iter.next().location();
			sb.append("\t");
			sb.append(loc.method().declaringType().name());
			sb.append(".");
			sb.append(loc.method().name());
			sb.append(": ");
			sb.append(loc.lineNumber());
			if (iter.hasNext()) {
				sb.append("\n");
			}
		}
		print(LIST_BREAKPOINTS, sb.toString());
	}

	/**
	 * Prints one specific field of given class.
	 * 
	 * @param className
	 *            Name of class which contains the field
	 * @param fieldName
	 *            Name of field which is about to be displayed.
	 * @param dump
	 *            True if complex structures like classes or arrays should be
	 *            printed including their contents. False if only the name and
	 *            id should be printed.
	 */
	private void performPrintField(final String className,
			final String fieldName, final boolean dump) {
		final ReferenceType clazz = findClass(className);
		final Field f = clazz.fieldByName(fieldName);
		if (f.isStatic())
			print(VAR, f.typeName(), f.name(),
					valueToString(clazz.getValue(f), dump));
		else
			print(NO_FIELD, fieldName, className);
	}

	/**
	 * Prints one specific field of given class.
	 * 
	 * @param varName
	 *            Name of local variable which is about to be displayed.
	 * @param dump
	 *            True if complex structures like classes or arrays should be
	 *            printed including their contents. False if only the name and
	 *            id should be printed.
	 */
	private void performPrintLocal(final String varName, final boolean dump) {
		try {
			StackFrame curFrame = curThread.frame(0);
			final LocalVariable var = curFrame.visibleVariableByName(varName);
			if (var != null) {
				final String value = valueToString(curFrame.getValue(var), dump);
				print(VAR, var.typeName(), var.name(), value);
			} else {
				print(UNKNOWN, varName);
			}
		} catch (IncompatibleThreadStateException e) {
			e.printStackTrace();
		} catch (AbsentInformationException e) {
			e.printStackTrace();
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

		// supervise entered methods
		MethodEntryRequest req = reqManager.createMethodEntryRequest();
		if (debuggee != null)
			req.addClassFilter(debuggee);
		req.addThreadFilter(curThread);
		req.enable();

		// supervise exited methods
		MethodExitRequest meReq = reqManager.createMethodExitRequest();
		if (debuggee != null)
			meReq.addClassFilter(debuggee);
		meReq.addThreadFilter(curThread);
		meReq.enable();

		vm.resume();
		boolean exit = false;

		while (!exit) {
			try {
				final EventSet events = vm.eventQueue().remove();
				final EventIterator iter = events.eventIterator();
				while (iter.hasNext()) {
					Event e = iter.nextEvent();
					if (e instanceof MethodEntryEvent) {
						// push entered method to method stack
						methodStack.push(((MethodEntryEvent) e).method());
						// init done, tell loop to stop
						exit = true;
						curThread.suspend();
						break;
					} else {
						assert e instanceof VMStartEvent;
					}
					vm.resume();
				}
			} catch (InterruptedException e) {
				break;
			}
		}

		// perform pending operations
		while (pendingBreakpoints.size() > 0) {
			performStop(pendingBreakpoints.remove());
		}

		if (curThread.isAtBreakpoint()) {
			try {
				final Location bp = curThread.frame(0).location();
				print(HIT_BREAKPOINT, curThread.name(), bp.method(),
						bp.lineNumber(), bp.codeIndex());
			} catch (IncompatibleThreadStateException e) {
				e.printStackTrace();
			}
		} else {
			performCont();
		}
	}

	/**
	 * Performs a single step throughout the source file.
	 */
	private void performStep() {
		setStep();
		performCont();
	}

	/**
	 * Find location by given breakpoint dummy and set breakpoint.
	 * 
	 * @param breakpoint
	 *            A dummy object containing all information required to set a
	 *            breakpoint.
	 */
	private void performStop(final Breakpoint breakpoint) {
		final String className = breakpoint.className;
		final String methodName = breakpoint.methodName;
		final int lineNumber = breakpoint.lineNumber;

		if (lineNumber >= 0) {
			// find location by line number
			try {
				List<Location> locs = findClass(className).locationsOfLine(
						lineNumber);
				assert locs.size() == 1;
				setBreakpoint(locs.get(0));
			} catch (AbsentInformationException e) {
				e.printStackTrace(); // TODO EMPTY_CLASS ?
			}
		} else {
			// find location by method name
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
					e.printStackTrace(); // TODO METHOD_EMPTY ?
				}
			} else {
				Message.print(Message.NO_METHOD, className, methodName,
						methodName, className);
			}
		}
	}

	/**
	 * Prints a method trace, i.e. all currently active methods.
	 */
	private void performWhere() {
		final StringBuilder sb = new StringBuilder();
		final int size = methodStack.size();
		for (int i = 1; i <= size; ++i) {
			final Method method = methodStack.get(size - i);
			sb.append("\t[");
			sb.append(i);
			sb.append("] ");
			sb.append(method.declaringType().name());
			sb.append(".");
			sb.append(method.name());
			try {
				final String sourceName = method.location().sourceName();
				sb.append(" (");
				sb.append(sourceName);
				sb.append(")");
			} catch (AbsentInformationException e) {
			}
			if (i < size) {
				sb.append("\n");
			}
		}
		print(TRACE, sb.toString());
	}

	/**
	 * Determines whether the vm is loaded or not.
	 * 
	 * @return True if VM is loaded, false otherwise.
	 */
	private boolean isLoaded() {
		try {
			return curThread.frameCount() > 0;
		} catch (IncompatibleThreadStateException e) {
		}
		return true;
	}

	/**
	 * Once the vm is loaded, the class object reference can be found using its
	 * name.
	 * 
	 * @param className
	 *            The name of the wanted class.
	 * @return class reference according to given name.
	 */
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
		pendingBreakpoints = new ArrayDeque<>();
		methodStack = new Stack<>();

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

	/**
	 * The textual interface of the debugger. Specifically it is nothing else
	 * than an infinite loop which blocks on user input and performs action
	 * according to commands. Can be exited by special commands.
	 * 
	 * @return 0 in case of successful processing, -1 otherwise
	 */
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
					terminate = true;
					break;
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
			vm.dispose();
		} catch (IOException e) {
		}
		return retValue;
	}

	/**
	 * Convert values of type Value into human-readable String objects.
	 * 
	 * @param val
	 *            Value which is about to be displayed.
	 * @param dump
	 *            If true, complex types (arrays, objects, ...) are displayed
	 *            including elements or fields. Otherwise they are printed using
	 *            their name and id.
	 * @return String representation of value.
	 */
	private static String valueToString(final Value val, final boolean dump) {
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
			return "\"" + ((StringReference) val).value() + "\"";
		} else if (val instanceof ArrayReference) {
			final ArrayReference arr = (ArrayReference) val;
			final StringBuilder sb = new StringBuilder();
			if (dump) {
				// print elements
				sb.append("{");
				Iterator<Value> iter = arr.getValues().iterator();
				while (iter.hasNext()) {
					sb.append(valueToString(iter.next(), true));
					if (iter.hasNext()) {
						sb.append(", ");
					}
				}
				sb.append("}");
			} else {
				final String type = arr.type().name();
				final long id = arr.uniqueID();
				sb.append("instance of " + type + " (id=" + id + ")");
			}
			return sb.toString();
		} else if (val instanceof ObjectReferenceImpl) {
			final ObjectReferenceImpl obj = (ObjectReferenceImpl) val;
			final StringBuilder sb = new StringBuilder();
			if (dump) {
				// print fields
				sb.append("[");
				Iterator<Field> iter = obj.referenceType().allFields()
						.iterator();
				while (iter.hasNext()) {
					Field f = iter.next();
					sb.append(f.name());
					sb.append("=");
					sb.append(valueToString(obj.getValue(f), true));
					if (iter.hasNext()) {
						sb.append(", ");
					}
				}
				sb.append("]");
			} else {
				final String type = obj.type().name();
				final long id = obj.uniqueID();
				sb.append("instance of " + type + " (id=" + id + ")");
			}
			return sb.toString();
		} else {
			throw new UnsupportedOperationException(val.getClass().getName());
		}
	}

	/**
	 * Public interface of the debugger. Takes a command in form of a String and
	 * performs necessary steps in order to follow the command.
	 * 
	 * @param cmd
	 *            Debugging command in String form (e.g. "stop in MyClass.main")
	 */
	public void perform(final String cmd) {
		StringTokenizer st = new StringTokenizer(cmd, " .");
		Breakpoint breakpoint;
		String className = null;
		String methodName;
		int lineNumber;

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
					performPrintField(className.trim(), st.nextToken(), false);
				} else {
					performPrintLocal(className.trim(), false);
				}
				break;

			case "locals":
				performLocals();
				break;

			case "fields":
				performFields(st.nextToken().trim());
				break;

			case "dump":
				className = st.nextToken();
				if (st.hasMoreTokens()) {
					performPrintField(className.trim(), st.nextToken(), true);
				} else {
					performPrintLocal(className.trim(), true);
				}
				break;

			case "stop":
				if (st.hasMoreTokens()) {
					// set new breakpoint
					switch (st.nextToken()) {
					case "in": // e.g. "stop in MyClass.main"
						className = st.nextToken().trim();
						methodName = st.nextToken().trim();
						breakpoint = new Breakpoint(className, methodName);
						break;

					case "at": // e.g. "stop at MyClass:22"
						className = st.nextToken(":").trim();
						lineNumber = Integer.parseInt(st.nextToken());
						breakpoint = new Breakpoint(className, lineNumber);
						break;

					default:
						throw new UnsupportedOperationException();
					}

					if (isLoaded()) {
						performStop(breakpoint);
					} else {
						print(DEFER_BREAKPOINT, breakpoint);
						pendingBreakpoints.add(breakpoint);
					}
				} else {
					// print all breakpoints
					performPrintBreakpoints();
				}
				break;

			case "where":
				performWhere();
				break;

			case "step":
			case "next":
				performStep();
				break;

			case "clear": // e.g. clear MyClass:45
				if (st.hasMoreTokens()) {
					// delete breakpoints
					className = st.nextToken(".:").trim();
					methodName = st.nextToken().trim();
					try {
						lineNumber = Integer.parseInt(methodName);
						breakpoint = new Breakpoint(className, lineNumber);
					} catch (NumberFormatException e) {
						breakpoint = new Breakpoint(className, methodName);
					}
					performClear(breakpoint);
				} else {
					// print all breakpoints
					performPrintBreakpoints();
				}
				break;

			case "catch":
			case "ignore":
			case "threads":
			case "thread":
				throw new UnsupportedOperationException(cmd);

			case "exit":
			case "quit":
			case "q":
				break;

			default:
				print(USAGE);
			}
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

	/**
	 * Starts an interactive instance of the debugger.
	 * 
	 * @param args
	 *            Command line arguments.
	 */
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