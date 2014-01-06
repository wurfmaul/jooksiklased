Jooksiklased - Java program debugger
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
- **locals**: list all visible local variables
- **fields**: list all visible fields of a given class
	- `fields MyClass` - lists all fields of class `MyClass`
- **where**: prints a snapshot of the method stack. I.e. all currently active methods
- **stop**: creates a breakpoint or prints all set breakpoints
	- `stop` - prints all available breakpoints
	- `stop in MyClass.myMethod` - creates a breakpoint at the first executable line in method `myMethod` of class `MyClass`
	- `stop at MyClass:42` - creates a breakpoint at line `42` of class `MyClass`
- **clear**: clears breakpoints or prints all set breakpoints
	- `clear` - see `stop`
	- `clear MyClass.myMethod` - removes breakpoint in `myMethod` of class `MyClass`
	- `clear MyClass:42` - removes breakpoint at line number `42` of class `MyClass`
- **step**: takes a step of one line of code in the current stack frame
- **next**: see `step`
- **threads**: list all currently active threads
- **thread**: switch to specific thread by id, which is provided by the `threads` command
	- `thread 1` - switch to thread with id `1`, must be performed after command `threads`

Naming
------
The name "Jooksiklased" is the Estonian word for "ground beetle".