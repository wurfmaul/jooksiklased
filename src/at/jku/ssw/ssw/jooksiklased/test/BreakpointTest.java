package at.jku.ssw.ssw.jooksiklased.test;

import static at.jku.ssw.ssw.jooksiklased.Message.DEFER_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.DEFER_BREAKPOINT_LOC;
import static at.jku.ssw.ssw.jooksiklased.Message.EXIT;
import static at.jku.ssw.ssw.jooksiklased.Message.HIT_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.LIST_BREAKPOINTS;
import static at.jku.ssw.ssw.jooksiklased.Message.SET_BREAKPOINT;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BreakpointTest extends AbstractTest {
	private static final String MAIN = "Test.main(java.lang.String[])";

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

		final StringBuilder exp = new StringBuilder();
		exp.append(format(DEFER_BREAKPOINT, "Test", "main"));
		exp.append(format(DEFER_BREAKPOINT_LOC, "Test", 11));
		exp.append(format(SET_BREAKPOINT, MAIN, 4));
		exp.append(format(SET_BREAKPOINT, "Test.hello(int)", 11));
		exp.append(format(HIT_BREAKPOINT, "main", MAIN, 4, 0));
		exp.append(format(HIT_BREAKPOINT, "main", "Test.hello(int)", 11, 15));
		exp.append(EXIT);
		assertEquals(exp.toString().trim(), out.toString().trim());
	}

	@Test
	public void simpleBreakpointTest() {
		debugger.perform("stop at Test:11");
		debugger.perform("run");
		debugger.perform("cont");

		final StringBuilder exp = new StringBuilder();
		exp.append(format(DEFER_BREAKPOINT_LOC, "Test", 11));
		exp.append(format(SET_BREAKPOINT, "Test.hello(int)", 11));
		exp.append(format(HIT_BREAKPOINT, "main", "Test.hello(int)", 11, 15));
		exp.append(EXIT);
		assertEquals(exp.toString().trim(), out.toString().trim());
	}

	@Test
	public void printBreakpointTest() {
		debugger.perform("stop in Test.main");
		debugger.perform("stop in Test.hello");
		debugger.perform("stop at Test:5");
		debugger.perform("run");
		debugger.perform("stop");
		debugger.perform("exit");

		final String bps = "\tTest.main: 4\n\tTest.hello: 10\n\tTest.main: 5";
		final StringBuilder sb = new StringBuilder();
		sb.append(format(DEFER_BREAKPOINT, "Test", "main"));
		sb.append(format(DEFER_BREAKPOINT, "Test", "hello"));
		sb.append(format(DEFER_BREAKPOINT_LOC, "Test", 5));
		sb.append(format(SET_BREAKPOINT, MAIN, 4));
		sb.append(format(SET_BREAKPOINT, "Test.hello(int)", 10));
		sb.append(format(SET_BREAKPOINT, MAIN, 5));
		sb.append(format(HIT_BREAKPOINT, "main", MAIN, 4, 0));
		sb.append(format(LIST_BREAKPOINTS, bps));
		assertEquals(sb.toString().trim(), out.toString().trim());
	}

	@Test
	public void runTest() {
		debugger.perform("run");
		assertEquals(EXIT.toString(), out.toString().trim());
	}

}
