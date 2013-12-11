package at.jku.ssw.ssw.jooksiklased.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.StringTokenizer;

import org.junit.After;
import org.junit.Before;
import org.junit.ComparisonFailure;

import at.jku.ssw.ssw.jooksiklased.Message;
import at.jku.ssw.ssw.jooksiklased.TextDebugger;

import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.VMStartException;

public abstract class AbstractTest {
	private static final String KEY_ID_OPEN = "(id=";
	private static final String KEY_ID_CLOSE = ")";
	
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
	 * Like assertEquals() but strings which are classified as ids are ignored.
	 * 
	 * @param expected
	 *            The expected String
	 * @param actual
	 *            The actual String
	 */
	protected static void assertEqualsIgnoreId(String expected, String actual) {
		final StringTokenizer expLines = new StringTokenizer(expected, "\n");
		final StringTokenizer actLines = new StringTokenizer(actual, "\n");

		String exp, act;
		while (expLines.hasMoreTokens() && actLines.hasMoreTokens()) {
			exp = expLines.nextToken();
			act = actLines.nextToken();
			int iId = exp.indexOf(KEY_ID_OPEN);

			if (iId > -1) {
				// id key was found
				if (!equalsIgnoreId(exp, act)) {
					throw new ComparisonFailure(
							"Strings differ although id is ignored.", exp, act);
				}
			} else {
				// normal string line (without id)
				assertEquals(exp, act);
			}
		}

		assertFalse(expLines.hasMoreTokens());
		assertFalse(actLines.hasMoreTokens());
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
	private static boolean equalsIgnoreId(final String exp, final String line) {
		final int from = exp.indexOf(KEY_ID_OPEN) + 4;
		assert from > 3;
		// does first part (before id) match?
		if (!exp.substring(0, from).equals(line.substring(0, from)))
			return false;
		// skip id
		final int toExp = from + exp.substring(from).indexOf(KEY_ID_CLOSE);
		final int toLine = from + line.substring(from).indexOf(KEY_ID_CLOSE);
		for (int i = from; i < toLine; i++) {
			if (!isHexDigit(line.charAt(i)))
				return false;
		}
		// does third part (after id) match?
		if (!exp.substring(toExp).trim().equals(line.substring(toLine).trim()))
			return false;
		return true;
	}

	/**
	 * Determins whether character is hex digit or not
	 * 
	 * @param ch
	 *            the character to check
	 * @return true if ch is [0-9A-Fa-f]
	 */
	private static boolean isHexDigit(final char ch) {
		return '0' <= ch && ch <= '9' || 'A' <= ch && ch <= 'F' || 'a' <= ch
				&& ch <= 'f';
	}
}
