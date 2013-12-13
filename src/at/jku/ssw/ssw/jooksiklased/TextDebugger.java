package at.jku.ssw.ssw.jooksiklased;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Implementation of debugger, providing a textual interface.
 * 
 * @author wurfmaul <wurfmaul@posteo.at>
 * 
 */
public class TextDebugger extends Debugger {
	/** Flag for the main loop, whether to exit at next run or not. */
	private boolean terminate = false;

	private TextDebugger(int port) {
		super(port);
	}

	private TextDebugger(String debuggee) {
		super(debuggee);
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

		while (!terminate && status != Status.TERMINATED) {
			try {
				System.out.print("> ");
				cmd = in.readLine().trim();
				switch (cmd) {
				case "quit":
				case "exit":
				case "q":
					terminate = true;
					break;
				default:
					perform(cmd);
				}
			} catch (IOException e) {
				retValue = -1;
				break;
			}
		}
		try {
			close();
			in.close();
		} catch (IOException e) {
		}
		return retValue;
	}

	/**
	 * Starts an interactive instance of the debugger.
	 * 
	 * @param args
	 *            Command line arguments.
	 */
	public static void main(String[] args) {
		TextDebugger debugger;
		int port = -1;

		int i = 0;
		// parse options (i.e. starting with "-")
		while (i < args.length && args[i].startsWith("-")) {
			switch (args[i]) {
			case "-attach":
				try {
					port = Integer.parseInt(args[++i]);
				} catch (NumberFormatException e) {
					port = DEFAULT_PORT;
				}
				break;

			// TODO deal with other options

			default:
				System.err.println("Unknown option: " + args[i]);
				i++;
			}
		}

		// TODO deal with debuggee's arguments

		if (i < args.length) {
			debugger = new TextDebugger(args[i]);
			debugger.ui();
		} else if (port >= 0) {
			debugger = new TextDebugger(port);
			debugger.ui();
		} else {
			System.err.println("Could not start debugger.");
		}
	}
}