package at.jku.ssw.ssw.jooksiklased;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * Implementation of abstract Debugger class. It provides special functionality
 * for automated testing.
 * 
 * @author wurfmaul <wurfmaul@posteo.at>
 * 
 */
public class TestDebugger extends Debugger {

	public TestDebugger(String... args) {
		super(args);
		out = new ByteArrayOutputStream();
	}

	/**
	 * Getter for super.out.
	 * 
	 * @return The output stream of the debugger.
	 */
	public OutputStream getOut() {
		return out;
	}

	@Override
	public void perform(String cmd) {
		super.perform(cmd);
	}
}
