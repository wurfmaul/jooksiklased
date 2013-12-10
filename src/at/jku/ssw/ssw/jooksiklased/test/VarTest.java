package at.jku.ssw.ssw.jooksiklased.test;

import static at.jku.ssw.ssw.jooksiklased.Message.DEFER_BREAKPOINT_LOC;
import static at.jku.ssw.ssw.jooksiklased.Message.EXIT;
import static at.jku.ssw.ssw.jooksiklased.Message.HIT_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.SET_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.VAR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VarTest extends AbstractTest {

	public VarTest() {
		classUnderTest = "Calc";
	}

	@Test
	public void localsTest() {
		// part 1
		debugger.perform("stop at Calc:16");
		debugger.perform("run");

		final String main = "Calc.main(java.lang.String[])";
		StringBuilder sb = new StringBuilder();
		sb.append(format(DEFER_BREAKPOINT_LOC, "Calc", 16));
		sb.append(format(SET_BREAKPOINT, main, 16));
		sb.append(format(HIT_BREAKPOINT, "main", main, 16, 0));
		assertEquals(sb.toString().trim(), out.toString().trim());
		out.reset();

		// part 2
		debugger.perform("locals");
		debugger.perform("cont");

		// deal with reference types
		String exp = format(VAR, "java.lang.String[]", "args",
				"instance of java.lang.String[](id=000)");
		assertTrue(ignoreId(exp, extractFirstLine(out)));
		exp = format(VAR, "Calc", "c", "instance of Calc(id=000)");
		assertTrue(ignoreId(exp, extractFirstLine(out)));

		// deal with native types
		sb = new StringBuilder();
		sb.append(format(VAR, "int", "tmp", "42"));
		sb.append(format(VAR, "boolean", "cmp", "true"));
		sb.append(EXIT);
		assertEquals(sb.toString().trim(), out.toString().trim());

	}
}
