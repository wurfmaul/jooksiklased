package at.jku.ssw.ssw.jooksiklased.test;

import static at.jku.ssw.ssw.jooksiklased.Message.*;
import static org.junit.Assert.*;

import org.junit.Test;

public class OverloadTest extends AbstractTest {

	public OverloadTest() {
		classUnderTest = "Overload";
	}

	@Test
	public void overloadTest() {
		perform("stop in Overload.add");
		perform("run");

		final StringBuilder exp = new StringBuilder();
		exp.append(format(DEFER_BREAKPOINT, "Overload.add"));
		exp.append(format(RUN, "Overload"));
		exp.append(format(METHOD_OVERLOAD, "Overload.add", "add"));
		exp.append(format(EXIT));
		assertEquals(exp.toString(), getOutput());
	}

	@Test
	public void invalidCmdTest() {
		perform("stopp please");
		perform("do something");
		perform("run");

		final StringBuilder exp = new StringBuilder();
		exp.append(format(INVALID_CMD, "stopp please"));
		exp.append(format(USAGE));
		exp.append(format(INVALID_CMD, "do something"));
		exp.append(format(USAGE));
		exp.append(format(RUN, "Overload"));
		exp.append(format(EXIT));
		assertEquals(exp.toString(), getOutput());
	}

	@Test
	public void noSuchComponentTest() {
		perform("stop at Overload:15 and do something");
		perform("run");
		perform("fields Overload");
		perform("print Overload.a");
		perform("stop in Overload.sub");
		perform("cont");

		final StringBuilder exp = new StringBuilder();
		exp.append(format(DEFER_BREAKPOINT, "Overload:15"));
		exp.append(format(TOO_MANY_ARGS, "and do something"));
		exp.append(format(RUN, "Overload"));
		exp.append(format(SET_BREAKPOINT, "Overload.add(int, long)", 15));
		exp.append(format(HIT_BREAKPOINT, "main", "Overload.add(int, long)", 15,
				0));
		exp.append(format(NO_FIELDS, "Overload"));
		exp.append(format(NO_FIELD, "a", "Overload"));
		exp.append(format(NO_METHOD, "Overload.sub", "sub", "Overload"));
		exp.append(format(EXIT));
		assertEquals(exp.toString(), getOutput());
	}

}
