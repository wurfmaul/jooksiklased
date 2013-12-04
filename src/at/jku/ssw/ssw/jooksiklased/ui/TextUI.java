package at.jku.ssw.ssw.jooksiklased.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import at.jku.ssw.ssw.jooksiklased.Debugger;

public class TextUI extends UI {
	private final Debugger debugger;

	private TextUI(String[] args) {
		debugger = new Debugger(args);
	}

	private int run() {
		final BufferedReader in = new BufferedReader(new InputStreamReader(
				System.in));
		int retValue = 0;
		String cmd;

		while (true) {
			try {
				System.out.print("> ");
				cmd = in.readLine().trim();
				debugger.command(cmd);
			} catch (IOException e) {
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

	public static void main(String[] args) {
		if (args.length == 0)
			System.err.println("Nothing to do...");
		else
			new TextUI(args).run();
	}

}
