package at.jku.ssw.ssw.jooksiklased.test;

import static at.jku.ssw.ssw.jooksiklased.Message.DEFER_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.EXIT;
import static at.jku.ssw.ssw.jooksiklased.Message.FIELD;
import static at.jku.ssw.ssw.jooksiklased.Message.HIT_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.INVALID_CMD;
import static at.jku.ssw.ssw.jooksiklased.Message.METHOD_OVERLOAD;
import static at.jku.ssw.ssw.jooksiklased.Message.NO_FIELD;
import static at.jku.ssw.ssw.jooksiklased.Message.NO_METHOD;
import static at.jku.ssw.ssw.jooksiklased.Message.REMOVE_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.RUN;
import static at.jku.ssw.ssw.jooksiklased.Message.SET_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.STEP;
import static at.jku.ssw.ssw.jooksiklased.Message.TOO_MANY_ARGS;
import static at.jku.ssw.ssw.jooksiklased.Message.USAGE;
import static at.jku.ssw.ssw.jooksiklased.Message.VAR;
import static at.jku.ssw.ssw.jooksiklased.Message.format;
import static org.junit.Assert.assertEquals;

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
		perform("stop at Overload:25 and do something");
		perform("run");
		perform("fields Overload");
		perform("print Overload.a");
		perform("stop in Overload.sub");
		perform("cont");

		final StringBuilder exp = new StringBuilder();
		exp.append(format(DEFER_BREAKPOINT, "Overload:25"));
		exp.append(format(TOO_MANY_ARGS, "and do something"));
		exp.append(format(RUN, "Overload"));
		exp.append(format(SET_BREAKPOINT, "Overload.add(int, long)", 25));
		exp.append(format(HIT_BREAKPOINT, "main", "Overload.add(int, long)",
				25, 0));
		exp.append(format(FIELD, "int", "i"));
		exp.append(format(NO_FIELD, "a", "Overload"));
		exp.append(format(NO_METHOD, "Overload.sub", "sub", "Overload"));
		exp.append(format(EXIT));
		assertEquals(exp.toString(), getOutput());
	}

	@Test
	public void nonStaticMethodTest() {
		perform("stop in Overload.mul");
		perform("run");
		perform("fields Overload");
		perform("print i");
		perform("step");
		perform("print i");
		perform("clear Overload.mul");
		perform("cont");

		final StringBuilder exp = new StringBuilder();
		exp.append(format(DEFER_BREAKPOINT, "Overload.mul"));
		exp.append(format(RUN, "Overload"));
		exp.append(format(SET_BREAKPOINT, "Overload.mul(int)", 33));
		exp.append(format(HIT_BREAKPOINT, "main", "Overload.mul(int)", 33, 0));
		exp.append(format(FIELD, "int", "i"));
		exp.append(format(VAR, "int", "i", "7"));
		exp.append(format(STEP, "main", "Overload.mul(int)", 34, 10));
		exp.append(format(VAR, "int", "i", "70"));
		exp.append(format(REMOVE_BREAKPOINT, "Overload.mul"));
		exp.append(format(EXIT));
		assertEquals(exp.toString(), getOutput());
	}

}
