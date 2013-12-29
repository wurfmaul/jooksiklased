package at.jku.ssw.ssw.jooksiklased.test;

import static at.jku.ssw.ssw.jooksiklased.Message.DEFER_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.EXIT;
import static at.jku.ssw.ssw.jooksiklased.Message.HIT_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.RUN;
import static at.jku.ssw.ssw.jooksiklased.Message.SET_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.THREAD_GROUP;
import static at.jku.ssw.ssw.jooksiklased.Message.THREAD_STATUS;
import static at.jku.ssw.ssw.jooksiklased.Message.THREAD_STATUS_BP;
import static at.jku.ssw.ssw.jooksiklased.Message.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class ThreadTest extends AbstractTest {
	private static final String CL = "ArgThread";
	private static final String ARGS = "java.lang.String[]";
	private static final String MAIN = "ArgThread.main(" + ARGS + ")";

	public ThreadTest() {
		args = new String[] { "ArgThread", "12", "42" };
	}

	@Test
	public void printThreadsTest() {
		perform("stop at ArgThread:31");
		perform("run");
		perform("threads");
		perform("cont");

		// short cuts for readability
		final String cl12 = "java.lang.Thread";
		final String cl14 = "java.lang.ref.Finalizer$FinalizerThread";
		final String cl15 = "java.lang.ref.Reference$ReferenceHandler";
		final String name13 = "Signal Dispatcher";
		final String name15 = "Reference Handler";

		final StringBuilder exp = new StringBuilder();
		exp.append(format(DEFER_BREAKPOINT, "ArgThread:31"));
		exp.append(format(RUN, CL));
		exp.append(format(SET_BREAKPOINT, MAIN, 31));
		exp.append(format(HIT_BREAKPOINT, "main", MAIN, 31, 76));
		exp.append(format(THREAD_GROUP, "main"));
		exp.append(format(THREAD_STATUS, 0, CL, 0x45, "Thread-11", "running"));
		exp.append(format(THREAD_STATUS, 1, CL, 0x46, "Thread-10", "sleeping"));
		exp.append(format(THREAD_STATUS, 2, CL, 0x47, "Thread-9", "sleeping"));
		exp.append(format(THREAD_STATUS, 3, CL, 0x48, "Thread-8", "sleeping"));
		exp.append(format(THREAD_STATUS, 4, CL, 0x49, "Thread-7", "sleeping"));
		exp.append(format(THREAD_STATUS, 5, CL, 0x4a, "Thread-6", "sleeping"));
		exp.append(format(THREAD_STATUS, 6, CL, 0x4b, "Thread-5", "sleeping"));
		exp.append(format(THREAD_STATUS, 7, CL, 0x4c, "Thread-4", "sleeping"));
		exp.append(format(THREAD_STATUS, 8, CL, 0x4d, "Thread-3", "sleeping"));
		exp.append(format(THREAD_STATUS, 9, CL, 0x4e, "Thread-2", "sleeping"));
		exp.append(format(THREAD_STATUS, 10, CL, 0x4f, "Thread-1", "sleeping"));
		exp.append(format(THREAD_STATUS, 11, CL, 0x50, "Thread-0", "sleeping"));
		exp.append(format(THREAD_STATUS_BP, 12, cl12, 0x1, "main", "running"));
		exp.append(format(THREAD_GROUP, "system"));
		exp.append(format(THREAD_STATUS, 13, cl12, 0x2, name13, "running"));
		exp.append(format(THREAD_STATUS, 14, cl14, 0x3, "Finalizer", "waiting"));
		exp.append(format(THREAD_STATUS, 15, cl15, 0x4, name15, "waiting"));
		exp.append(format(EXIT));

		assertEquals(exp.toString(), getOutput());
	}

	@Test
	public void noLineTest() {
		perform("stop at ArgThread:33");
		perform("run");
		perform("stop at ArgThread:75");

		System.out.println(getOutput());

		fail();
	}
}
