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
		exp.append(format(INVALID_CMD, "do something"));
		exp.append(format(EXIT));
		assertEquals(exp.toString(), getOutput());
	}

}
