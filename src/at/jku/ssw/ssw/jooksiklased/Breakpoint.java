package at.jku.ssw.ssw.jooksiklased;

/**
 * Represents a breakpoint which has not yet been worked with. The structure
 * simplifies dealing with breakpoint prototypes (e.g. setting or removing
 * breakpoints before class is loaded). Consider an object as this class as
 * dummy breakpoint.
 * 
 * @author wurfmaul <wurfmaul@posteo.at>
 * 
 */
class Breakpoint {
	final String className;
	final String methodName;
	final int lineNumber;

	Breakpoint(String className, String methodName) {
		this.className = className;
		this.methodName = methodName;
		this.lineNumber = -1;
	}

	Breakpoint(String className, int lineNumber) {
		this.className = className;
		this.methodName = null;
		this.lineNumber = lineNumber;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(className);
		if (methodName != null) {
			sb.append(".");
			sb.append(methodName);
		} else {
			sb.append(":");
			sb.append(lineNumber);
		}
		return sb.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Breakpoint))
			return false;

		final Breakpoint other = (Breakpoint) obj;
		return className.equals(other.className)
				&& methodName.equals(other.methodName)
				&& lineNumber == other.lineNumber;
	}
}