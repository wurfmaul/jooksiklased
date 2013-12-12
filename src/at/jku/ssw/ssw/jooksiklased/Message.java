package at.jku.ssw.ssw.jooksiklased;

public enum Message {
	BREAKPOINT_NOT_FOUND("Not found: breakpoint %s"),
	DEFER_BREAKPOINT("Deferring breakpoint %s.\nIt will be set after the class is loaded."),
	EXIT("The application exited."), 
	FIELD("%s %s"),
	HIT_BREAKPOINT("Breakpoint hit: \"thread=" + "%s\", %s, line=%d bci=%d"), 
	INVALID_CMD("Command not valid: '%s'"),
	LIST_BREAKPOINTS("Breakpoints set:\n%s"),
	NO_FIELD("No static field or method with the name %s in %s"),
	NO_METHOD("Unable to set deferred breakpoint %s.%s : No method %s in %s"), 
	REMOVE_BREAKPOINT("Removed: breakpoint %s"),
	SET_BREAKPOINT("Set breakpoint in '%s' at line %d."), 
	STEP("Step completed: \"thread=%s\", %s, line=%d bci=%d"),
	TOO_MANY_ARGS("No use for arguments: %s"),
	TRACE("%s"),
	UNKNOWN("Name unknown: %s"), 
	USAGE("run / cont / print / dump / threads / thread / where / stop / clear / step / next / catch / ignore"),
	VAR("%s %s = %s"),
	VM_ERROR("%s"),
	VM_NOT_RUNNING("Command 'cont' is not valid until the VM is started with the 'run' command"), 
	VM_RUNNING("VM already running. Use 'cont' to continue after events."); 

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
