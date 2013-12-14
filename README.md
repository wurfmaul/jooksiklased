jooksiklased - A Java program debugger
======================================

Project
-------
Jooksiklased is a debugger for Java programs written in Java. It uses the `com.sun.jdi` package family of the [Java™ Platform Debugger Architecture](http://docs.oracle.com/javase/7/docs/technotes/guides/jpda/index.html) (JPDA).


Functionality
-------------
The functionality is planned to be equivalent to Oracle's [JDB](http://download.java.net/jdk8/docs/technotes/tools/unix/jdb.html), but by now only the following commands are supported:

- **run**: start the virtual machine and execute the debuggee
- **cont**: continue executing after hitting a breakpoint or stepping
- **print**: print variables, fields or constants with its' values. Examples:
	- `print i` - prints type and value of local variable or field `i`
	- `print MyClass.i` - prints field `i` of class `MyClass`
- **dump**: same as print, except it prints content of complex types (arrays, objects)
- **where**: prints a snapshot of the method stack. I.e. all currently active methods
- **stop**: creates a breakpoint or prints all set breakpoints
	- `stop` - prints all available breakpoints
	
... to be continued!