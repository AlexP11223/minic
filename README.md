# Mini-C compiler

[![Build Status](https://travis-ci.org/AlexP11223/minic.svg?branch=master)](https://travis-ci.org/AlexP11223/minic)

A simple compiler for a C-like programming language.

It has if-else statements, loops, variables (of int, double, bool or string types), arithmetic, comparison, logical operations, “functions” for user communications via stdin/stdout.

Implemented in Kotlin, using ANTLR for parsing and ASM library for JVM bytecode output.

# How to build

Requirements:
- JDK 8+.
- Maven 3+.

Run Maven **package** phase. This will download all dependencies, run JUnit tests and build JAR file + .exe and shell script. Check Maven output to see if all tests and build steps are completed successfully.

(Maven is included in popular Java IDEs such as IntelliJ Idea or Eclipse. You can run it either via your IDE Maven plugin or from command line in separate [Maven installation](https://maven.apache.org/install.html): `mvn package`.)

`dist/` folder will contain JAR file, .exe for Windows and shell script for Linux/MacOS (it simply executes the JAR file via `java`), as well as a sample source code file.
 
 Some of the tests launch `java`, using path from `System.getProperty("java.home")`. Fallbacks to `java` (from PATH environment variable) if it is not found.
 
# Usage
 
 1. `cd dist/minic-dist`
 2. Run `minic  <parameters>` (via shell script or Windows .exe) or `java -jar minic.jar <parameters>`.
 
If launched without parameters, it reads input from stdin until EOF (Ctrl+D, or Ctrl+Z for Windows), compiles and runs the program.
 
Also it is possible to specify input and output files: 

```
minic input_file [output_file]
```

**input_file** is path (or name) of file with Mini-C source code.

**output_file** is path (or name) of output file with JVM bytecode. Optional. If not specified, _input_file_ without extension will be used. **.class** extension is appended if not present (otherwise `java` will not run it), such as MyProgram.class.

Example:

```
minic MyProgram.mc
```
or
```
minic MyProgram.mc MyProgram
```
and
```
java MyProgram
```
