/** $Header: /p/condor/repository/CONDOR_SRC/src_java/condor/classad/tests/CATest.java,v 1.8 2005/05/06 20:53:31 solomon Exp $ */

/* **************************Copyright-DO-NOT-REMOVE-THIS-LINE**
 * Condor Copyright Notice
 *
 * See LICENSE.TXT for additional notices and disclaimers.
 *
 * Copyright (c)1990-2005 Condor Team, Computer Sciences Department,
 * University of Wisconsin-Madison, Madison, WI.  All Rights Reserved.
 * Use of the CONDOR Software Program Source Code is authorized
 * solely under the terms of the Condor Public License (see LICENSE.TXT).
 * For more information contact:
 * CONDOR Team, Attention: Professor Miron Livny,
 * 7367 Computer Sciences, 1210 W. Dayton St., Madison, WI 53706-1685,
 * (608) 262-0856 or miron@cs.wisc.edu.
 * ***************************Copyright-DO-NOT-REMOVE-THIS-LINE**/

package condor.classad.tests;
import condor.classad.*;
import java.io.*;
import java.util.*;

/** General purpose ClassAd tesing program.
 * Designed to be run either interactively or in batch mode.  Input consists of
 * a sequence of "commands".  Lines starting with '//' and empty lines are
 * ignored.  If input comes from a file, lines starting with whitespace are
 * continuation lines, and are appended to the previous non-comment, non-empty
 * line.  If input comes from stdin (which only happens if there are no
 * files mentioned on the command line), a continuation line is indicated by
 * a backslash at the end of the previous line.
 *
 * The return status is the number of test failures (0 if all tests succeed).
 *
 * In the following, VAR denotes a sequence of letters, digits, and underscores
 * starting with a letter.  After processing of comments and continuation
 * lines and before other processing, each occurrence of $VAR is replaced by
 * the current value of variable VAR (the null string if VAR has never been
 * assigned a value).  Note that there is no way to "escape" or "quote"
 * dollar signs in the input.
 *
 * Comands are as follows (case is significant).
 * <dl>
 * <dt> echo args       <dd> Prints args.
 * <dt> set name = value<dd> Sets an option.  See below.
 * <dt> show name       <dd> Displays an option value.
 * <dt> let VAR = EXPR  <dd> Sets VAR equal to the expression EXPR.
 * <dt> eval VAR = EXPR <dd> Sets VAR equal to the result of evaluating EXPR.
 * <dt> print EXPR      <dd> Displays EXPR.
 * <dt> same EXPR1 EXPR2<dd> Compares EXPR1 and EXPR2 and prints a message if
 *                           they differ.
 * <dt> diff EXPR1 EXPR2<dd> Compares EXPR1 and EXPR2 and prints a message if
 *                           they are the same.
 * <dt> quit            <dd> Immediately exit.
 * </dl>
 *
 * For the "same" and "diff" commands, comparison is "deep structural
 * equality":
 * <ul>
 * <li>Two constants match if they have the same value (for string constants,
 * the comparison is case-sensitive).
 * <li>Two record expressions match if they have the same set of
 * selectors and corresponding sub-expressions are (recursively) the same.
 * Matching of selectors (attribute names) is <em>not</em> case-sensitive.
 * <li>Any other two non-atomic expressions match if they have the same
 * operator and the same number of operands and corresponding operands match.
 * </ul>
 * 
 * The command "set name = value" sets the named option to the indicated value.
 * Leading and trailing space are stripped from the value.  Otherwise, the
 * name and value strings are processed verbatim.  Options are either boolean
 * or string valued.  For boolean options, the value must be "true" or "false".
 * Currently, the following options are supported.  An attempt to set or show
 * an unknown option is an error.  Option names and values are case-sensitive.
 *
 * <p><table border="1">
 * <tr><th> Option    <th> Type    <th> Default <th> Comments
 * <tr><td> xmlinput  <td> boolean <td> false   <td> EXPR inputs are in XML
 * <tr><td> format    <td> string  <td> compact <td> format for printing EXPRs
 * </table><p>
 *
 * Values for the the "format" options are
 * <dl>
 * <dt>compact<dd>Display each expression on one line in the "canonical native"
 *                representation.
 * <dt>xml    <dd>Display expressions in xml
 * <dt>pretty <dd>Display expressions in the "native" format using newlies
 *                and indentation to improve readability.
 * </dl>
 */
public class CATest {
    // Command-line options
    /** -v flag: amount of debugging output. */
    private int verbosity = 1;

    /** -t flag: enable tracing in the parser. */
    private boolean enableTracing = false;

    /** -e flag: echo each input line. */
    private boolean echo = false;

    // Options set by the "set" command.

    /** Flags for controling printing. */
    private int printFlags = 0;

    /** Ouput representation: ClassAdWriter.NATIVE or ClassAdWriter.XML. */
    private int printRep = ClassAdWriter.NATIVE;

    /** Input representation: ClassAdWriter.NATIVE or ClassAdWriter.XML. */
    private int inRep = ClassAdParser.TEXT;

    // Current state of the input

    /** Next physical line of input. */
    private String nextLine;

    /** Current command line, after processing comments and continuations. */
    private String curLine;

    /** Current position in curLine. */
    private int curPos;

    /** Line number in input file of the first line of the current command. */
    private int curLineNumber;

    /** Name of current input source. */
    private String curFname;

    /** True if input is comming from stdin. */
    private boolean interactive;

    /** True if interactive and we already got an EOF indication. */
    private boolean atEOF;

    // Misc globals

    /** Number of errors detected. */
    private int errorCount = 0;

    /** Current source of input */
    private LineNumberReader in;

    /** Place to display output. */
    private ClassAdWriter out;

    /** Parser for parsing expressions in the input. */
    private ClassAdParser parser = new ClassAdParser(inRep);

    /** Enumeration value to indicate which command. */
    private static final int
        DIFF = 1,
        ECHO = 2,
        EVAL = 3,
        HELP = 4,
        LET = 5,
        PRINT = 6,
        QUIT = 7,
		READXML = 8,
        SAME = 9,
        SET = 10,
        SHOW = 11,
		WRITEXML = 12;

    /** Command names. */
    private static final String commandNames[] = {
        null,
        "diff",
        "echo",
        "eval",
        "help",
        "let",
        "print",
        "quit",
		"readxml",
        "same",
        "set",
        "show",
		"writexml",
    };

    /** Help message. */
    private static final String[] HELP_TEXT = {
        "Commads are:",
        "  help                // print this message",
        "  echo args           // display args",
        "  let var = expr      // set var to expr",
        "  eval var = expr     // set var to result of evaluating expr",
        "  print expr          // print the expression",
        "  diff  expr1, expr2  // complain if expr1 and expr2 are the same",
        "  same  expr1, expr2  // complain if expr1 and expr2 are not the same",
        "  set option = value  // set the value of an option",
        "  show option         // show the current value of an option",
		"  writexml f list     // write the value of list to file f",
		"  readxml var f       // assign to var the list of ads in file f",
        "  quit                // exit immediately",
        "",
        "Each occurrence of $var is replaced by the value of var (or the null",
        "   string if var has never been assigned a value).  There is no way",
        "   to quote a dollar sign in the input.",
        "A continuation line is indicated by leading whitespace (batch mode),",
        "   or a trailing backslash on the previous line (interactive mode)"
    };

    /** Mapping of variables assigned by "let" or "eval" to Expr objects. */
    private Map vars = new HashMap();

    /** Exception to throw if an error is detected in the input. */
    private static class SyntaxError extends Exception { }

    /** Abbreviation for system.err.println.
     * @param s something to be printed (generally a String)
     */
    private static void pl(Object s) {
        System.err.println(s);
    }

    /** Main program.
     * Command-line options are
     * <dl>
     * <dt>-v<dd>Request more verbose debugging output (may be repeated).
     * <dt>-v<em>nn</em><dd>Set the debugging level to <em>nn</em>.
     * <dt>-t<dd>Trace the actions of the parser.
     * <dt>-l <em>class</em>Load <em>class</em> as a library.
     * </dl>
     * Other arguments are names of input files to process.  If none are
     * specified, use stdin.
     * @param args the command-line arguments.
     */
    public static void main(String[] args) {
        new CATest().run(args);
    } // main(String[])

    /** Constructor.  Does nothing. */
    private CATest() { }

    /** Print a usage message and exit. */
    private void usage() {
        pl("usage: java "
            + getClass().getName()
            + " [-v[verbosity]] [-q] [-e] [-t]"
            + " [-l library_class] [input_file ...]");
        System.exit(1);
    } // usage()

    /** The "main program" as a non-static method.
     * @param args the command-line arguments.
     */
    private void run(String[] args) {
        // Get command-line arguments
        GetOpt opts = new GetOpt(getClass().getName(), args, "v::qetl:");
    opt_loop:
        for (;;) switch(opts.nextOpt()) {
        case 'v': // More verbose
            if (opts.optarg == null) {
                verbosity++;
            } else {
                verbosity = Integer.parseInt(opts.optarg);
            }
            break;
        case 'q': // quiet
            verbosity = 0;
            break;
        case 't': // parser trace
            enableTracing = true;
            parser.enableTracing(enableTracing);
            break;
        case 'e': // echo input
            echo = true;
            break;
        case 'l':
            if (!ClassAd.loadJavaLibrary(opts.optarg)) {
                pl("Library " + opts.optarg + " cannot be loaded");
            }
            break;
        case -1:
            break opt_loop;
        default: usage();
        }


        out = new ClassAdWriter(System.out, ClassAdWriter.NATIVE, true);
        out.setFormatFlags(printFlags);

        if (opts.optind < args.length) {
            interactive = false;
            for (int i = opts.optind; i < args.length; i++) {
                Reader input = null;
                try {
                    curFname = args[i];
                    input = new FileReader(curFname);
                } catch (IOException e) {
                    System.err.println(curFname + ": " + e);
                    continue;
                }
                in = new LineNumberReader(input);
                processFile();
            }
        } else {
            interactive = true;
            curFname = "INPUT";
            in = new LineNumberReader(new InputStreamReader(System.in));
            processFile();
        }
        if (verbosity > 0) {
            out.println(errorCount
                + " error"
                + (errorCount == 1 ? "" : "s")
                + " detected");
        }
        System.exit(errorCount);
    } // run(String[])

    /** Main processing loop.  Process commands from reader "in".  */
    private void processFile() {
        if (!interactive) {
            try {
                nextLine = in.readLine();
                curLineNumber = in.getLineNumber();
            } catch (IOException e) {
                if (verbosity > 0) {
                    e.printStackTrace(out);
                }
                errorCount++;
                return;
            }
        }
        while (getLine()) {
            try {
                replaceVariables();
                Expr expr1, expr2;
                String var, value, fname;
                int curCommand = getCommand();
                switch (curCommand) {
                default:
                    if (interactive) {
                        error("Unknown command.  Type \"help\" for "
                            + "more information");
                    } else {
                        error("Unknown command \"" + curLine + "\"");
                    }
                case 0:
                    // empty line
                    break;
                case DIFF:
                case SAME:
                    expr1 = getExpr(true).eval();
                    expr2 = getExpr(false).eval();
                    if (
                           (curCommand == SAME && !expr1.sameAs(expr2))
                        || (curCommand == DIFF && expr1.sameAs(expr2)))
                    {
                        if (verbosity > 0) {
                            out.println("* " + curFname
                                + ", line: " + curLineNumber
                                + (curCommand == SAME
                                    ? " the expressions differ"
                                    : " the expressions are the same"));
                        }
                        errorCount++;

                        if (verbosity > 1) {
                            out.print("+<<");
                            out.print(expr1);
                            out.println("<<");
                            out.print("+>>");
                            out.print(expr2);
                            out.println(">>");
                        }
                    } else if (verbosity > 1) {
                        out.println("+ " + curFname
                            + ", line: " + in.getLineNumber()
                            + (curCommand == SAME
                                ? " the expressions are the same"
                                : " the expressions differ"));
                    }
                    break;
                case EVAL:
                    var = getVar(true);
                    expr1 = getExpr(false).eval();
                    vars.put(var, expr1);
                    break;
                case HELP:
                    if (verbosity > 0) {
                        for (int i = 0; i < HELP_TEXT.length; i++) {
                            out.println(HELP_TEXT[i]);
                        }
                    }
                    break;
                case LET:
                    var = getVar(true);
                    expr1 = getExpr(false);
                    vars.put(var, expr1);
                    break;
                case ECHO:
                    if (verbosity > 0) {
                        out.print("+ ");
                        out.println(curLine.substring(curPos));
                    }
                    break;
                case PRINT:
                    expr1 = getExpr(false);
                    if (verbosity > 0) {
                        out.print("+ ");
                        out.println(expr1);
                    }
                    break;
                case QUIT:
                    return;
                case SET:
                    if (curChar() == 0) {
                        optionsHelp();
                        break;
                    }
                    var = getVar(true);
                    value = getVar(false);
                    setOption(var, value);
                    break;
                case SHOW:
                    if (curChar() == 0) {
                        showOptions();
                        break;
                    }
                    var = getVar(false);
                    showOption("+ ", var);
                    break;
				case WRITEXML:
                    fname = getWord();
                    expr1 = getExpr(false);
					try {
						if (expr1.type != Expr.LIST) {
							if (verbosity > 0) {
								out.println(
									"* writexml: argument must be a list");
							}
							errorCount++;
							throw new SyntaxError();
						}
						ListExpr l = (ListExpr) expr1;
						ClassAdWriter xout
							= new ClassAdWriter(
										new FileWriter(fname),
										ClassAdWriter.XML,
										true);
						for (Iterator i = l.iterator(); i.hasNext(); ) {
							xout.println((Expr) (i.next()));
						}
						xout.close();
					} catch (IOException e) {
						if (verbosity > 0) {
							out.println("* " + e);
						}
						errorCount++;
					}
					break;
				case READXML:
					var = getVar(false);
                    fname = getWord();
					try {
						ClassAdParser p = new ClassAdParser(
												new FileReader(fname),
												ClassAdParser.XML);
						expr1 = p.parse();
						vars.put(var, expr1);
					} catch (IOException e) {
						if (verbosity > 0) {
							out.println("* " + e);
						}
						errorCount++;
					}
					break;
                }
            } catch (SyntaxError e) {
                // nothing
            }
        } // while
    } // processFile(Reader)

    /** Gets one logical line from the input stream and assign it to curLine.
     * Issues prompts (if interactive), strips comments (lines starting with
     * "//"), and handles continuation lines.  If "interactive" is false,
     * assumes that nextLine already has been set to the "lookahead" line.
     * @return false if no input remains (because of eof).
     */
    private boolean getLine() {
        curLine = null;
        try {
            StringBuffer sb;
            int firstLine = in.getLineNumber();
            if (interactive) {
                if (atEOF) {
                    return false;
                }
                // Prompt.  Continuation indicated by backslash at end of
                // previous line.   No lookahead.
                String line;

                // Get first line
                out.print("> ");
                out.flush();
                line = in.readLine();
                curLineNumber = in.getLineNumber();
                if (line == null) {
                    atEOF = true;
                    return false;
                }
                if (line.startsWith("//")) {
                    curLine = "";
                    return true;
                }

                sb = new StringBuffer(line);
                // get continuation lines, if any
                while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\\') {
                    sb.setCharAt(sb.length() - 1, '\n');
                    out.print("? ");
                    out.flush();
                    line = in.readLine();
                    curLineNumber = in.getLineNumber();
                    if (line == null) {
                        // NB: Silently ignore a backslash at the end of the
                        // last line before EOF
                        atEOF = true;
                        break;
                    }
                    if (line.startsWith("//")) {
                        continue;
                        // NB: This allows intermediate lines of a multi-line
                        // command to be commented out.
                    }
                    sb.append(line);
                }
            } // interactive
            else { // batch
                // No prompt.  Continuation indicated by whitespace at the
                // start of continuation line.  One-line lookahead in nextLine.
                if (nextLine == null) {
                    return false;
                }
                if (nextLine.startsWith("//")) {
                    nextLine = in.readLine();
                    curLineNumber = in.getLineNumber();
                    curLine = "";
                    return true;
                }
                sb = new StringBuffer(nextLine);
                for (;;) {
                    nextLine = in.readLine();
                    curLineNumber = in.getLineNumber();
                    if (nextLine == null) {
                        break;
                    }
                    if (nextLine.startsWith("//")) {
                        continue;
                        // NB: This allows intermediate lines of a multi-line
                        // command to be commented out.
                    }
                    if (nextLine.length() == 0
                            || !Character.isWhitespace(nextLine.charAt(0)))
                    {
                        // Non-comment, non-continuation line
                        break;
                    }
                    sb.append('\n').append(nextLine);
                }
            } // batch
            curLine =  sb.toString();
            curLineNumber = firstLine;
            if (echo) {
                out.println("> " + curLine);
            }
            return true;
        } catch (IOException e) {
            if (verbosity > 0) {
                e.printStackTrace(out);
            }
            errorCount++;
            return false;
        }
    } // getLine()

    /** Expands variable references in curLine.
     * The first substring of the form $xxx, where xxx is a variable name (a
     * letter followed by zero or more letters, digits, or underscores) is
     * replaced by the value of variable xxx, converted to a string, or the
     * null string if xxx has no value.  This replacement is repeated until
     * no dollar signs remain.  The value of curPos is set to 0.
     * @throws SyntaxError if a dollar sign not followed by a letter is found.
     */
    private void replaceVariables() throws SyntaxError {
        curPos = 0;
        for (;;) {
            int dollar = curLine.indexOf('$', curPos);
            if (dollar < 0) {
                return;
            }
            curPos = dollar + 1;
            if (!Character.isLetter(curChar())) {
                error("invalid variable name");
            }
            curPos++;
            while (Character.isLetterOrDigit(curChar()) || curChar() == '_') {
                curPos++;
            }
            String var = curLine.substring(dollar + 1, curPos);
            Object value = vars.get(var);
            if (value == null) {
                value = "";
            }
            curLine = curLine.substring(0, dollar)
                        + value
                        + curLine.substring(curPos);
        }
    } // replaceVariables()

    /** Checks the start of curLine for the name of a recognized command.
     * If found, advances curPos past the command and any following whitespace.
     * Returns 0 for an empty line and an invalid command code for an
     * unrecognized command.  Sets curPost to skip the command and following
     * whitespace.
     * @return a command code such as LET or PRINT.
     */
    private int getCommand() {
        if (curLine.length() == 0) {
            return 0; // empty line or comment
        }
        curPos = 0;
        while (Character.isLetter(curChar())) {
            curPos++;
        }
        int i;
        for (i = 1; i < commandNames.length; i++) {
            String cmd = commandNames[i];
            if (curLine.startsWith(cmd) && curPos == cmd.length()) {
                break;
            }
        }
        skipSpace();
        return i;
    } // getCommand()

    /** Gets a word from curLine starting at curPos.
	 * A "word" is any nonempty sequence of non-whitespace characters.
     * Sets curPos to skip any following whitespace.
     * @return the variable name.
     * @throws SyntaxError on syntax errors, after printing a
     * message.
     */
    private String getWord() throws SyntaxError {
        int start = curPos;
		while (curPos < curLine.length()
				&& !Character.isWhitespace(curLine.charAt(curPos)))
		{
			curPos++;
		}
        int end = curPos;
        skipSpace();
        return curLine.substring(start, end);
    } // getVar(boolean)

    /** Gets a variable name from curLine starting at curPos.
     * Sets curPos to skip any following whitespace. If "stripEquals" is
     * true also checks for an equals sign and skips it and any spaces
     * following.
     * @param stripEquals if true, look for and strip an equals sign.
     * @return the variable name.
     * @throws SyntaxError on syntax errors, after printing a
     * message.
     */
    private String getVar(boolean stripEquals) throws SyntaxError {
        int start = curPos;
        if (!Character.isLetter(curChar())) {
            error("invalid variable name");
        }
        curPos++;
        while (Character.isLetterOrDigit(curChar()) || curChar() == '_') {
            curPos++;
        }
        int end = curPos;
        skipSpace();
        if (stripEquals) {
            if (curChar() != '=') {
                error("missing equal sign");
            }
            curPos++;
            skipSpace();
        }
        return curLine.substring(start, end);
    } // getVar(boolean)

    /** Gets an expression from curLine starting at curPos.
     * Adjusts curPos to point just past the expression.
     * @param expectComma if true, expect the expression to be followed by a
     * comma, and skip over it before returning.
     */
    private Expr getExpr(boolean expectComma) throws SyntaxError {
        Expr res;
        parser.reset(curLine.substring(curPos));
        parser.enableTracing(enableTracing);
        res = parser.parse();
        if (res == null) {
            error("cannot parse expression");
        }
        curPos += parser.curColumn() - 1;
        skipSpace();
        if (expectComma) {
            if (curChar() != ',') {
                error("missing comma");
            }
            curPos++;
            skipSpace();
        }
        return res;
    } // getExpr(boolean)

    private void skipSpace() {
        while (curPos < curLine.length()
                    && Character.isWhitespace(curLine.charAt(curPos)))
        {
            curPos++;
        }
    } // skipSpace()

    private char curChar() {
        if (curPos >= curLine.length()) {
            return 0;
        }
        return curLine.charAt(curPos);
    } // curChar()

    /** Prints an error message, including the file and line where the error
     * occurred, and throws a SyntaxError.  
     * @param msg the error message.
     * @throws SyntaxError always.
     */
    private void error(String msg) throws SyntaxError {
        if (verbosity > 0) {
            out.println("* " + curFname
                + ", line " + curLineNumber + ": " + msg);
        }
        errorCount++;
        throw new SyntaxError();
    } // error(String)

    /** Sets the value of option "o" to "v".
     * @param o the name of the option.
     * @param v the new value.
     * @throws SyntaxError if "o" is unknown or "v" is not valid.
     */
    private void setOption(String o, String v) throws SyntaxError {
        if (o.equals("xmlinput")) {
            if (v.equals("true")) {
                inRep = ClassAdParser.XML;
            } else if (v.equals("false")) {
                inRep = ClassAdParser.TEXT;
            } else {
                error("Set xmlinput: argument must be \"true\" or \"false\"");
            }
            parser = new ClassAdParser(inRep);
            parser.enableTracing(enableTracing);
        } else if (o.equals("format")) {
            if (v.equals("compact")) {
                printRep = ClassAdWriter.NATIVE;
                printFlags = 0;
            } else if (v.equals("xml")) {
                printRep = ClassAdWriter.XML;
                printFlags = ClassAdWriter.MULTI_LINE_LISTS
                                | ClassAdWriter.MULTI_LINE_ADS;
            } else if (v.equals("pretty")) {
                printRep = ClassAdWriter.NATIVE;
                printFlags = ClassAdWriter.MULTI_LINE_ADS
                                  | ClassAdWriter.MULTI_LINE_LISTS
                                  | ClassAdWriter.MINIMAL_PARENTHESES
                                  | ClassAdWriter.SHOW_ERROR_DETAIL
                                  | ClassAdWriter.JAVA_REALS;
            } else {
                error("Set format: argument must be"
                        + " \"compact\", \"xml\", or \"pretty\"");
            }
            out.setRepresentation(printRep);
            out.setFormatFlags(printFlags);
        } else {
            error("Unknown option. Must be \"xmlinput\" or \"format\"");
        }
    } // setOption(String, String)

    /** Displays the current value of option "o" on the output.
     * @param prefix a prefix for each line of output.
     * @param o the name of the option to display.
     */
    private void showOption(String prefix, String o) throws SyntaxError {
        if (o.equals("xmlinput")) {
            if (verbosity > 0) {
                out.print(prefix);
                out.println(inRep == ClassAdParser.XML);
            }
        } else if (o.equals("format")) {
            if (printRep == ClassAdWriter.XML) {
                if (verbosity > 0) {
                    out.print(prefix);
                    out.println("xml");
                }
            } else if (printFlags == 0) {
                if (verbosity > 0) {
                    out.print(prefix);
                    out.println("compact");
                }
            } else {
                if (verbosity > 0) {
                    out.print(prefix);
                    out.println("pretty");
                }
            }
        } else {
            error("Unknown option. Must be \"xmlinput\" or \"format\"");
        }
    } // showOption(String)

    /** Displays the current values of all options on the output.
     */
    private void showOptions() throws SyntaxError {
        out.print("+ xmlinput: "); showOption("", "xmlinput");
        out.print("+ format: "); showOption("", "format");
    } // showOptions()

    /** Displays help about options. */
    private void optionsHelp() {
        out.println("+ usage:");
        out.println("+ set xmlinput { true | false }");
        out.println("+ set format { compact | pretty | xml }");
    } // optionsHelp()
        
} // class CATest
