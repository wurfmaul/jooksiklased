package at.jku.ssw.ssw.jooksiklased.test;

import static at.jku.ssw.ssw.jooksiklased.Message.BREAKPOINT_ERROR;
import static at.jku.ssw.ssw.jooksiklased.Message.DEFER_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.EXIT;
import static at.jku.ssw.ssw.jooksiklased.Message.HIT_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.NO_LOCAL_INFO;
import static at.jku.ssw.ssw.jooksiklased.Message.RUN;
import static at.jku.ssw.ssw.jooksiklased.Message.SET_BREAKPOINT;
import static at.jku.ssw.ssw.jooksiklased.Message.STEP;
import static at.jku.ssw.ssw.jooksiklased.Message.THREAD_GROUP;
import static at.jku.ssw.ssw.jooksiklased.Message.THREAD_STATUS;
import static at.jku.ssw.ssw.jooksiklased.Message.THREAD_STATUS_BP;
import static at.jku.ssw.ssw.jooksiklased.Message.TRACE_SRC;
import static at.jku.ssw.ssw.jooksiklased.Message.UNKNOWN;
import static at.jku.ssw.ssw.jooksiklased.Message.VAR;
import static at.jku.ssw.ssw.jooksiklased.Message.format;
import static org.junit.Assert.assertEquals;

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

		final StringBuilder exp = new StringBuilder();
		exp.append(format(DEFER_BREAKPOINT, "ArgThread:31"));
		exp.append(format(RUN, CL));
		exp.append(format(SET_BREAKPOINT, MAIN, 31));
		exp.append(format(HIT_BREAKPOINT, "main", MAIN, 31, 76));
		appendThreads(exp);
		exp.append(format(EXIT));

		assertEquals(exp.toString(), getOutput());
	}

	@Test
	public void threadSwitchTest() {
		perform("stop at ArgThread:31");
		perform("run");
		perform("threads");
		perform("thread 1");
		perform("locals");
		perform("dump sum");
		perform("where");
		perform("step");
		perform("dump sum");
		perform("thread 12");
		perform("cont");

		final StringBuilder exp = new StringBuilder();
		exp.append(format(DEFER_BREAKPOINT, "ArgThread:31"));
		exp.append(format(RUN, CL));
		exp.append(format(SET_BREAKPOINT, MAIN, 31));
		exp.append(format(HIT_BREAKPOINT, "main", MAIN, 31, 76));
		appendThreads(exp);
		exp.append(format(NO_LOCAL_INFO));
		exp.append(format(UNKNOWN, "sum"));
		exp.append(format(TRACE_SRC, 1, "ArgThread", "main", "ArgThread.java"));
		exp.append(format(STEP, "Thread-10", "ArgThread.run()", 19, 39));
		exp.append(format(VAR, "long", "sum", "903"));
		exp.append(format(EXIT));
		assertEquals(exp.toString(), getOutput());
	}

	@Test
	public void noLineTest() {
		perform("stop at ArgThread:33");
		perform("stop at ArgThread:65");
		perform("run");
		perform("stop at ArgThread:75");

		final StringBuilder exp = new StringBuilder();
		exp.append(format(DEFER_BREAKPOINT, "ArgThread:33"));
		exp.append(format(DEFER_BREAKPOINT, "ArgThread:65"));
		exp.append(format(RUN, CL));
		exp.append(format(SET_BREAKPOINT, MAIN, 34));
		exp.append(format(BREAKPOINT_ERROR, "ArgThread:65", 65, CL));
		exp.append(format(HIT_BREAKPOINT, "main", MAIN, 34, 85));
		exp.append(format(BREAKPOINT_ERROR, "ArgThread:75", 75, CL));

		assertEquals(exp.toString(), getOutput());
	}

	/**
	 * Little helper to avoid double coding all threads' states. Very specific
	 * function as it prints all threads for a breakpoint at line number 31.
	 * 
	 * @param sb
	 *            StringBuilder to which the information is appended.
	 */
	private void appendThreads(final StringBuilder sb) {
		// shortcuts for readability
		final String cl12 = "java.lang.Thread";
		final String cl14 = "java.lang.ref.Finalizer$FinalizerThread";
		final String cl15 = "java.lang.ref.Reference$ReferenceHandler";
		final String name13 = "Signal Dispatcher";
		final String name15 = "Reference Handler";

		sb.append(format(THREAD_GROUP, "main"));
		sb.append(format(THREAD_STATUS, 0, CL, 0x45, "Thread-11", "running"));
		sb.append(format(THREAD_STATUS, 1, CL, 0x46, "Thread-10", "sleeping"));
		sb.append(format(THREAD_STATUS, 2, CL, 0x47, "Thread-9", "sleeping"));
		sb.append(format(THREAD_STATUS, 3, CL, 0x48, "Thread-8", "sleeping"));
		sb.append(format(THREAD_STATUS, 4, CL, 0x49, "Thread-7", "sleeping"));
		sb.append(format(THREAD_STATUS, 5, CL, 0x4a, "Thread-6", "sleeping"));
		sb.append(format(THREAD_STATUS, 6, CL, 0x4b, "Thread-5", "sleeping"));
		sb.append(format(THREAD_STATUS, 7, CL, 0x4c, "Thread-4", "sleeping"));
		sb.append(format(THREAD_STATUS, 8, CL, 0x4d, "Thread-3", "sleeping"));
		sb.append(format(THREAD_STATUS, 9, CL, 0x4e, "Thread-2", "sleeping"));
		sb.append(format(THREAD_STATUS, 10, CL, 0x4f, "Thread-1", "sleeping"));
		sb.append(format(THREAD_STATUS, 11, CL, 0x50, "Thread-0", "sleeping"));
		sb.append(format(THREAD_STATUS_BP, 12, cl12, 0x1, "main", "running"));
		sb.append(format(THREAD_GROUP, "system"));
		sb.append(format(THREAD_STATUS, 13, cl12, 0x2, name13, "running"));
		sb.append(format(THREAD_STATUS, 14, cl14, 0x3, "Finalizer", "waiting"));
		sb.append(format(THREAD_STATUS, 15, cl15, 0x4, name15, "waiting"));
	}
}
