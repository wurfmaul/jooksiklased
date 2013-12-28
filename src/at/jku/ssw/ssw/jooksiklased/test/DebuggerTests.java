package at.jku.ssw.ssw.jooksiklased.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ BreakpointTest.class, VarTest.class, RecursionTest.class,
		OverloadTest.class, ThreadTest.class })
public class DebuggerTests {

}
