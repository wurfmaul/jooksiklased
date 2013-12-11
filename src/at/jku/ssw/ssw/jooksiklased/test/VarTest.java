package at.jku.ssw.ssw.jooksiklased.test;

import static at.jku.ssw.ssw.jooksiklased.Message.DEFER_BREAKPOINT_LOC;
import static at.jku.ssw.ssw.jooksiklased.Message.EXIT;
import static at.jku.ssw.ssw.jooksiklased.Message.HIT_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.SET_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.VAR;

import org.junit.Test;

public class VarTest extends AbstractTest {
	private static final String MAIN = "Calc.main(java.lang.String[])";
	private static final String ARGS = "java.lang.String[]";

	public VarTest() {
		classUnderTest = "Calc";
	}

	@Test
	public void localsTest() {
		debugger.perform("stop at Calc:16");
		debugger.perform("run");
		debugger.perform("locals");
		debugger.perform("cont");

		final StringBuilder exp = new StringBuilder();
		exp.append(format(DEFER_BREAKPOINT_LOC, "Calc", 16));
		exp.append(format(SET_BREAKPOINT, MAIN, 16));
		exp.append(format(HIT_BREAKPOINT, "main", MAIN, 16, 0));
		exp.append(format(VAR, ARGS, "args", "instance of " + ARGS + " (id=0)"));
		exp.append(format(VAR, "Calc", "c", "instance of Calc (id=0)"));
		exp.append(format(VAR, "int", "tmp", "42"));
		exp.append(format(VAR, "boolean", "cmp", "true"));
		exp.append(EXIT);
		assertEqualsIgnoreId(exp.toString().trim(), out.toString().trim());
	}
	
	@Test
	public void printTest() {
		debugger.perform("stop at Calc:16");
		debugger.perform("run");
		debugger.perform("print c");
		debugger.perform("dump c");
		debugger.perform("print args");
		debugger.perform("dump args");
		debugger.perform("cont");
		
		final StringBuilder exp = new StringBuilder();
		final String c = "[a=12, b=13, c=\"42\", d=8]";
		final String args = "{}";
		exp.append(format(DEFER_BREAKPOINT_LOC, "Calc", 16));
		exp.append(format(SET_BREAKPOINT, MAIN, 16));
		exp.append(format(HIT_BREAKPOINT, "main", MAIN, 16, 0));
		exp.append(format(VAR, "Calc", "c", "instance of Calc (id=0)"));
		exp.append(format(VAR, "Calc", "c", c));
		exp.append(format(VAR, ARGS, "args", "instance of " + ARGS + " (id=0)"));
		exp.append(format(VAR, ARGS, "args", args));
		exp.append(EXIT);
		assertEqualsIgnoreId(exp.toString().trim(), out.toString().trim());
	}
}