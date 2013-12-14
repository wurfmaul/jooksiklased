package at.jku.ssw.ssw.jooksiklased.test;

import static at.jku.ssw.ssw.jooksiklased.Message.DEFER_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.EXIT;
import static at.jku.ssw.ssw.jooksiklased.Message.FIELD;
import static at.jku.ssw.ssw.jooksiklased.Message.HIT_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.RUN;
import static at.jku.ssw.ssw.jooksiklased.Message.SET_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.STEP;
import static at.jku.ssw.ssw.jooksiklased.Message.TRACE;
import static at.jku.ssw.ssw.jooksiklased.Message.VAR;
import static at.jku.ssw.ssw.jooksiklased.Message.format;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class VarTest extends AbstractTest {
	private static final String MAIN = "Calc.main(java.lang.String[])";
	private static final String ARGS = "java.lang.String[]";

	public VarTest() {
		classUnderTest = "Calc";
	}

	@Test
	public void localsTest() {
		perform("stop at Calc:16");
		perform("run");
		perform("locals");
		perform("cont");

		final StringBuilder exp = new StringBuilder();
		exp.append(format(DEFER_BREAKPOINT, "Calc:16"));
		exp.append(format(RUN, "Calc"));
		exp.append(format(SET_BREAKPOINT, MAIN, 16));
		exp.append(format(HIT_BREAKPOINT, "main", MAIN, 16, 20));
		exp.append(format(VAR, ARGS, "args", "instance of " + ARGS + " (id=0)"));
		exp.append(format(VAR, "Calc", "c", "instance of Calc (id=0)"));
		exp.append(format(VAR, "int", "tmp", "42"));
		exp.append(format(VAR, "boolean", "cmp", "true"));
		exp.append(EXIT);
		assertEqualsIgnoreId(exp.toString(), getOutput());
	}

	@Test
	public void printTest() {
		perform("stop at Calc:16");
		perform("run");
		perform("print c");
		perform("dump c");
		perform("print args");
		perform("dump args");
		perform("cont");

		final String c = "[a=12, b=13, c=\"42\", d=8]";
		final String args = "{}";
		final StringBuilder exp = new StringBuilder();
		exp.append(format(DEFER_BREAKPOINT, "Calc:16"));
		exp.append(format(RUN, "Calc"));
		exp.append(format(SET_BREAKPOINT, MAIN, 16));
		exp.append(format(HIT_BREAKPOINT, "main", MAIN, 16, 20));
		exp.append(format(VAR, "Calc", "c", "instance of Calc (id=0)"));
		exp.append(format(VAR, "Calc", "c", c));
		exp.append(format(VAR, ARGS, "args", "instance of " + ARGS + " (id=0)"));
		exp.append(format(VAR, ARGS, "args", args));
		exp.append(EXIT);
		assertEqualsIgnoreId(exp.toString(), getOutput());
	}

	@Test
	public void stackTraceTest() {
		perform("stop at Calc:26");
		perform("run");
		perform("where");
		perform("cont");

		final String trace = "\t[1] Calc.calc (Calc.java)\n"
				+ "\t[2] Calc.main (Calc.java)";
		final StringBuilder exp = new StringBuilder();
		exp.append(format(DEFER_BREAKPOINT, "Calc:26"));
		exp.append(format(RUN, "Calc"));
		exp.append(format(SET_BREAKPOINT, "Calc.calc(float)", 26));
		exp.append(format(HIT_BREAKPOINT, "main", "Calc.calc(float)", 26, 14));
		exp.append(format(TRACE, trace));
		exp.append(format(EXIT));
		assertEquals(exp.toString(), getOutput());
	}

	@Test
	public void stepTest() {
		perform("stop in Calc.calc");
		perform("run");
		perform("locals");
		perform("step");
		perform("locals");
		perform("cont");

		final StringBuilder exp = new StringBuilder();
		exp.append(format(DEFER_BREAKPOINT, "Calc.calc"));
		exp.append(format(RUN, "Calc"));
		exp.append(format(SET_BREAKPOINT, "Calc.calc(float)", 24));
		exp.append(format(HIT_BREAKPOINT, "main", "Calc.calc(float)", 24, 0));
		exp.append(format(VAR, "float", "val", "2.0"));
		exp.append(format(STEP, "main", "Calc.calc(float)", 25, 9));
		exp.append(format(VAR, "float", "val", "2.0"));
		exp.append(format(VAR, "double", "tmp", "25.0"));
		exp.append(format(EXIT));
		assertEquals(exp.toString(), getOutput());
	}

	@Test
	public void fieldTest() {
		perform("stop in Calc.main");
		perform("run");
		perform("print Calc.d");
		perform("print d");
		perform("step");
		perform("print d");
		perform("cont");

		final StringBuilder exp = new StringBuilder();
		exp.append(format(DEFER_BREAKPOINT, "Calc.main"));
		exp.append(format(RUN, "Calc"));
		exp.append(format(SET_BREAKPOINT, MAIN, 13));
		exp.append(format(HIT_BREAKPOINT, "main", MAIN, 13, 0));
		exp.append(format(FIELD, "long", "Calc.d"));
		exp.append(format(FIELD, "long", "d"));
		exp.append(format(STEP, "main", MAIN, 14, 8));
		exp.append(format(VAR, "long", "d", "8"));
		exp.append(format(EXIT));
		assertEquals(exp.toString(), getOutput());
	}
}
