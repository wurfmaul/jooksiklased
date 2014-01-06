package at.jku.ssw.ssw.jooksiklased.test;

import static at.jku.ssw.ssw.jooksiklased.Message.BREAKPOINT_NOT_FOUND;
import static at.jku.ssw.ssw.jooksiklased.Message.DEFER_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.EXIT;
import static at.jku.ssw.ssw.jooksiklased.Message.HIT_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.NO_LOCALS;
import static at.jku.ssw.ssw.jooksiklased.Message.RUN;
import static at.jku.ssw.ssw.jooksiklased.Message.SET_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.STEP;
import static at.jku.ssw.ssw.jooksiklased.Message.TRACE_LOC;
import static at.jku.ssw.ssw.jooksiklased.Message.TRACE_SRC;
import static at.jku.ssw.ssw.jooksiklased.Message.VM_NOT_RUNNING;
import static at.jku.ssw.ssw.jooksiklased.Message.VM_RUNNING;
import static at.jku.ssw.ssw.jooksiklased.Message.format;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RecursionTest extends AbstractTest {
	private static final String ARGS = "java.lang.String[]";
	private static final String MAIN = "Recursion.main(" + ARGS + ")";
	private static final String CLASS = "Recursion.java";

	public RecursionTest() {
		args = new String[] { "Recursion" };
	}

	@Test
	public void whereTest() {
		perform("stop at Recursion:14");
		perform("run");
		perform("where");
		perform("cont");
		perform("exit");

		final StringBuilder exp = new StringBuilder();
		exp.append(format(DEFER_BREAKPOINT, "Recursion:14"));
		exp.append(format(RUN, "Recursion"));
		exp.append(format(SET_BREAKPOINT, "Recursion.fac(int)", 14));
		exp.append(format(HIT_BREAKPOINT, "main", "Recursion.fac(int)", 14, 21));
		exp.append(format(TRACE_LOC, 1, "Recursion", "fac", CLASS, 14));
		exp.append(format(TRACE_SRC, 2, "Recursion", "fac", CLASS));
		exp.append(format(TRACE_SRC, 3, "Recursion", "fac", CLASS));
		exp.append(format(TRACE_SRC, 4, "Recursion", "fac", CLASS));
		exp.append(format(TRACE_SRC, 5, "Recursion", "fac", CLASS));
		exp.append(format(TRACE_SRC, 6, "Recursion", "fac", CLASS));
		exp.append(format(TRACE_SRC, 7, "Recursion", "fac", CLASS));
		exp.append(format(TRACE_SRC, 8, "Recursion", "main", CLASS));
		exp.append(format(HIT_BREAKPOINT, "main", "Recursion.fac(int)", 14, 21));
		assertEquals(exp.toString(), getOutput());
	}

	@Test
	public void emptyBreakpointTest() {
		perform("stop in Recursion.empty");
		perform("run");
		perform("locals");
		perform("step");
		perform("cont");

		final StringBuilder exp = new StringBuilder();
		exp.append(format(DEFER_BREAKPOINT, "Recursion.empty"));
		exp.append(format(RUN, "Recursion"));
		exp.append(format(SET_BREAKPOINT, "Recursion.empty()", 18));
		exp.append(format(HIT_BREAKPOINT, "main", "Recursion.empty()", 18, 0));
		exp.append(format(NO_LOCALS));
		exp.append(format(STEP, "main", "Recursion.main(" + ARGS + ")", 6, 9));
		exp.append(format(EXIT));
		assertEquals(exp.toString(), getOutput());
	}

	@Test
	public void vmStatusTest() {
		perform("cont");
		perform("locals");
		perform("step");
		perform("stop in Recursion.main");
		perform("run");
		perform("run");
		perform("clear Recursion.fac");
		perform("cont");

		final StringBuilder exp = new StringBuilder();
		exp.append(format(VM_NOT_RUNNING, "cont"));
		exp.append(format(VM_NOT_RUNNING, "locals"));
		exp.append(format(VM_NOT_RUNNING, "step"));
		exp.append(format(DEFER_BREAKPOINT, "Recursion.main"));
		exp.append(format(RUN, "Recursion"));
		exp.append(format(SET_BREAKPOINT, "Recursion.main(" + ARGS + ")", 4));
		exp.append(format(HIT_BREAKPOINT, "main", MAIN, 4, 0));
		exp.append(format(VM_RUNNING));
		exp.append(format(BREAKPOINT_NOT_FOUND, "Recursion.fac"));
		exp.append(format(EXIT));
		assertEquals(exp.toString(), getOutput());
	}

}
