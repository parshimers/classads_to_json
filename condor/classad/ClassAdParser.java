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

package condor.classad;

import java.util.*;
import java.io.*;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.InputSource;

/**
 * A factory for creating classad expressions (instances of {@link
 * condor.classad.Expr}), by parsing a textual or XML representation.
 * Input can come from a String, a Reader, or an InputStream.
 * @see ClassAd
 * @see Expr
 * @author <a href="mailto:solomon@cs.wisc.edu">Marvin Solomon</a>
 * @version 2.2
 */
public class ClassAdParser {
    private static String VERSION = "$Id: ClassAdParser.java,v 1.20 2005/05/06 20:54:06 solomon Exp $";

    // ========== public fields ===========

    /** Flag indicating that input format is "text".
     * @see #XML
     */
    public static final int TEXT = 0;

    /** Flag indicating that input format is xml.
     * @see #TEXT
     */
    public static final int XML = 1;

    /** Input format.
     * Either {@link #TEXT} or {@link #XML}.
     */
    public final int format;

    // ========== private fields ===========

    /** The underlying parser for text representation (only used if format is
     * TEXT).
     */
    private Parser parser;

    /** The underlying parser for xml representation (only used if format is
     * XML).
     */
    private SAXParser saxParser;

    /** Source of characaters for parsing (on used if format is XML). */
    private InputSource source;

    /** SAX event handler for parsing (only used if format is XML). */
    private ClassAdSAXHandler saxHandler;

    /** Verbosity of error messages.
     * The invariant is maintained that verbosity == parser.verbosity for TEXT
     * parsers and verbosity == saxHandler.verbosity for XML parsers.
     */
    private int verbosity = 3;

    /** Distination for error messages (if verbosity &gt; 0).
     * The invariant is maintained that errs == parser.errs for TEXT
     * parsers and errs == saxHandler.errs for XML parsers.
     */
    private PrintStream errs = System.err;

    // ========== constructors ===========

    /** Create a parser to parse text input.
     * An input source needs to be supplied by one of the reset methods before
     * it can be used for parsing.
     */
    public ClassAdParser() {
        this(TEXT);
    } // ClassAdParser()

    /** Create a parser to parse input.
     * An input source needs to be supplied by one of the reset methods before
     * it can be used for parsing.
     * @param format either {@link #TEXT} or {@link #XML}.
     */
    public ClassAdParser(int format) {
        this.format = format;
        switch (format) {
        case TEXT:
            break;
        case XML:
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(false); // TODO: allow caller to decide
            try {
                saxParser = factory.newSAXParser();
            } catch (ParserConfigurationException e) {
                throw new IllegalArgumentException(
                            "Cannot create parser: " + e);
            } catch (SAXException e) {
                throw new IllegalArgumentException(
                            "Cannot create parser: " + e);
            }
            saxHandler = new ClassAdSAXHandler();
            saxHandler.setVerbosity(verbosity, errs);
            break;
        default:
            throw new IllegalArgumentException("Invalid format " + format);
        }
    } // ClassAdParser()

    /** Create a parser to parse a string in default (text) format.
     * @param s the string to be parsed.
     */
    public ClassAdParser(String s) {
        this(TEXT);
        reset(s);
    } // ClassAdParser(String)

    /** Create a parser to parse a string.
     * @param s the string to be parsed.
     * @param format either {@link #TEXT} or {@link #XML}.
     */
    public ClassAdParser(String s, int format) {
        this(format);
        reset(s);
    } // ClassAdParser(String,int)

    /** Create a parser to parse an InputStream in default (text) format.
     * @param str the string to be parsed.
     */
    public ClassAdParser(InputStream str) {
        this(TEXT);
        reset(str);
    } // ClassAdParser(InputStream)

    /** Create a parser to parse an InputStream.
     * @param str the string to be parsed.
     * @param format either {@link #TEXT} or {@link #XML}.
     */
    public ClassAdParser(InputStream str, int format) {
        this(format);
        reset(str);
    } // ClassAdParser(String,int)

    /** Create a parser to parse characters in default (text) format.
     * @param rdr the string to be parsed.
     */
    public ClassAdParser(Reader rdr) {
        this(TEXT);
        reset(rdr);
    } // ClassAdParser(Reader)

    /** Create a parser to parse characters.
     * @param rdr the string to be parsed.
     * @param format either {@link #TEXT} or {@link #XML}.
     */
    public ClassAdParser(Reader rdr, int format) {
        this(format);
        reset(rdr);
    } // ClassAdParser(Reader,int)

    // ========== public methods ===========

    /** Reset this parser to parse input from a String.
     * @param s the string to parse.
     */
    public void reset(String s) {
        reset(new StringReader(s));
    } // reset(String)

    /** Reset this parser to parse input from an InputStream.
     * @param str the stream that supplies the input.
     */
    public void reset(InputStream str) {
        switch (format) {
        case TEXT:
            parser = new Parser(new InputStreamReader(str));
            parser.verbosity = verbosity;
            parser.errs = errs;
            break;
        case XML:
            source = new InputSource(str);
            break;
        }
    } // reset(InputStream)

    /** Reset this parser to parse input from a Reader.
     * @param rdr the reader that supplies the input.
     */
    public void reset(Reader rdr) {
        switch (format) {
        case TEXT:
            parser = new Parser(rdr);
            parser.verbosity = verbosity;
            parser.errs = errs;
            break;
        case XML:
            source = new InputSource(rdr);
            break;
        }
    } // reset(Reader)

    /** Parse a classsad Expr from the current input stream.
     * If "acceptPrefix" is true, the parser attempt to find a maximal
     * prefix of the input that is a valid Expr.  More precisely, it stops at
     * the first token than cannot be part of an Expr, and the stream is left
     * positioned so that the next token to be read is the first token (if any)
     * not part of the expression parsed.  For example, if the input stream
     * contains
     * <pre>
     *    a + b c
     * </pre>
     * The first call to parse() will return "a+b" and a subsequent call
     * will return "c".  Note that this "greedy" algorithm may fail to break
     * the input into a sequence of valid expressions if it is possible.
     * For example, the input
     * <pre>
     *   a - - b
     * </pre>
     * can be parsed as the two expressions <code>a</code> and
     * <code>--b</code>, but the first call will stop at the second minus sign
     * and fail, because <code>a -</code> is not a valid expression.
     * <p>
     * If "acceptPrefix" is false, the parse will fail unless all the remaining
     * input, up to end-of-stream, forms a valid expression.
     * <p>
     * If an error is detected, the return value is null and one or more error
     * messages will be printed to the current error stream, provided the
     * verbosity level is non-zero.  See {@link #setVerbosity(int)} and
     * {@link #setErrorStream(PrintStream)}.
     * <p>
     * If format is XML, the entire stream will be parsed as a single Expr
     * as if "acceptPrefix" were false.
     * In this case, subsequent calls will always return null.
     *
     * @param acceptPrefix if true, parse a maximal valid prefix of the input.
     * @return the parsed Expr or null on EOF or error.
     * @throws IllegalStateException if no input source has been specified by
     *         a constructor or a call to a reset method.
     * @see #nextToken()
     */
    public Expr parse(boolean acceptPrefix) {
        Expr result = null;
        switch (format) {
        case TEXT:
            if (parser == null) {
                throw new IllegalStateException(
                            "No input source has been specified");
            }
            result = (Expr)parser.parse(acceptPrefix);
            break;
        case XML:
            if (source == null) {
                throw new IllegalStateException("Input stream exhausted");
            }
            try {
                saxParser.parse(source, saxHandler);
                result = saxHandler.getResult();
                // A SAX parser only allows one bite at the apple
                source.getCharacterStream().close();
                source = null;
            } catch (Exception e) {
                if (verbosity > 1) {
                    e.printStackTrace(errs);
                } else if (verbosity > 0) {
                    errs.println(e.getMessage());
                }
            }
            break;
        }
        return result;
    } // parse(boolean)

    /** Parse a classsad Expr from the current input stream.
     * This method is equivalent to parse(true).
     * @see #parse(boolean)
     * @return the parsed Expr or null on EOF or error.
     * @throws IllegalStateException if no input source has been specified by
     *         a constructor or a call to a reset method.
     */
    public Expr parse() {
        return parse(true);
    } // parse()

    /** Get the "lookahead" token: the first token not yet consumed by the
     * parser.  The result is an internal code.  In the case of one-charcter
     * tokens, the code is the same as the integer value of the character.
     * The code 0 means end-of-file.
     * The token is not removed from the input stream.
     * For {@link #XML} parsers, always returns 0;
     * @return the code for the next token.
     * @see #getNextToken()
     */
    public int nextToken() {
        return format == TEXT ? parser.nextToken() : 0;
    } // nextToken()

    /** The the "value" of the lookahead token.
     * For {@link #XML} parsers, always returns null;
     * @see #nextToken()
     * @return the value
     */
    public Object nextValue() {
        return format == TEXT ? parser.nextValue() : null;
    } // nextValue()

    /** Get and remove the "lookahead" token: the first token not yet consumed
     * by the parser.  The result is an internal code.  In the case of
     * one-charcter tokens, the code is the same as the integer value of the
     * character.  The code 0 means end-of-file.
     * The token is removed from the input stream.
     * For {@link #XML} parsers, always returns 0;
     * @return the code for the next token.
     * @see #getNextToken()
     */
    public int getNextToken() {
        return format == TEXT ? parser.getNextToken() : 0;
    } // getNextToken()

    /** Returns the current input line.
     * For {@link #XML} parsers, the result is zero.
     * @return the current line number.
     */
    public int curLine() {
        return parser == null ? 0 : parser.curLine();
    } // curLine()

    /** Returns the current input column.
     * For {@link #XML} parsers, the result is zero.
     * @return the current line number.
     */
    public int curColumn() {
        return parser == null ? 0 : parser.curColumn();
    } // curColumn()

    /** Control whether the actions of the parser are traced.  The default is
     * not to trace.
     * For {@link #XML} parsers, has no effect.
     * @param on if true, turn on tracing; if false, turn it off.
     * @return the previous value of the tracing flag.
     */
    public boolean enableTracing(boolean on) {
        if (parser == null) {
            // XXX only happens with XML parsers.  For now, tracing is not
            // supported.
            return false;
        } else {
            return parser.enableTracing(on);
        }
    } // enableTracing(boolean)

    /** Set verbosity of error messages.
     * <dl>
     * <dt>0<dd>no messages
     * <dt>1<dd>only show message and line number
     * <dt>2<dd>also echo line in question ({@link #TEXT}) or document info
     *          ({@link #XML})
     * <dt>3<dd>also list tokens expected ({@link #TEXT} only)
     * </dl>
     * The default is maximally verbose.
     * @param level desired level.
     * @return the old verbosity level.
     * @throws IllegalArgumentException if level &lt; 0 or level &gt; 0 and
     *     {@link #setErrorStream(PrintStream)} has been called with a null
     *     argument.
     * @see #setErrorStream(PrintStream)
     */
    public int setVerbosity(int level) {
        if (verbosity < 0) {
            throw new IllegalArgumentException("bad verbosity level " + level);
        }
        if (verbosity > 0 && errs == null) {
            throw new IllegalArgumentException("parser has no error stream");
        }
        int result = verbosity;
        verbosity = level;
        if (parser != null) {
            parser.verbosity = level;
        }
        if (saxHandler != null) {
            saxHandler.setVerbosity(verbosity, errs);
        }
        return result;
    } // setVerbosity

    /** Set the destination for error messages.  By default, errors
     * go to System.err.
     * @param dest destination of error messages.  A null value is equivalent
     * to setVerbosity(0).
     * @return the previous error stream.
     * @see #setVerbosity(int)
     */
    public PrintStream setErrorStream(PrintStream dest) {
        PrintStream result = errs;
        if (dest == null) {
            setVerbosity(0);
        } else {
            errs = dest;
            if (parser != null) {
                parser.errs = dest;
            }
            if (saxHandler != null) {
                saxHandler.setVerbosity(verbosity, errs);
            }
        }
        return result;
    } // setErrorStream(PrintStream)

    /** Print a message, decorated with the current line and column number.
     * @param msg the message to print.
     */
    public void printMessage(String msg) {
        if (parser != null) {
            parser.printMessage(msg);
        }
        if (saxHandler != null) {
            saxHandler.printMessage(msg);
        }
    } // printMessage(String)

} // ClassAdParser
