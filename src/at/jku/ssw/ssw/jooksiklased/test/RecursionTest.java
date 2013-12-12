package at.jku.ssw.ssw.jooksiklased.test;

import static at.jku.ssw.ssw.jooksiklased.Message.DEFER_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.HIT_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.SET_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.TRACE;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RecursionTest extends AbstractTest {

	public RecursionTest() {
		classUnderTest = "Recursion";
	}

	@Test
	public void whereTest() {
		debugger.perform("stop at Recursion:13");
		debugger.perform("run");
		debugger.perform("where");
		debugger.perform("cont");
		debugger.perform("exit");

		final String trace = "\t[1] Recursion.fac (Recursion.java)\n"
				+ "\t[2] Recursion.fac (Recursion.java)\n"
				+ "\t[3] Recursion.fac (Recursion.java)\n"
				+ "\t[4] Recursion.fac (Recursion.java)\n"
				+ "\t[5] Recursion.fac (Recursion.java)\n"
				+ "\t[6] Recursion.fac (Recursion.java)\n"
				+ "\t[7] Recursion.fac (Recursion.java)\n"
				+ "\t[8] Recursion.main (Recursion.java)";

		final StringBuilder exp = new StringBuilder();
		exp.append(format(DEFER_BREAKPOINT, "Recursion:13"));
		exp.append(format(SET_BREAKPOINT, "Recursion.fac(int)", 13));
		exp.append(format(HIT_BREAKPOINT, "main", "Recursion.fac(int)", 13, 21));
		exp.append(format(TRACE, trace));
		exp.append(format(HIT_BREAKPOINT, "main", "Recursion.fac(int)", 13, 21));
		assertEquals(exp.toString().trim(), out.toString().trim());
	}
	
}
