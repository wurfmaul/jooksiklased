package at.jku.ssw.ssw.jooksiklased;

public enum Message {
	NO_METHOD("Unable to set deferred breakpoint %s.%s : No method %s in %s"), 
	DEFER_BREAKPOINT("Deferring breakpoint %s.%s.\nIt will be set after the class is loaded."), 
	DEFER_BREAKPOINT_LOC("Deferring breakpoint %s:%d.\nIt will be set after the class is loaded."), 
	HIT_BREAKPOINT("Breakpoint hit: \"thread=" + "%s\", %s, line=%d bci=%d"), 
	SET_BREAKPOINT("Set breakpoint in '%s' at line %d."), 
	VM_RUNNING("VM already running. Use 'cont' to continue after events."), 
	VM_NOT_RUNNING("Command 'cont' is not valid until the VM is started with the 'run' command"), 
	EXIT("The application exited."), UNKNOWN("Name unknown: %s"), 
	VAR("%s %s = %s"),
	FIELD("%s %s"),
	INVALID_CMD("Command not valid: '%s'"),
	VM_ERROR("%s"),
	USAGE("run / cont / print / dump / threads / thread / where / stop / clear / step / next / catch / ignore"),
	TOO_MANY_ARGS("No use for arguments: %s"),
	NO_FIELD("No static field or method with the name %s in %s"),
	LIST_BREAKPOINTS("Breakpoints set:\n%s");

	private final String msg;

	private Message(String msg) {
		this.msg = msg;
	}

	static void print(Message msg, Object... args) {
		System.out.printf(msg.msg + "\n", args);
	}

	static void error(Message msg, Object... args) {
		System.err.printf(msg.msg + "\n", args);
	}
	
	@Override
	public String toString() {
		return msg;
	}
}
