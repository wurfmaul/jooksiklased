package at.jku.ssw.ssw.jooksiklased.test;

import static org.junit.Assert.assertEquals;
import static at.jku.ssw.ssw.jooksiklased.Message.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import at.jku.ssw.ssw.jooksiklased.Message;
import at.jku.ssw.ssw.jooksiklased.TextDebugger;

import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.VMStartException;

public class BasicTest {
	private TextDebugger debugger;
	private PrintStream backup;
	private ByteArrayOutputStream out;

	@Before
	public void setUp() throws Exception {
		try {
			backup = System.out;
			out = new ByteArrayOutputStream();
			System.setOut(new PrintStream(out, true));

			debugger = new TextDebugger("Test");
		} catch (IOException | IllegalConnectorArgumentsException
				| VMStartException e) {
		}
	}

	@After
	public void tearDown() throws Exception {
		System.setOut(backup);
	}

	@Test
	public void simpleTest() {
		debugger.perform("stop in Test.main");
		debugger.perform("stop at Test:11");
		debugger.perform("run");
		debugger.perform("cont");
		debugger.perform("cont");

		// compute expected output
		final String main = "Test.main(java.lang.String[])";
		StringBuilder sb = new StringBuilder();
		sb.append(format(DEFER_BREAKPOINT, "Test", "main"));
		sb.append(format(DEFER_BREAKPOINT_LOC, "Test", 11));
		sb.append(format(SET_BREAKPOINT, main, 4));
		sb.append(format(SET_BREAKPOINT, "Test.hello()", 11));
		sb.append(format(HIT_BREAKPOINT, "main", main, 4, 0));
		sb.append(format(HIT_BREAKPOINT, "main", "Test.hello()", 11, 0));
		sb.append(EXIT);
		assertEquals(sb.toString().trim(), out.toString().trim());
	}
	
	private static String format(Message msg, Object... args) {
		return String.format(msg + "\n", args);
	}

}
