package at.jku.ssw.ssw.jooksiklased.test;

import static at.jku.ssw.ssw.jooksiklased.Message.DEFER_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.DEFER_BREAKPOINT_LOC;
import static at.jku.ssw.ssw.jooksiklased.Message.EXIT;
import static at.jku.ssw.ssw.jooksiklased.Message.HIT_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.SET_BREAKPOINT;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BreakpointTest extends AbstractTest {

	public BreakpointTest() {
		classUnderTest = "Test";
	}

	@Test
	public void startUpBreakpointTest() {
		debugger.perform("stop in Test.main");
		debugger.perform("stop at Test:11");
		debugger.perform("run");
		debugger.perform("cont");
		debugger.perform("cont");

		final String main = "Test.main(java.lang.String[])";
		final StringBuilder sb = new StringBuilder();
		sb.append(format(DEFER_BREAKPOINT, "Test", "main"));
		sb.append(format(DEFER_BREAKPOINT_LOC, "Test", 11));
		sb.append(format(SET_BREAKPOINT, main, 4));
		sb.append(format(SET_BREAKPOINT, "Test.hello(int)", 11));
		sb.append(format(HIT_BREAKPOINT, "main", main, 4, 0));
		sb.append(format(HIT_BREAKPOINT, "main", "Test.hello(int)", 11, 0));
		sb.append(EXIT);
		assertEquals(sb.toString().trim(), out.toString().trim());
	}

	@Test
	public void simpleBreakpointTest() {
		debugger.perform("stop at Test:11");
		debugger.perform("run");
		debugger.perform("cont");

		StringBuilder sb = new StringBuilder();
		sb.append(format(DEFER_BREAKPOINT_LOC, "Test", 11));
		sb.append(format(SET_BREAKPOINT, "Test.hello(int)", 11));
		sb.append(format(HIT_BREAKPOINT, "main", "Test.hello(int)", 11, 0));
		sb.append(EXIT);
		assertEquals(sb.toString().trim(), out.toString().trim());
	}

	@Test
	public void runTest() {
		debugger.perform("run");
		assertEquals(EXIT.toString(), out.toString().trim());
	}

}
