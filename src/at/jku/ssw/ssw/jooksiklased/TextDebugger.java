package at.jku.ssw.ssw.jooksiklased;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

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
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.StepRequest;

public class TextDebugger {
	private final VirtualMachine vm;
	private final EventRequestManager reqManager;
	private ThreadReference curThread;

	private TextDebugger() throws IOException,
			IllegalConnectorArgumentsException {
		AttachingConnector con = null;
		Iterator<Connector> iter = Bootstrap.virtualMachineManager()
				.allConnectors().iterator();
		while (iter.hasNext()) {
			Connector x = (Connector) iter.next();
			if (x.name().equals("com.sun.jdi.SocketAttach"))
				con = (AttachingConnector) x;
		}
		Map<String, Argument> args = con.defaultArguments();
		((Connector.Argument) args.get("port")).setValue("8000");

		// Establish VirtualMachine
		vm = con.attach(args);

		// Establish Request Manager
		reqManager = vm.eventRequestManager();

		vm.suspend();

		// Get current thread
		for (ThreadReference t : vm.allThreads()) {
			if (t.name().equals("main")) {
				curThread = t;
				break;
			}
		}
	}

	private void run() {
		vm.resume();
		curThread.resume();

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
						System.out.println("reached breakpoint at "
								+ be.location().lineNumber() + " in "
								+ be.location().method().toString());
						printVars(be.thread().frame(0));
					} else {
						vm.resume();
					}
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IncompatibleThreadStateException e) {
			e.printStackTrace();
		}
	}

	private void methodEntry() {
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
						return;
					} else {
						vm.resume();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setBreakpoint(Method m, int line) {
		try {
			List<Location> locs = m.locationsOfLine(line);
			if (locs.size() > 0) {
				Location loc = locs.get(0);
				EventRequestManager reqManager = vm.eventRequestManager();
				BreakpointRequest req = reqManager.createBreakpointRequest(loc);
				req.enable();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
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

	@SuppressWarnings("unused")
	private static void printLocation(Location loc) {
		System.out.println(loc.lineNumber() + ", " + loc.codeIndex());
	}

	private void perform(final String cmd) throws InterruptedException {
		StringTokenizer st = new StringTokenizer(cmd, " ");

		switch (st.nextToken()) {
		// http://download.java.net/jdk8/docs/technotes/tools/windows/jdb.html
		case "run":
			run();
			break;
			
		case "cont":
		case "print":
		case "dump":
		case "threads":
		case "thread":
		case "where":
			throw new UnsupportedOperationException();

		case "stop":
			if (st.nextToken().equals("in")) {
				final String className = st.nextToken(".").trim();
				final String methodName = st.nextToken().trim();
				System.out.printf("wants to stop in %s.%s\n", className,
						methodName);

				try {
					assert curThread.frameCount() == 1;

					// find class
					final List<ReferenceType> classes = vm
							.classesByName(className);
					assert classes.size() == 1;
					final ReferenceType clazz = classes.get(0);

					// find method
					final List<Method> methods = clazz
							.methodsByName(methodName);
					assert methods.size() == 1;
					final Method method = methods.get(0);

					// get first executable line
					final List<Location> locs = method.allLineLocations();
					assert locs.size() > 0;
					final Location loc = locs.get(0);

					System.out.println("Set breakpoint in '" + method
							+ "' at line " + loc.lineNumber());

					setBreakpoint(method, loc.lineNumber());
				} catch (Exception e) {
					e.printStackTrace();
				}
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

	private int ui() {
		final BufferedReader in = new BufferedReader(new InputStreamReader(
				System.in));
		int retValue = 0;
		String cmd;

		while (true) {
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
			debugger.methodEntry();
			debugger.ui();
		} catch (IOException | IllegalConnectorArgumentsException e) {
			e.printStackTrace();
		}
	}
}