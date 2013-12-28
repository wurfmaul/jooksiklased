package at.jku.ssw.ssw.jooksiklased.test;

import static at.jku.ssw.ssw.jooksiklased.Message.BREAKPOINT_NOT_FOUND;
import static at.jku.ssw.ssw.jooksiklased.Message.DEFER_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.EXIT;
import static at.jku.ssw.ssw.jooksiklased.Message.HIT_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.LIST_BREAKPOINTS;
import static at.jku.ssw.ssw.jooksiklased.Message.REMOVE_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.RUN;
import static at.jku.ssw.ssw.jooksiklased.Message.SET_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.format;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BreakpointTest extends AbstractTest {
	private static final String MAIN = "Test.main(java.lang.String[])";

	public BreakpointTest() {
		args = new String[] { "Test" };
	}

	@Test
	public void deleteBreakpointTest() {
		perform("stop in Test.main");
		perform("stop in Test.hello");
		perform("stop at Test:11");
		perform("run");
		perform("clear Test.hello");
		perform("clear Test:11");
		perform("cont");

		final StringBuilder exp = new StringBuilder();
		exp.append(format(DEFER_BREAKPOINT, "Test.main"));
		exp.append(format(DEFER_BREAKPOINT, "Test.hello"));
		exp.append(format(DEFER_BREAKPOINT, "Test:11"));
		exp.append(format(RUN, "Test"));
		exp.append(format(SET_BREAKPOINT, MAIN, 4));
		exp.append(format(SET_BREAKPOINT, "Test.hello(int)", 10));
		exp.append(format(SET_BREAKPOINT, "Test.hello(int)", 11));
		exp.append(format(HIT_BREAKPOINT, "main", MAIN, 4, 0));
		exp.append(format(REMOVE_BREAKPOINT, "Test.hello"));
		exp.append(format(REMOVE_BREAKPOINT, "Test:11"));
		exp.append(format(EXIT));
		assertEquals(exp.toString(), getOutput());
	}

	@Test
	public void deleteNonExistentBreakpoint() {
		perform("stop in Test.main");
		perform("stop in Test.hello");
		perform("clear Test.hello");
		perform("clear Test.hello");
		perform("run");
		perform("clear Test:42");
		perform("cont");

		final StringBuilder exp = new StringBuilder();
		exp.append(format(DEFER_BREAKPOINT, "Test.main"));
		exp.append(format(DEFER_BREAKPOINT, "Test.hello"));
		exp.append(format(REMOVE_BREAKPOINT, "Test.hello"));
		exp.append(format(BREAKPOINT_NOT_FOUND, "Test.hello"));
		exp.append(format(RUN, "Test"));
		exp.append(format(SET_BREAKPOINT, MAIN, 4));
		exp.append(format(HIT_BREAKPOINT, "main", MAIN, 4, 0));
		exp.append(format(BREAKPOINT_NOT_FOUND, "Test:42"));
		exp.append(format(EXIT));
		assertEquals(exp.toString(), getOutput());
	}

	@Test
	public void noBreakpointsTest() {
		perform("run");
		final StringBuilder exp = new StringBuilder();
		exp.append(format(RUN, "Test"));
		exp.append(format(EXIT));
		assertEquals(exp.toString(), getOutput());
	}

	@Test
	public void printBreakpointsTest() {
		perform("stop in Test.main");
		perform("stop in Test.hello");
		perform("stop at Test:5");
		perform("run");
		perform("stop");
		perform("exit");

		final String bps = "\tTest.main: 4\n\tTest.hello: 10\n\tTest.main: 5";
		final StringBuilder exp = new StringBuilder();
		exp.append(format(DEFER_BREAKPOINT, "Test.main"));
		exp.append(format(DEFER_BREAKPOINT, "Test.hello"));
		exp.append(format(DEFER_BREAKPOINT, "Test:5"));
		exp.append(format(RUN, "Test"));
		exp.append(format(SET_BREAKPOINT, MAIN, 4));
		exp.append(format(SET_BREAKPOINT, "Test.hello(int)", 10));
		exp.append(format(SET_BREAKPOINT, MAIN, 5));
		exp.append(format(HIT_BREAKPOINT, "main", MAIN, 4, 0));
		exp.append(format(LIST_BREAKPOINTS, bps));
		assertEquals(exp.toString(), getOutput());
	}

	@Test
	public void simpleBreakpointTest() {
		perform("stop at Test:11");
		perform("run");
		perform("cont");

		final StringBuilder exp = new StringBuilder();
		exp.append(format(DEFER_BREAKPOINT, "Test:11"));
		exp.append(format(RUN, "Test"));
		exp.append(format(SET_BREAKPOINT, "Test.hello(int)", 11));
		exp.append(format(HIT_BREAKPOINT, "main", "Test.hello(int)", 11, 15));
		exp.append(format(EXIT));
		assertEquals(exp.toString(), getOutput());
	}

	@Test
	public void startUpBreakpointTest() {
		perform("stop in Test.main");
		perform("stop at Test:11");
		perform("run");
		perform("cont");
		perform("cont");

		final StringBuilder exp = new StringBuilder();
		exp.append(format(DEFER_BREAKPOINT, "Test.main"));
		exp.append(format(DEFER_BREAKPOINT, "Test:11"));
		exp.append(format(RUN, "Test"));
		exp.append(format(SET_BREAKPOINT, MAIN, 4));
		exp.append(format(SET_BREAKPOINT, "Test.hello(int)", 11));
		exp.append(format(HIT_BREAKPOINT, "main", MAIN, 4, 0));
		exp.append(format(HIT_BREAKPOINT, "main", "Test.hello(int)", 11, 15));
		exp.append(format(EXIT));
		assertEquals(exp.toString(), getOutput());
	}

}
