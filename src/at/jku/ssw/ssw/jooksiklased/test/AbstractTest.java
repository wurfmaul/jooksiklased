package at.jku.ssw.ssw.jooksiklased.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.StringTokenizer;

import org.junit.Before;
import org.junit.ComparisonFailure;

import at.jku.ssw.ssw.jooksiklased.TestDebugger;

/**
 * Superclass of all test classes. Implements important functionality and
 * provides access to the debugger object.
 * 
 * @author wurfmaul <wurfmaul@posteo.at>
 * 
 */
public abstract class AbstractTest {
	private static final String KEY_ID_OPEN = "(id=";
	private static final String KEY_ID_CLOSE = ")";

	protected TestDebugger debugger;
	protected String classUnderTest;

	@Before
	public void setUp() throws Exception {
		debugger = new TestDebugger(classUnderTest);
	}

	/**
	 * Interface between test cases and debugger itself.
	 * 
	 * @param cmd
	 *            The command which is to be passed to the debugger
	 */
	protected void perform(String cmd) {
		debugger.perform(cmd);
	}

	/**
	 * Gets the debugger's output stream and converts it to string.
	 * 
	 * @return String representation of output stream.
	 */
	protected String getOutput() {
		return debugger.getOut().toString();
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
