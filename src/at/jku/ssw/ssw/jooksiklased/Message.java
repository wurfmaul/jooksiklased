package at.jku.ssw.ssw.jooksiklased;

/**
 * Central class for the formatting of messages concerning the debugger.
 * 
 * @author wurfmaul <wurfmaul@posteo.at>
 * 
 */
public enum Message {
	BREAKPOINT_NOT_FOUND("Not found: breakpoint %s"), 
	BREAKPOINT_ERROR("Unable to set breakpoint %s : No code at line %d in %s"),
	DEFER_BREAKPOINT("Deferring breakpoint %s.\nIt will be set after the class "
			+ "is loaded."), 
	EXIT("The application exited"),
	FIELDS_HEADER("** fields list **"),
	FIELD("%s %s"),
	FIELD_INHERITED("%s %s (inherited from %s)"),
	HIT_BREAKPOINT("Breakpoint hit: \"thread=" + "%s\", %s, line=%d bci=%d"),
	ILLEGAL_ARGUMENTS("Illegal arguments: '%s'."),
	INVALID_CMD("Command not valid: '%s'"), 
	LIST_BREAKPOINTS("Breakpoints set:\n%s"), 
	METHOD_OVERLOAD("Unable to set breakpoint %s: Method %s is overloaded"),
	NO_CLASS("No class specified."),
	NO_FIELD("No static field or method with the name %s in %s"), 
	NO_FIELDS("No fields in class %s"),
	NO_METHOD("Unable to set breakpoint %s : No method %s in %s"), 
	NO_LOCALS("No local variables."),
	NO_LOCAL_INFO("Local variable information not available."),
	NO_SUCH_THREAD("No thread with index %d."),
	REMOVE_BREAKPOINT("Removed: breakpoint %s"),
	RUN("run %s"),
	SET_BREAKPOINT("Set breakpoint in '%s' at line %d"), 
	STEP("Step completed: \"thread=%s\", %s, line=%d bci=%d"),
	THREAD_GROUP("Group %s:"),
	THREAD_STATUS("  %d (%s)0x%x\t\t%s\t\t%s"),
	THREAD_STATUS_BP("  %d (%s)0x%x\t\t%s\t\t%s (at breakpoint)"),
	TOO_MANY_ARGS("No use for arguments: %s"), 
	TRACE_LOC("\t[%d] %s.%s (%s:%d)"),
	TRACE_SRC("\t[%d] %s.%s (%s)"),
	TRACE("\t[%d] %s.%s"), 
	UNABLE_TO_ATTACH("Unable to attach due to the following error: '%s'."),
	UNABLE_TO_LAUNCH("Unable to launch due to the following error: '%s'."),
	UNABLE_TO_START("Unable to start due to the following error: '%s'."),
	UNKNOWN("Name unknown: %s"), 
	USAGE("run / cont / print / dump / threads / thread / where / stop / clear "
			+ "/ step / next / catch / ignore"), // TODO formulate usage info
	VAR("%s %s = %s"),
	VM_NOT_RUNNING("Command '%s' is not valid until the VM is started with "
			+ "the 'run' command"), 
	VM_RUNNING("VM already running. Use 'cont' to continue after events.");

	private final String msg;

	private Message(String msg) {
		this.msg = msg;
	}
	
	public static String format(Message msg, Object...args) {
		return String.format(msg + "\n", args);
	}

	@Override
	public String toString() {
		return msg;
	}
}
