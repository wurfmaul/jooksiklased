package at.jku.ssw.ssw.jooksiklased.test;

import static at.jku.ssw.ssw.jooksiklased.Message.*;
import static org.junit.Assert.*;

import org.junit.Test;

public class ThreadTest extends AbstractTest {
	private static final String CLASS = "ArgThread";
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

		final StringBuilder exp = new StringBuilder();
		exp.append(format(DEFER_BREAKPOINT, "ArgThread:31"));
		exp.append(format(RUN, CLASS));
		exp.append(format(SET_BREAKPOINT, MAIN, 31));
		exp.append(format(HIT_BREAKPOINT, "main", MAIN, 31, 76));
		exp.append(format(THREAD_GROUP, "main"));
		exp.append(format(THREAD_STATUS, CLASS, 0x45, "Thread-11", "running"));
		exp.append(format(THREAD_STATUS, CLASS, 0x46, "Thread-10", "sleeping"));
		exp.append(format(THREAD_STATUS, CLASS, 0x47, "Thread-9", "sleeping"));
		exp.append(format(THREAD_STATUS, CLASS, 0x48, "Thread-8", "sleeping"));
		exp.append(format(THREAD_STATUS, CLASS, 0x49, "Thread-7", "sleeping"));
		exp.append(format(THREAD_STATUS, CLASS, 0x4a, "Thread-6", "sleeping"));
		exp.append(format(THREAD_STATUS, CLASS, 0x4b, "Thread-5", "sleeping"));
		exp.append(format(THREAD_STATUS, CLASS, 0x4c, "Thread-4", "sleeping"));
		exp.append(format(THREAD_STATUS, CLASS, 0x4d, "Thread-3", "sleeping"));
		exp.append(format(THREAD_STATUS, CLASS, 0x4e, "Thread-2", "sleeping"));
		exp.append(format(THREAD_STATUS, CLASS, 0x4f, "Thread-1", "sleeping"));
		exp.append(format(THREAD_STATUS, CLASS, 0x50, "Thread-0", "sleeping"));
		exp.append(format(THREAD_STATUS_BREAKPOINT, "java.lang.Thread", 0x1,
				"main", "running"));
		exp.append(format(THREAD_GROUP, "system"));
		exp.append(format(THREAD_STATUS, "java.lang.Thread", 0x2,
				"Signal Dispatcher", "running"));
		exp.append(format(THREAD_STATUS,
				"java.lang.ref.Finalizer$FinalizerThread", 0x3, "Finalizer",
				"waiting"));
		exp.append(format(THREAD_STATUS,
				"java.lang.ref.Reference$ReferenceHandler", 0x4,
				"Reference Handler", "waiting"));
		exp.append(format(EXIT));

		assertEquals(exp.toString(), getOutput());
	}
}
