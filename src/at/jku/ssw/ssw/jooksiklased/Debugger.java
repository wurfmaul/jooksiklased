package at.jku.ssw.ssw.jooksiklased;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.sun.jdi.ArrayReference;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.Connector.Argument;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.StepRequest;

public class Debugger {

	private VirtualMachine vm;
	private EventRequestManager reqManager;
	private String debuggee = null;

	public Debugger(String[] args) {
		final List<String> arguments = new ArrayList<>();
		boolean launch = false;
		boolean attach = false;
		String address = "8000";

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-attach")) {
				attach = true;
				if (args.length >= ++i)
					address = args[i];
				else {
					System.err.print("Warning: no address specified.");
					System.err.println("Using default address " + address);
				}
			} else if (args[i].equalsIgnoreCase("-launch")) {
				launch = false;
			} else if (debuggee == null) {
				debuggee = args[i];
			} else {
				arguments.add(args[i]);
			}
		}

		if (launch && attach) {
			throw new IllegalStateException("Cannot attach and launch at once.");
		} else if (attach) {
			/*
			 * TODO Attaches the debugger to a running JVM with the default
			 * connection mechanism.
			 */
			attach(address);
		} else {
			/*
			 * TODO Starts the debugged application immediately upon startup of
			 * JDB. The -launch option removes the need for the run command. The
			 * debugged application is launched and then stopped just before the
			 * initial application class is loaded. At that point, you can set
			 * any necessary breakpoints and use the cont command to continue
			 * execution.
			 */
			launch();
		}
	}

	private void launch() {
		LaunchingConnector con = Bootstrap.virtualMachineManager()
				.defaultConnector();
		Map<String, Argument> args = con.defaultArguments();
		((Argument) args.get("main")).setValue(debuggee);
		
		try {
			vm = con.launch(args);
			Process proc = vm.process();
			new Redirection(proc.getErrorStream(), System.err).start();
			new Redirection(proc.getInputStream(), System.out).start();
			
			reqManager = vm.eventRequestManager();
			MethodEntryRequest req = reqManager.createMethodEntryRequest();
			req.addClassFilter(debuggee);
			req.enable();
			vm.resume();
			
			listen();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void attach(String address) {
		AttachingConnector con = null;
		Iterator<Connector> iter = Bootstrap.virtualMachineManager()
				.allConnectors().iterator();

		while (iter.hasNext()) {
			Connector x = (Connector) iter.next();
			if (x.name().equals("com.sun.jdi.SocketAttach"))
				con = (AttachingConnector) x;
		}
		Map<String, Argument> args = con.defaultArguments();
		((Connector.Argument) args.get("port")).setValue(address);
		try {
			vm = con.attach(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void listen() {
		EventQueue q = vm.eventQueue();
		while (true) {
			try {
				EventSet events = q.remove();
				EventIterator iter = events.eventIterator();
				while (iter.hasNext()) {
					Event e = iter.nextEvent();
					if (e instanceof MethodEntryEvent) {
						MethodEntryEvent me = (MethodEntryEvent) e;
						System.out.println("call of " + me.method().toString());
						printVars(me.thread().frame(0));
						vm.resume();
					} else if (e instanceof BreakpointEvent) {
						BreakpointEvent be = (BreakpointEvent) e;
						System.out.println("breakpoint at "
								+ be.location().lineNumber() + " in "
								+ be.location().method().toString());
						printVars(be.thread().frame(0));
					} else if (e instanceof StepEvent) {
						StepEvent se = (StepEvent) e;
						System.out.print("step halted in "
								+ se.location().method().name() + " at ");
						printLocation(se.location());
						printVars(se.thread().frame(0));
						reqManager.deleteEventRequest(se.request());
					} else {
						System.out.println("ELSE");
						System.out.println(e);
					}
				}
			} catch (Exception e) {
				break;
			}
		}
	}

	public void stepOver(ThreadReference thread) {
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

	public void setBreakpoint(Method m, int line) {
		try {
			List<Location> locs = m.locationsOfLine(line);
			if (locs.size() > 0) {
				Location loc = locs.get(0);
				BreakpointRequest req = reqManager.createBreakpointRequest(loc);
				req.enable();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String printVars(StackFrame frame) {
		final StringBuilder sb = new StringBuilder();
		try {
			Iterator<LocalVariable> iter = frame.visibleVariables().iterator();
			while (iter.hasNext()) {
				LocalVariable v = (LocalVariable) iter.next();
				sb.append(v.name() + ": " + v.type().name() + " = ");
				printValue(frame.getValue(v));
				sb.append(Character.LINE_SEPARATOR);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	private String printValue(Value val) {
		final StringBuilder sb = new StringBuilder();
		if (val instanceof IntegerValue) {
			sb.append(((IntegerValue) val).value() + " ");
		} else if (val instanceof StringReference) {
			sb.append(((StringReference) val).value() + " ");
		} else if (val instanceof ArrayReference) {
			Iterator<Value> iter = ((ArrayReference) val).getValues()
					.iterator();
			while (iter.hasNext()) {
				printValue((Value) iter.next());
			}
		} else {
		}
		return sb.toString();
	}

	private String printLocation(Location loc) {
		return loc.lineNumber() + ", " + loc.codeIndex();
	}
	
	public String command(String cmd) {
		StringTokenizer st = new StringTokenizer(cmd, " ");

		switch (st.nextToken()) {
		// http://download.java.net/jdk8/docs/technotes/tools/windows/jdb.html
		case "run":
			// TODO After you start JDB and set breakpoints, you can use
			// the run command to execute the debugged application. The
			// run command is available only when the jdb command starts
			// the debugged application as opposed to attaching to an
			// existing JVM.
			throw new UnsupportedOperationException("run");

		case "cont":
			// TODO Continues execution of the debugged application
			// after a breakpoint, exception, or step.
			throw new UnsupportedOperationException("cont");

		case "print":
			// TODO Displays Java objects and primitive values. For
			// variables or fields of primitive types, the actual value
			// is printed. For objects, a short description is printed.
			// See the dump command to find out how to get more
			// information about an object.
			// EXAMPLES:
			// print MyClass.myStaticField
			// print myObj.myInstanceField
			// print i + j + k (i, j, k are primities and either fields
			// or local variables)
			// print myObj.myMethod() (if myMethod returns a non-null)
			// print new java.lang.String("Hello").length()
			throw new UnsupportedOperationException("print");

		case "dump":
			// TODO For primitive values, the dump command is identical
			// to the print command. For objects, the dump command
			// prints the current value of each field defined in the
			// object. Static and instance fields are included. The dump
			// command supports the same set of expressions as the print
			// command.
			throw new UnsupportedOperationException("dump");

		case "threads":
			// TODO List the threads that are currently running. For
			// each thread, its name and current status are printed and
			// an index that can be used in other commands. In this
			// example, the thread index is 4, the thread is an instance
			// of java.lang.Thread, the thread name is main, and it is
			// currently running.
			throw new UnsupportedOperationException("threads");

		case "thread":
			// TODO Select a thread to be the current thread. Many jdb
			// commands are based on the setting of the current thread.
			// The thread is specified with the thread index described
			// in the threads command.
			throw new UnsupportedOperationException("thread");

		case "where":
			// TODO The where command with no arguments dumps the stack
			// of the current thread. The where all command dumps the
			// stack of all threads in the current thread group. The
			// where threadindex command dumps the stack of the
			// specified thread. If the current thread is suspended
			// either through an event such as a breakpoint or through
			// the suspend command, then local variables and fields can
			// be displayed with the print and dump commands. The up and
			// down commands select which stack frame is the current
			// stack frame.
			throw new UnsupportedOperationException("where");

		case "stop":
			
			if (st.nextToken().equals("in")){
				final String className = st.nextToken(".");
				final String methodName = st.nextToken();
				// "stop in Test.main"
				
				System.out.println(className + " -> " + methodName);
//				setBreakpoint(m, line);
			}
			break;
		case "clear":
			/*
			 * TODO Breakpoints can be set in JDB at line numbers or at
			 * the first instruction of a method, for example:
			 * 
			 * - stop at MyClass:22 sets a breakpoint at the first
			 * instruction for line 22 of the source file containing
			 * MyClass.
			 * 
			 * - stop in java.lang.String.length sets a breakpoint at
			 * the beginning of the method java.lang.String.length.
			 * 
			 * - stop in MyClass.<clinit> uses <clinit> to identify the
			 * static initialization code for MyClass.
			 * 
			 * When a method is overloaded, you must also specify its
			 * argument types so that the proper method can be selected
			 * for a breakpoint. For example,
			 * MyClass.myMethod(int,java.lang.String) or
			 * MyClass.myMethod(). The clear command removes breakpoints
			 * using the following syntax: clear MyClass:45. Using the
			 * clear or stop command with no argument displays a list of
			 * all breakpoints currently set. The cont command continues
			 * execution.
			 */
			throw new UnsupportedOperationException("stop / clear");

		case "step":
		case "next":
			// TODO The step command advances execution to the next line
			// whether it is in the current stack frame or a called
			// method. The next command advances execution to the next
			// line in the current stack frame.
			throw new UnsupportedOperationException("step / next");

		case "catch":
		case "ignore":
			// TODO When an exception occurs for which there is not a
			// catch statement anywhere in the throwing thread's call
			// stack, the JVM typically prints an exception trace and
			// exits. When running under JDB, however, control returns
			// to JDB at the offending throw. You can then use the jdb
			// command to diagnose the cause of the exception. Use the
			// catch command to cause the debugged application to stop
			// at other thrown exceptions, for example: catch
			// java.io.FileNotFoundException or catch
			// mypackage.BigTroubleException. Any exception that is an
			// instance of the specified class or subclass stops the
			// application at the point where it is thrown. The ignore
			// command negates the effect of an earlier catch command.
			// The ignore command does not cause the debugged JVM to
			// ignore specific exceptions, but only to ignore the
			// debugger.
			throw new UnsupportedOperationException("catch / ignore");

		default:
			// TODO list of commands
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
		
		return "";
	}
}
