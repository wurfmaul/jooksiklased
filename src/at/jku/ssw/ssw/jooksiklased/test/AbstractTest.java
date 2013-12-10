package at.jku.ssw.ssw.jooksiklased.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;

import at.jku.ssw.ssw.jooksiklased.Message;
import at.jku.ssw.ssw.jooksiklased.TextDebugger;

import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.VMStartException;

public abstract class AbstractTest {
	protected TextDebugger debugger;
	protected PrintStream backup;
	protected ByteArrayOutputStream out;
	protected String classUnderTest;

	@Before
	public void setUp() throws Exception {
		try {
			backup = System.out;
			out = new ByteArrayOutputStream();
			System.setOut(new PrintStream(out, true));

			debugger = new TextDebugger(classUnderTest);
		} catch (IOException | IllegalConnectorArgumentsException
				| VMStartException e) {
		}
	}

	@After
	public void tearDown() throws Exception {
		System.setOut(backup);
	}

	/**
	 * Simulates the behavior of Message.print, but returns String instead.
	 * 
	 * @param msg
	 *            One of enum Message.
	 * @param args
	 *            Arguments for format string.
	 * @return The formatted string.
	 */
	protected static String format(Message msg, Object... args) {
		return String.format(msg + "\n", args);
	}

	/**
	 * Extracts the first line from the output stream. E.g. it reads the content
	 * of out and takes the first line. The rest is written back to out. The
	 * first line is returned. Assume a new-line character given as '\n'.
	 * 
	 * @param out
	 *            The OutputStream that is to be extracted.
	 * @return The first line of out.
	 */
	protected static String extractFirstLine(ByteArrayOutputStream out) {
		final String allLines = out.toString();
		final int index = allLines.indexOf("\n") + 1;
		final String firstLine = allLines.substring(0, index);
		final String restLines = allLines.substring(index);
		try {
			out.reset();
			out.write(restLines.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return firstLine;
	}

	/**
	 * Compares two Strings ignoring the numeric id-part. Assume that the id is
	 * formulated as "(id=000)".
	 * 
	 * @param exp
	 *            The expected string
	 * @param line
	 *            The actual string
	 * @return true if exp and line are equal except for the id
	 */
	protected static boolean ignoreId(final String exp, final String line) {
		final int from = exp.indexOf("(id=") + 4;
		assert from > 3;
		// does first part (before id) match?
		if (!exp.substring(0, from).equals(line.substring(0, from)))
			return false;
		// skip id
		final int toExp = from + exp.substring(from).indexOf(")");
		final int toLine = from + line.substring(from).indexOf(")");
		for (int i = from; i < toLine; i++) {
			if (!Character.isDigit(line.charAt(i)))
				return false;
		}
		// does third part (after id) match?
		if (!exp.substring(toExp).trim().equals(line.substring(toLine).trim()))
			return false;
		return true;
	}
}
