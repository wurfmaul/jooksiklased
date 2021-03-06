package at.jku.ssw.ssw.jooksiklased;

import static at.jku.ssw.ssw.jooksiklased.Debugger.Status.NOT_YET_RUNNING;
import static at.jku.ssw.ssw.jooksiklased.Debugger.Status.RUNNING;
import static at.jku.ssw.ssw.jooksiklased.Debugger.Status.TERMINATED;
import static at.jku.ssw.ssw.jooksiklased.Message.BREAKPOINT_ERROR;
import static at.jku.ssw.ssw.jooksiklased.Message.BREAKPOINT_NOT_FOUND;
import static at.jku.ssw.ssw.jooksiklased.Message.DEFER_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.EXIT;
import static at.jku.ssw.ssw.jooksiklased.Message.FIELD;
import static at.jku.ssw.ssw.jooksiklased.Message.FIELDS_HEADER;
import static at.jku.ssw.ssw.jooksiklased.Message.FIELD_INHERITED;
import static at.jku.ssw.ssw.jooksiklased.Message.HIT_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.ILLEGAL_ARGUMENTS;
import static at.jku.ssw.ssw.jooksiklased.Message.INVALID_CMD;
import static at.jku.ssw.ssw.jooksiklased.Message.LIST_BREAKPOINTS;
import static at.jku.ssw.ssw.jooksiklased.Message.METHOD_OVERLOAD;
import static at.jku.ssw.ssw.jooksiklased.Message.NO_CLASS;
import static at.jku.ssw.ssw.jooksiklased.Message.NO_FIELD;
import static at.jku.ssw.ssw.jooksiklased.Message.NO_FIELDS;
import static at.jku.ssw.ssw.jooksiklased.Message.NO_LOCALS;
import static at.jku.ssw.ssw.jooksiklased.Message.NO_LOCAL_INFO;
import static at.jku.ssw.ssw.jooksiklased.Message.NO_METHOD;
import static at.jku.ssw.ssw.jooksiklased.Message.NO_SUCH_THREAD;
import static at.jku.ssw.ssw.jooksiklased.Message.REMOVE_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.RUN;
import static at.jku.ssw.ssw.jooksiklased.Message.SET_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.STEP;
import static at.jku.ssw.ssw.jooksiklased.Message.THREAD_GROUP;
import static at.jku.ssw.ssw.jooksiklased.Message.THREAD_STATUS;
import static at.jku.ssw.ssw.jooksiklased.Message.THREAD_STATUS_BP;
import static at.jku.ssw.ssw.jooksiklased.Message.TOO_MANY_ARGS;
import static at.jku.ssw.ssw.jooksiklased.Message.TRACE;
import static at.jku.ssw.ssw.jooksiklased.Message.TRACE_LOC;
import static at.jku.ssw.ssw.jooksiklased.Message.TRACE_SRC;
import static at.jku.ssw.ssw.jooksiklased.Message.UNABLE_TO_ATTACH;
import static at.jku.ssw.ssw.jooksiklased.Message.UNABLE_TO_LAUNCH;
import static at.jku.ssw.ssw.jooksiklased.Message.UNABLE_TO_START;
import static at.jku.ssw.ssw.jooksiklased.Message.UNKNOWN;
import static at.jku.ssw.ssw.jooksiklased.Message.USAGE;
import static at.jku.ssw.ssw.jooksiklased.Message.VAR;
import static at.jku.ssw.ssw.jooksiklased.Message.VM_NOT_RUNNING;
import static at.jku.ssw.ssw.jooksiklased.Message.VM_RUNNING;
import static at.jku.ssw.ssw.jooksiklased.Message.format;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ShortValue;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadGroupReference;
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
 * Abstract base class of debugger. It provides the complete functionality.
 * 
 * @author wurfmaul <wurfmaul@posteo.at>
 * 
 */
public abstract class Debugger {
	protected static final int DEFAULT_PORT = 8000;

	/** Mapping from command to a boolean whether the vm has to be loaded. */
	private static final Map<String, Status> NEEDS_STATUS;

	/** Flags if the vm is still active. */
	protected Status status = NOT_YET_RUNNING;
	/** Output stream of debugger. */
	protected OutputStream out = System.out;

	/** The environment for the debuggee. */
	private VirtualMachine vm;
	/** The central request manager of the given vm. */
	private EventRequestManager reqManager;
	/** A reference to the current thread. */
	private ThreadReference curThread;

	/** Stack of currently active methods. */
	private Stack<Method> methodStack;
	/** Breakpoints that were not yet set because vm is not loaded. */
	private Queue<Breakpoint> pendingBreakpoints;
	/** Mapping from thread index to referenced thread. */
	private Map<Integer, ThreadReference> threads;
	/** The name of the debuggee class. */
	private String debuggee = null;

	/**
	 * The attaching debugger needs the debuggee to be run itself using a
	 * specific port.
	 * 
	 * @param port
	 *            Number of port, to which to attach
	 */
	protected Debugger(final int port) {
		// find attaching connector
		AttachingConnector con = null;
		for (Connector x : Bootstrap.virtualMachineManager().allConnectors()) {
			if (x.name().equals("com.sun.jdi.SocketAttach"))
				con = (AttachingConnector) x;
		}

		// configure connector
		final Map<String, Argument> args = con.defaultArguments();
		((Argument) args.get("port")).setValue(Integer.toString(port));

		// establish virtual machine for attached debuggee
		try {
			vm = con.attach(args);
			init();
		} catch (IOException e) {
			print(UNABLE_TO_ATTACH, e.getMessage());
		} catch (IllegalConnectorArgumentsException e) {
			print(ILLEGAL_ARGUMENTS, e.getMessage());
		}
	}

	/**
	 * The launching debugger does not need the debuggee to be started by
	 * itself, but starts the vm itself.
	 * 
	 * @param debuggee
	 *            The name of the class which is about to be debugged.
	 * @param args
	 *            The debuggee's arguments.
	 */
	protected Debugger(final String... args) {
		// parse arguments
		assert args.length >= 1;
		this.debuggee = args[0];
		final StringBuilder sb = new StringBuilder();
		for (String s : args) {
			sb.append(s);
			sb.append(" ");
		}

		// establish connection
		LaunchingConnector con = Bootstrap.virtualMachineManager()
				.defaultConnector();
		Map<String, Argument> arguments = con.defaultArguments();
		((Argument) arguments.get("main")).setValue(sb.toString());

		// establish virtual machine for debuggee
		try {
			vm = con.launch(arguments);
			init();
		} catch (IOException e) {
			print(UNABLE_TO_LAUNCH, e.getMessage());
		} catch (IllegalConnectorArgumentsException e) {
			print(ILLEGAL_ARGUMENTS, e.getMessage());
		} catch (VMStartException e) {
			print(UNABLE_TO_START, e.getMessage());
		}
	}

	static {
		// e.g. "cont" needs the vm to be running
		// "run" needs the vm to be stopped
		// commands that are not listed here don't care about the status
		NEEDS_STATUS = new HashMap<>();
		NEEDS_STATUS.put("cont", RUNNING);
		NEEDS_STATUS.put("dump", RUNNING);
		NEEDS_STATUS.put("fields", RUNNING);
		NEEDS_STATUS.put("locals", RUNNING);
		NEEDS_STATUS.put("next", RUNNING);
		NEEDS_STATUS.put("print", RUNNING);
		NEEDS_STATUS.put("run", NOT_YET_RUNNING);
		NEEDS_STATUS.put("step", RUNNING);
		NEEDS_STATUS.put("where", RUNNING);
	}

	/**
	 * Initializes fields that re necessary for both constructors.
	 */
	private void init() {
		// make space for pending operations
		pendingBreakpoints = new ArrayDeque<>();
		methodStack = new Stack<>();
		threads = new LinkedHashMap<>();

		// Establish Request Manager
		reqManager = vm.eventRequestManager();

		// redirect IO streams
		Process proc = vm.process();
		new Redirection(proc.getErrorStream(), out).start();
		new Redirection(proc.getInputStream(), out).start();

		vm.suspend();

		// find current thread
		for (ThreadReference t : vm.allThreads()) {
			if (t.name().equals("main")) {
				curThread = t;
				break;
			}
		}

		assert curThread != null;
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
		if (status == NOT_YET_RUNNING) {
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
		// let vm run
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
						// update status
						status = TERMINATED;
						break;
					} else if (e instanceof MethodEntryEvent) {
						// push entered method on method stack
						final MethodEntryEvent mee = (MethodEntryEvent) e;
						final Method method = mee.method();
						methodStack.push(method);

						// do not resume vm if a breakpoint is reached
						boolean isBreakpoint = false;
						for (BreakpointRequest bpr : reqManager
								.breakpointRequests()) {
							if (bpr.location().equals(mee.location()))
								isBreakpoint = true;
						}
						if (!isBreakpoint) {
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
		if (clazz.visibleFields().size() > 0) {
			print(FIELDS_HEADER);
			for (Field f : clazz.visibleFields()) {
				if (!clazz.equals(f.declaringType())) {
					// extract declaring class (heritage)
					final String declClass = f.declaringType().name();
					print(FIELD_INHERITED, f.typeName(), f.name(), declClass);
				} else {
					// not inherited
					print(FIELD, f.typeName(), f.name());
				}
			}
		} else {
			print(NO_FIELDS, className);
		}
	}

	/**
	 * Reads visible local variables at certain position, depending on the
	 * current frame.
	 */
	private void performLocals() {
		try {
			final StackFrame curFrame = curThread.frame(0);
			if (curFrame.visibleVariables().size() > 0) {
				for (LocalVariable var : curFrame.visibleVariables()) {
					String value = valueToString(curFrame.getValue(var), false);
					print(VAR, var.typeName(), var.name(), value);
				}
			} else {
				print(NO_LOCALS);
			}
		} catch (IncompatibleThreadStateException e) {
			e.printStackTrace();
		} catch (IndexOutOfBoundsException e) {
		} catch (AbsentInformationException e) {
			print(NO_LOCAL_INFO);
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
	 *            Name of class which contains the field or null for the current
	 *            position
	 * @param fieldName
	 *            Name of field or local variable which is about to be
	 *            displayed.
	 * @param dump
	 *            True if complex structures like classes or arrays should be
	 *            printed including their contents. False if only the name and
	 *            id should be printed.
	 */
	private void performPrint(String className, String varName, boolean dump) {
		if (className != null) {
			final ReferenceType clazz = findClass(className);
			final Field f = clazz.fieldByName(varName);
			if (f == null) {
				print(NO_FIELD, varName, className);
			} else if (f.isStatic()) {
				final String value = valueToString(clazz.getValue(f), dump);
				print(VAR, f.typeName(), f.name(), value);
			} else {
				print(FIELD, f.typeName(), className + "." + f.name());
			}
			return;
		}
		// className = null
		try {
			final StackFrame curFrame = curThread.frame(0);
			LocalVariable var = curFrame.visibleVariableByName(varName);
			if (var != null) {
				// local variable
				String value = valueToString(curFrame.getValue(var), dump);
				print(VAR, var.typeName(), var.name(), value);
				return;
			}
			final ReferenceType type = curFrame.location().declaringType();
			final Field f = type.fieldByName(varName);
			if (f == null) {
				print(UNKNOWN, varName);
				return;
			}
			// instance variable of static method
			Value val;
			if (curFrame.location().method().isStatic()) {
				final List<ObjectReference> instances = type.instances(0);
				if (instances.size() == 0) {
					// class not yet initiated
					print(FIELD, f.typeName(), f.name());
					return;
				}
				// class already initiated
				assert instances.size() == 1;
				val = instances.get(0).getValue(f);
			} else {
				val = curFrame.thisObject().getValue(f);
			}
			print(VAR, f.typeName(), f.name(), valueToString(val, dump));
		} catch (IncompatibleThreadStateException e) {
			e.printStackTrace();
		} catch (AbsentInformationException e) {
			print(UNKNOWN, varName);
		}
	}

	/**
	 * This method starts the VM and stops at first method entry.
	 */
	private void performRun() {
		print(RUN, debuggee);

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
						// tell everyone that we are running
						status = RUNNING;
						// initialization done, tell loop to exit
						exit = true;
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
		final ReferenceType clazz = findClass(className);
		int lineNumber = breakpoint.lineNumber;

		if (lineNumber >= 0) {
			// find location by line number
			try {
				// define range
				final List<Location> allLines = clazz.allLineLocations();
				final int lastLine = allLines.get(allLines.size() - 1)
						.lineNumber();
				List<Location> locs;
				// find executable line in range
				do {
					locs = clazz.locationsOfLine(lineNumber++);
				} while (locs.size() < 1 && lineNumber < lastLine);

				if (locs.isEmpty()) {
					print(BREAKPOINT_ERROR, breakpoint, breakpoint.lineNumber,
							className);
				} else {
					setBreakpoint(locs.get(0));
				}

			} catch (AbsentInformationException e) {
				e.printStackTrace();
			}
		} else {
			// find location by method name
			final List<Method> methods = clazz.methodsByName(methodName);

			if (methods.size() == 1) {
				Method method = methods.get(0);

				// get first executable line
				try {
					List<Location> locs = method.allLineLocations();
					assert locs.size() > 0;
					setBreakpoint(locs.get(0));
				} catch (AbsentInformationException e) {
					e.printStackTrace();
				}
			} else if (methods.size() == 0) {
				print(NO_METHOD, breakpoint, methodName, className);
			} else {
				print(METHOD_OVERLOAD, breakpoint, methodName);
			}
		}
	}

	/**
	 * Print all currently active threads.
	 */
	private void performThreads() {
		// categorize threads
		final Map<ThreadGroupReference, List<ThreadReference>> groups = new LinkedHashMap<>();
		for (ThreadReference t : vm.allThreads()) {
			ThreadGroupReference group = t.threadGroup();
			if (!groups.containsKey(group))
				groups.put(group, new LinkedList<ThreadReference>());
			groups.get(group).add(t);
		}
		// prepare listing
		threads.clear();
		int idx = 0;
		// print categories
		for (Entry<ThreadGroupReference, List<ThreadReference>> e : groups
				.entrySet()) {
			print(THREAD_GROUP, e.getKey().name());

			// print threads
			String type, name, status;
			long id;

			for (ThreadReference t : e.getValue()) {
				threads.put(idx, t);
				type = t.type().name();
				id = t.uniqueID();
				name = t.name();
				status = statusToString(t.status());
				if (t.isAtBreakpoint())
					print(THREAD_STATUS_BP, idx, type, id, name, status);
				else
					print(THREAD_STATUS, idx, type, id, name, status);
				idx++;
			}
		}
	}

	/**
	 * Switch current thread to thread with given index.
	 * 
	 * @param index
	 *            Index of thread which is to be the current thread from now.
	 */
	private void performThread(final int index) {
		if (threads.containsKey(index)) {
			curThread = threads.get(index);
		} else {
			print(NO_SUCH_THREAD, index);
		}
	}

	/**
	 * Prints a method trace, i.e. all currently active methods.
	 */
	private void performWhere() {
		final int size = methodStack.size();
		for (int i = 1; i <= size; ++i) {
			final Method method = methodStack.get(size - i);
			final String caller = method.declaringType().name();
			try {
				final String sourceName = method.location().sourceName();
				final Location curLoc = curThread.frame(0).location();
				if (i == 1 && method.equals(curLoc.method())) {
					// current location
					final int line = curLoc.lineNumber();
					print(TRACE_LOC, i, caller, method.name(), sourceName, line);
				} else {
					// TODO trace should always include line number
					print(TRACE_SRC, i, caller, method.name(), sourceName);
				}
			} catch (AbsentInformationException
					| IncompatibleThreadStateException e) {
				print(TRACE, i, caller, method.name());
			}
		}
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
	 * Prints message to the output stream
	 * 
	 * @param msg
	 *            formatting string
	 * @param args
	 *            formatting arguments
	 */
	private void print(Message msg, Object... args) {
		try {
			out.write(format(msg, args).getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns human readable thread status from {@link ThreadReference} status
	 * codes.
	 * 
	 * @param status
	 *            Integer representing the status code.
	 * @return String representation of thread status.
	 */
	private static String statusToString(final int status) {
		switch (status) {
		case ThreadReference.THREAD_STATUS_ZOMBIE:
			return "terminated";
		case ThreadReference.THREAD_STATUS_RUNNING:
			return "running";
		case ThreadReference.THREAD_STATUS_SLEEPING:
			return "sleeping";
		case ThreadReference.THREAD_STATUS_MONITOR:
			return "cond. waiting";
		case ThreadReference.THREAD_STATUS_WAIT:
			return "waiting";
		case ThreadReference.THREAD_STATUS_NOT_STARTED:
			return "not started";
		default:
			return "unknown";
		}
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
			if (val == null)
				return "<no value>";
			throw new UnsupportedOperationException(val.getClass().getName());
		}
	}

	/**
	 * Close connections, terminate VM
	 */
	protected void close() {
		try {
			vm.dispose();
		} catch (VMDisconnectedException e) {
		}
	}

	/**
	 * Public interface of the debugger. Takes a command in form of a String and
	 * performs necessary steps in order to follow the command.
	 * 
	 * @param cmd
	 *            Debugging command in String form (e.g. "stop in MyClass.main")
	 */
	protected void perform(final String cmd) {
		StringTokenizer st = new StringTokenizer(cmd, " .");
		Breakpoint breakpoint;
		String className = null;
		String methodName, varName;
		int lineNumber;

		try {
			final String command = st.nextToken();

			// check whether the machine should be running for the command
			final Status wantedStatus = NEEDS_STATUS.get(command);
			if (wantedStatus != null && wantedStatus != status) {
				if (wantedStatus == RUNNING)
					print(VM_NOT_RUNNING, command);
				else
					print(VM_RUNNING);
				return;
			}

			// perform action according to command
			switch (command) {
			case "run":
				performRun();
				break;

			case "cont":
				performCont();
				break;

			case "dump":
			case "print":
				varName = st.nextToken().trim();
				if (st.hasMoreTokens()) {
					// class.field
					performPrint(varName, st.nextToken(),
							command.equals("dump"));
				} else {
					// var
					performPrint(null, varName, command.equals("dump"));
				}
				break;

			case "locals":
				performLocals();
				break;

			case "fields":
				if (st.hasMoreTokens())
					performFields(st.nextToken().trim());
				else
					print(NO_CLASS);
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
						className = st.nextToken(": ").trim();
						lineNumber = Integer.parseInt(st.nextToken());
						breakpoint = new Breakpoint(className, lineNumber);
						break;

					default:
						throw new UnsupportedOperationException();
					}

					if (status == RUNNING) {
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

			case "threads":
				performThreads();
				break;

			case "thread":
				int index = Integer.parseInt(st.nextToken());
				performThread(index);
				break;

			// TODO case "catch":
			// TODO case "ignore":

			case "exit":
			case "quit":
			case "q":
				break;

			default:
				throw new UnsupportedOperationException();
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
		} catch (NoSuchElementException | UnsupportedOperationException
				| NumberFormatException e) {
			print(INVALID_CMD, cmd);
			print(USAGE);
		}
	}

	/**
	 * Returns the name of the current thread
	 * 
	 * @return Name of the current thread.
	 */
	public String getThreadName() {
		return curThread.name();
	}

	/**
	 * Indicates the current status of the machine.
	 */
	static enum Status {
		RUNNING, TERMINATED, NOT_YET_RUNNING;
	}
}
