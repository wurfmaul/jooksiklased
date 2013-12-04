package at.jku.ssw.ssw.jooksiklased;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

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
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
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

public class TextDebugger {
	private final VirtualMachine vm;
	private final EventRequestManager reqManager;

	public TextDebugger() throws IOException,
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
		vm = con.attach(args);
		reqManager = vm.eventRequestManager();
	}

	private void run() {
		try {
			// --- post request
			MethodEntryRequest req = reqManager.createMethodEntryRequest();
			req.addClassFilter("Test");
			req.enable();

			// --- listen for events
			EventQueue q = vm.eventQueue();
			while (true) {
				EventSet events = q.remove();
				EventIterator iiter = events.eventIterator();
				while (iiter.hasNext()) {
					Event e = iiter.nextEvent();
					if (e instanceof MethodEntryEvent) {
						MethodEntryEvent me = (MethodEntryEvent) e;
						System.out.println("call of " + me.method().toString());
						printVars(me.thread().frame(0));
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
						System.out.println(e);
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
			throw new NotImplementedException();
		}
	}

	private static void printLocation(Location loc) {
		System.out.println(loc.lineNumber() + ", " + loc.codeIndex());
	}

	public static void main(String[] arguments) {
		try {
			TextDebugger debugger = new TextDebugger();
			debugger.run();
		} catch (IOException | IllegalConnectorArgumentsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}