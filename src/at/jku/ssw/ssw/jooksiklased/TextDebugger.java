package at.jku.ssw.ssw.jooksiklased;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.Connector.Argument;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.MethodEntryRequest;

public class TextDebugger {
	private final VirtualMachine vm;
	
	public TextDebugger() throws IOException, IllegalConnectorArgumentsException {
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
	}
	
	private void run() {
		try{
			//--- post request
			EventRequestManager reqManager = vm.eventRequestManager();
			MethodEntryRequest req = reqManager.createMethodEntryRequest();
			req.addClassFilter("Test");
			req.enable();
			
			//--- listen for events
			EventQueue q = vm.eventQueue();
			while (true) {
				EventSet events = q.remove();
				EventIterator iiter = events.eventIterator();
				while (iiter.hasNext()) {
					Event e = iiter.nextEvent();
					if (e instanceof MethodEntryEvent) {
						MethodEntryEvent me = (MethodEntryEvent)e;
						System.out.println("call of " + me.method().toString());
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