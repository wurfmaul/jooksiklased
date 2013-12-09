package at.jku.ssw.ssw.jooksiklased;

public enum Message {
	NO_METHOD("Unable to set deferred breakpoint %s.%s : No method %s in %s"), DEFER_BREAKPOINT(
			"Deferring breakpoint %s.%s.\nIt will be set after the class is loaded."), HIT_BREAKPOINT(
			"Breakpoint hit: \"thread=" + "%s\", %s, line=%d bci=%d"), SET_BREAKPOINT(
			"Set breakpoint in '%s' at line %d."), VM_RUNNING(
			"VM already running. Use 'cont' to continue after events."), VM_NOT_RUNNING(
			"Command 'cont' is not valid until the VM is started with the 'run' command"), EXIT(
			"The application exited.");

	private String msg;

	Message(String msg) {
		this.msg = msg;
	}

	static void print(Message msg, Object... args) {
		System.out.println(String.format(msg.msg, args));
	}

	static void error(Message msg, Object... args) {
		System.err.println(String.format(msg.msg, args));
	}
}
