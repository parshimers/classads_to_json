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

import java.io.*;
import java.util.Date;
import java.util.Iterator;
import java.util.SimpleTimeZone;
import java.text.DateFormat;

/** A tool for converting classads to characters in a variety of formats.
 * A ClassAdWriter behaves like a java.io.PrintWriter except that it has two
 * additional methods <code>print(Expr)</code> and <code>println(Expr)</code>.
 * An instance has an associated <em>representation</em> and set of
 * <em>options</em>.
 * <p>
 * Currently, we support two representations:
 * <dl>
 * <dt>NATIVE<dd>The "native" ASCII representation, e.g.
 * <pre>
 *    [ my_string = "abc"; my_list = {1, 2, x + 1};]
 * </pre>
 *
 * <dt>XML<dd>An XML (Extensible Markup Language) representation, e.g.
 * <pre>
 *     &lt;c&gt;
 *         &lt;a n="my_string"&gt;&lt;s&gt;abc&lt;/s&gt;&lt;/a&gt;
 *         &lt;a n="my_list"&gt;&lt;l&gt; &lt;n&gt;1&lt;/n&gt; &lt;n&gt;2&lt;/n&gt; &lt;e&gt;x+1&lt;/e&gt; &lt;/l&gt;&lt;/a&gt;
 *     &lt;/c&gt;
 * </pre>
 * </dl>
 *
 * The third representation is "OLD" classads, supported only for compatibility
 * with a previous version C++ implementation of classified advertisements.
 * Only RecordExprs can be represented in this format, and the format is
 * inherently binary, requiring a DataOutput.  To represent an ad in the OLD
 * format, use {@link condor.classad.RecordExpr#transmit(java.io.DataOutput)}.
 * <p>
 * Other external representations may be added in the future.
 * <p>
 * Options provide finer control over the output format, for example, the
 * native representation may be formatted for readability (e.g. listing each
 * attribute of a RecordExpression on a separate line) or for compactness.
 * By default, a ClassAdWriter uses NATIVE representation and formats the same
 * as <code>Expr.toString()</code> so that code such as
 * <pre>
 *     Expr e = ...;
 *     ClassAdWriter out = new ClassAdWriter(System.out);
 *     out.println(e)
 * </pre>
 * has the same effect as
 * <pre>
 *     Expr e = ...;
 *     System.out.println(e);
 * </pre>
 * However, a ClassAdWriter allows more control over representation and
 * formatting details.  It also may be more efficient for large expressions,
 * since the string representation doesn't need to be assembled in memory
 * before being printed.
 * @author <a href="mailto:solomon@cs.wisc.edu">Marvin Solomon</a>
 * @version 2.2
 */
public class ClassAdWriter extends java.io.PrintWriter {
    private static String VERSION = "$Id: ClassAdWriter.java,v 1.15 2005/05/06 20:54:07 solomon Exp $";

    // Internal state ------------------------------------------------------

    /** The representation to be used for output. */
    private int rep;

    /** Indicates that a &lt;classads&gt; was output by the constructor and
     * should be balanced by a &lt;/classads&gt; tag on close.
     */
    private boolean needTrailer;

    /** Flags to control the formatting of expressions. A value of zero creates
     * a representation that is unambiguous and easy to parse but not very
     * readable.  In the case of the NATIVE representation, this is the
     * "canonical" format.  Each bit turns on a formatting feature that makes
     * the string more readable, but perhaps more verbose and/or ambiguous.
     * @see #NO_ESCAPE_STRINGS
     * @see #SHOW_ERROR_DETAIL
     * @see #MINIMAL_PARENTHESES
     * @see #JAVA_REALS
     * @see #MULTI_LINE_ADS
     * @see #MULTI_LINE_LISTS
     * @see #BRIEF
     * @see #COMPACT
     * @see #READABLE
     */
    private int formatFlags = 0;

    /** The current indentation level, for "pretty printing". */
    private int indentLevel = 0;

    // Constructors ----------------------------------------------------------

    /** Creates a new ClassAdWriter for writing to an existing OutputStream
     * in a given representation.
     * Characters are converted to bytes using the default character encoding.
     * @param out the desitination for output.
     * @param representation the representation (one of NATIVE or XML).
     * @param autoFlush if true, the println() methods will flush the output
     * buffer.
     */
    public ClassAdWriter(OutputStream out,
                         int representation,
                         boolean autoFlush)
    {
        super(out, autoFlush);
        rep = representation;
        if (rep == XML) {
            println("<?xml version=\"1.0\"?>");
            println("<!DOCTYPE classads>");
            println("<classads>");
            needTrailer = true;
        }
    } // ClassAdWriter(OutputStream,int,boolean)

    /** Creates a new ClassAdWriter for writing to an existing OutputStream
     * in a given representation, without automatic line flushing.
     * Characters are converted to bytes using the default character encoding.
     * @param out the desitination for output.
     * @param representation the representation (one of NATIVE or XML).
     */
    public ClassAdWriter(OutputStream out, int representation) {
        this(out, representation, false);
    } // ClassAdWriter(OutputStream,int)

    /** Creates a new ClassAdWriter for writing to an existing OutputStream
     * using the NATIVE representation.
     * Characters are converted to bytes using the default character encoding.
     * @param out the desitination for output.
     * @param autoFlush if true, the println() methods will flush the output
     * buffer.
     */
    public ClassAdWriter(OutputStream out, boolean autoFlush) {
        super(out, autoFlush);
        rep = NATIVE;
    } // ClassAdWriter(OutputStream,boolean)

    /** Creates a new ClassAdWriter for writing to an existing OutputStream
     * using the NATIVE representation, without automatic line flushing.
     * Characters are converted to bytes using the default character encoding.
     * @param out the desitination for output.
     */
    public ClassAdWriter(OutputStream out) {
        super(out);
        rep = NATIVE;
    } // ClassAdWriter(OutputStream)

    /** Creates a new ClassAdWriter for writing to an existing Writer
     * in a given representation.
     * Characters are converted to bytes using the default character encoding.
     * @param out the desitination for output.
     * @param representation the representation (one of NATIVE or XML).
     * @param autoFlush if true, the println() methods will flush the output
     * buffer.
     */
    public ClassAdWriter(Writer out,
                         int representation,
                         boolean autoFlush)
    {
        super(out, autoFlush);
        rep = representation;
        if (rep == XML) {
            println("<?xml version=\"1.0\"?>");
            println("<!DOCTYPE classads>");
            println("<classads>");
            needTrailer = true;
        }
    } // ClassAdWriter(Writer,int,boolean)

    /** Creates a new ClassAdWriter for writing to an existing Writer
     * in a given representation, without automatic line flushing.
     * Characters are converted to bytes using the default character encoding.
     * @param out the desitination for output.
     * @param representation the representation (one of NATIVE or XML).
     */
    public ClassAdWriter(Writer out, int representation) {
        this(out, representation, false);
    } // ClassAdWriter(Writer,int)

    /** Creates a new ClassAdWriter for writing to an existing Writer
     * using the NATIVE representation.
     * Characters are converted to bytes using the default character encoding.
     * @param out the desitination for output.
     * @param autoFlush if true, the println() methods will flush the output
     * buffer.
     */
    public ClassAdWriter(Writer out, boolean autoFlush) {
        super(out, autoFlush);
        rep = NATIVE;
    } // ClassAdWriter(Writer,boolean)

    /** Creates a new ClassAdWriter for writing to an existing Writer
     * using the NATIVE representation, without automatic line flushing.
     * Characters are converted to bytes using the default character encoding.
     * @param out the desitination for output.
     */
    public ClassAdWriter(Writer out) {
        super(out);
        rep = NATIVE;
    } // ClassAdWriter(Writer)

    // Public constants ------------------------------------------------------

    /** Native output representation (e.g. <code>[a=x+1]</code>). */
    public static final int NATIVE = 1;

    /** XML output representation
     * (e.g.  <code>&lt;c&gt;&lt;a name="a"&gt;&lt;e&gt;x+1&lt;/e&gt;&lt;/a&gt;&lt;/c&gt; </code>).
     */
    public static final int XML = 2;

    /** An option flag for <code>formatFlags</code>.
     * Don't surround strings with quotes or escape special characters in the
     * string.
     * <p><strong>WARNING</strong>: Strings created with this flag cannot be
     * parsed to recover the expressions that created them.
     * @see #setFormatFlags
     */
    static public final int NO_ESCAPE_STRINGS = 1<<0;

    /** An option flag for <code>formatFlags</code>.
     * Print the ERROR value as "ERROR(reason)" rather than just "ERROR",
     * and similarly for UNDEFINED.
     * @see #setFormatFlags
     */
    static public final int SHOW_ERROR_DETAIL = 1<<1;

    /** An option flag for <code>formatFlags</code>.
     * Use only as many parentheses are are required to override the default
     * precendence of operators.
     * @see #setFormatFlags
     */
    static public final int MINIMAL_PARENTHESES = 1<<2;

    /** An option flag for <code>formatFlags</code>.
     * Display classads with one attribute per line.
     * @see #setFormatFlags
     */
    static public final int MULTI_LINE_ADS = 1<<3;

    /** An option flag for <code>formatFlags</code>.
     * Display lists with one member per line.
     * @see #setFormatFlags
     */
    static public final int MULTI_LINE_LISTS = 1<<4;

    /** An option flag for <code>formatFlags</code>.
     * Use the default Java representation for reals rather than always
     * using exponential notation.  For example, print 12345 rather than
     * 1.2345E4.
     * @see #setFormatFlags
     */
    static public final int JAVA_REALS = 1<<5;

    /** A combination of option flags for <code>formatFlags</code>.
     * Produce a concise version: MINIMAL_PARENTHESES,
     * NO_ESCAPE_STRINGS, and JAVA_REALS.
     * @see #setFormatFlags
     * @see #MINIMAL_PARENTHESES
     * @see #NO_ESCAPE_STRINGS
     * @see #JAVA_REALS
     */
    static public final int BRIEF
        = MINIMAL_PARENTHESES | NO_ESCAPE_STRINGS | JAVA_REALS;

    /** A combination of option flags for <code>formatFlags</code>.
     * Produce a compact but parsable version: MINIMAL_PARENTHESES and
     * JAVA_REALS.
     * @see #setFormatFlags
     * @see #MINIMAL_PARENTHESES
     * @see #JAVA_REALS
     */
    static public final int COMPACT = MINIMAL_PARENTHESES | JAVA_REALS;

    /** A combination of option flags for <code>formatFlags</code>.
     * Produce a maximally-readable version (all flags set).
     * @see #setFormatFlags
     */
    static public final int READABLE =
                                NO_ESCAPE_STRINGS
                                | SHOW_ERROR_DETAIL
                                | MINIMAL_PARENTHESES
                                | JAVA_REALS
                                | MULTI_LINE_ADS
                                | MULTI_LINE_LISTS;

    // Public methods ------------------------------------------------------

    /** Change the output representation.
     * <b>Note</b> If the initial representation set by the constructor was
     * XML, the output is "wrapped" by
     * <pre>
     *     &lt;?xml version=\"1.0\"?&gt;
     *     &lt;!DOCTYPE classads&gt;
     *     &lt;classads&gt;
     *     ...
     *     &lt;/classads&gt;
     * </pre>
     * and the final <tt>&lt;/classads&gt;</tt> is output by the
     * {@link #close()} method regardless of the current representation.
     * @param representation the new representation: one of XML or NATIVE.
     * @see #setFormatFlags(int)
     * @see #getRepresentation
     */
    public void setRepresentation(int representation) {
        switch (representation) {
        case XML:
        case NATIVE:
            rep = representation;
            break;
        default:
            throw new IllegalArgumentException(
                "Invalid representation code " + representation);
        }
    } // setRepresentation(int)

    /** Get the current output representation.
     * @return the current output representation, either XML or NATIVE.
     * @see #setRepresentation
     */
    public int getRepresentation() {
        return rep;
    } // getRepresentation()

    /** Set options for converting expressions to strings.
     * @param flags the new flag values.
     * @see #NO_ESCAPE_STRINGS
     * @see #SHOW_ERROR_DETAIL
     * @see #MINIMAL_PARENTHESES
     * @see #JAVA_REALS
     * @see #MULTI_LINE_ADS
     * @see #MULTI_LINE_LISTS
     * @see #BRIEF
     * @see #COMPACT
     * @see #READABLE
     * @see #getFormatFlags
     */
    public void setFormatFlags(int flags) {
        formatFlags = flags;
    } // setFormatFlags(int)

    /** Get existing options for converting expressions to strings.
     * @return the current option flags.
     * @see #NO_ESCAPE_STRINGS
     * @see #SHOW_ERROR_DETAIL
     * @see #MINIMAL_PARENTHESES
     * @see #JAVA_REALS
     * @see #MULTI_LINE_ADS
     * @see #MULTI_LINE_LISTS
     * @see #BRIEF
     * @see #COMPACT
     * @see #READABLE
     * @see #setFormatFlags
     */
    public int getFormatFlags() {
        return formatFlags;
    } // getFormatFlags()

    /** Turn on options for converting expressions to strings.
     * @param flags the flag values to be enabled
     * @see #NO_ESCAPE_STRINGS
     * @see #SHOW_ERROR_DETAIL
     * @see #MINIMAL_PARENTHESES
     * @see #JAVA_REALS
     * @see #MULTI_LINE_ADS
     * @see #MULTI_LINE_LISTS
     * @see #BRIEF
     * @see #COMPACT
     * @see #READABLE
     */
    public void enableFormatFlags(int flags) {
        formatFlags |= flags;
    } // enableFormatFlags(int)

    /** Turn off options for converting expressions to strings.
     * @param flags the flag values to be disabled
     * @see #NO_ESCAPE_STRINGS
     * @see #SHOW_ERROR_DETAIL
     * @see #MINIMAL_PARENTHESES
     * @see #JAVA_REALS
     * @see #MULTI_LINE_ADS
     * @see #MULTI_LINE_LISTS
     * @see #BRIEF
     * @see #COMPACT
     * @see #READABLE
     */
    public void disableFormatFlags(int flags) {
        formatFlags &= ~flags;
    } // disableFormatFlags(int)

    /** Print a classad expression and then terminate the line. This method
     * behaves as though it invokes print(Expr) and then println().
     * @param exp the expression to be printed.
     */
    public void println(Expr exp) {
        print(exp);
        println();
    } // println(Expr)

    /** Print a classad expression.
     * @param exp the expression to be printed.
     */
    public void print(Expr exp) {
        switch (rep) {
        case NATIVE: printNative(exp, false); return;
        case XML: printXML(exp); return;
        default:
            throw new RuntimeException(
                "Internal error, unknown representation " + rep);
        }
    } // print(Expr)

    /** Close this writer. */
    public void close() {
        if (needTrailer) {
            println("</classads>");
        }
        super.close();
    } // close()

    // Private methods -----------------------------------------------------

    /** Indent output by the current indentation level. */
    private void indent() {
        for (int i = 0; i < indentLevel; i++) {
            print("    ");
        }
    } // indent

    /** Print a classad expression in NATIVE format.
     * @param exp the expression to be printed.
     * @param escapeXML if true, the characters &lt; etc. in the result are
     *     replaced by XML character entities &amp;lt;, etc. because we are
     *     inside of an "&lt;e&gt;" element in XML output.
     */
    private void printNative(Expr exp, boolean escapeXML) {
        if (exp == null) {
            super.print(exp);
            return;
        }
        Constant c = exp instanceof Constant ? (Constant) exp : null;
        switch (exp.type) {

        // Internal nodes in tree

        case Expr.CALL: {
            FuncCall e = (FuncCall) exp;
            String comma = "";
            print(e.func);
            print('(');
            for (Iterator i = e.args.iterator(); i.hasNext(); ) {
                print(comma);
                printNative((Expr) i.next(), escapeXML);
                comma = ",";
            }
            print(')');
            break;
        }
        case Expr.COND: {
            CondExpr e = (CondExpr) exp;
            if ((formatFlags & MINIMAL_PARENTHESES ) == 0) {
            } else {
                if (e.ec.prec() <= e.prec()) {
                    print('(');
                    printNative(e.ec, escapeXML);
                    print(')');
                } else {
                    printNative(e.ec, escapeXML);
                }
                print('?');
                printNative(e.et, escapeXML);
                print(':');
                printNative(e.ef, escapeXML);
            }
            break;
        }
        case Expr.LIST: {
            ListExpr e = (ListExpr) exp;
            if ((formatFlags & MULTI_LINE_LISTS ) == 0) {
                print('{');
                for (int i = 0; i < e.size(); i++) {
                    if (i > 0) {
                        print(',');
                    }
                    printNative(e.sub(i), escapeXML);
                }
                print('}');
            } else {
                println('{');
                indentLevel++;
                indent();
                for (int i = 0; i < e.size(); i++) {
                    if (i > 0) {
                        println(',');
                        indent();
                    }
                    printNative(e.sub(i), escapeXML);
                }
                println();
                indentLevel--;
                indent();
                print('}');
            }
            break;
        }
        case Expr.OP: {
            Op e = (Op) exp;
            String op = escapeXML ? Expr.opNameXML[e.op] : Expr.opName[e.op];
            if ((formatFlags & MINIMAL_PARENTHESES ) == 0) {
                // Fully parenthesize
                print('(');
                if (e.arg2 == null) {
                    // unary operator
                    print(op);
                }
                // left (or only) operand
                printNative(e.arg1, escapeXML);
                if (e.arg2 != null) {
                    // binary operator
                    print(op);

                    // right operand
                    printNative(e.arg2, escapeXML);
                }
                print(')');
            } else {
                // Only use as many parentheses as necessary
                if (e.arg2 == null) {
                    // unary operator
                    print(op);
                }

                // left (or only) operand
                if (e.arg1.prec() < e.prec()) {
                    print('(');
                    printNative(e.arg1, escapeXML);
                    print(')');
                } else {
                    printNative(e.arg1, escapeXML);
                }

                if (e.arg2 != null) {
                    // binary operator
                    print(op);

                    // right operand
                    if (e.arg2.prec() <= e.prec()) {
                        print('(');
                        printNative(e.arg2, escapeXML);
                        print(')');
                    } else {
                        printNative(e.arg2, escapeXML);
                    }
                }
            } // minimal parentheses
            break;
        }
        case Expr.RECORD: {
            RecordExpr e = (RecordExpr) exp;
            boolean first = true;

            if ((formatFlags & MULTI_LINE_ADS ) == 0) {
                print('[');
                for (Iterator i = e.attributes(); i.hasNext(); ) {
                    if (first) {
                        first = false;
                    } else {
                        print(';');
                    }
                    AttrName attr = (AttrName) i.next();
                    printNativeAttrName(attr, escapeXML);
                    print("=");
                    printNative(e.lookup(attr), escapeXML);
                }
                print(']');
            } else {
                println('[');
                indentLevel++;
                indent();
                for (Iterator i = e.attributes(); i.hasNext(); ) {
                    if (first) {
                        first = false;
                    } else {
                        println(';');
                        indent();
                    }
                    AttrName attr = (AttrName) i.next();
                    printNativeAttrName(attr, escapeXML);
                    print(" = ");
                    printNative(e.lookup(attr), escapeXML);
                }
                println();
                indentLevel--;
                indent();
                print(']');
            }
            break;
        }
        case Expr.SELECTION: {
            SelectExpr e = (SelectExpr) exp;
            printNative(e.base, escapeXML);
            print('.');
            print(e.selector);
            break;
        }
        case Expr.SUBSCRIPT: {
            SubscriptExpr e = (SubscriptExpr) exp;
            printNative(e.base, escapeXML);
            print('[');
            printNative(e.selector, escapeXML);
            print(']');
            break;
        }

        // Leaf nodes (constants)

        case Expr.ATTRIBUTE:
            printNativeAttrName(((AttrRef) exp).name, escapeXML);
            break;
        case Expr.ABSOLUTE_TIME:
            print("absTime(\"");
            print(c.value);
            print("\")");
            break;
        case Expr.RELATIVE_TIME:
            print("relTime(\"");
            print(Constant.relTimeToString(c.milliseconds()));
            print("\")");
            break;
        case Expr.BOOLEAN:
            print(c.isTrue());
            break;
        case Expr.ERROR:
            if ((formatFlags & SHOW_ERROR_DETAIL) == 0) {
                print("ERROR");
            } else {
                print("ERROR(");
                print(c.value);
                print(')');
            }
            break;
        case Expr.UNDEFINED:
            if ((formatFlags & SHOW_ERROR_DETAIL) == 0) {
                print("UNDEFINED");
            } else {
                print("UNDEFINED(");
                print(c.value);
                print(')');
            }
            break;
        case Expr.INTEGER:
            print(c.value);
            break;
        case Expr.REAL:
            print(
                (formatFlags & JAVA_REALS) == 0
                    ? Constant.doubleToString(c.realValue())
                    : c.value);
            break;
        case Expr.STRING:
            if ((formatFlags & NO_ESCAPE_STRINGS) == 0) {
                print('"');
                printQuotedString((String) c.value, '"', escapeXML);
                print('"');
            } else {
                print(c.value);
            }
            break;
        } // switch (exp.type)
    } // printNative(Expr)

    /** Helper method for printNative.  Prints an attribute name as name or
     * 'backslash_escape(name)', as appropriate.
     * @param attr the attribute to be printed.
     * @param escapeXML if true, the characters &lt; etc. in the result are
     *                  replaced by XML character entities &amp;lt;, etc.
     */
    private void printNativeAttrName(AttrName attr, boolean escapeXML) {
        if (attr.needsQuoting()) {
            print("'");
            printQuotedString(attr.rawString(), '\'', escapeXML);
            print("'");
        } else {
            printQuotedString(attr.rawString(), (char) 0, escapeXML);
        }
    } // printNativeAttrName(AttrName, boolean)

    /** Print a classad expression in XML format.
     * @param exp the expression to be printed.
     */
    private void printXML(Expr exp) {
        if (exp == null) {
            super.print(exp);
            return;
        }
        Constant c = exp instanceof Constant ? (Constant) exp : null;
        switch (exp.type) {
        case Expr.LIST: {
            ListExpr e = (ListExpr) exp;
            if ((formatFlags & MULTI_LINE_LISTS ) == 0) {
                print("<l>");
                for (int i = 0; i < e.size(); i++) {
                    printXML(e.sub(i));
                }
                print("</l>");
            } else {
                println("<l>");
                indentLevel++;
                for (int i = 0; i < e.size(); i++) {
                    indent();
                    printXML(e.sub(i));
                    println();
                }
                indentLevel--;
                indent();
                print("</l>");
            }
            break;
        }
        case Expr.RECORD: {
            RecordExpr e = (RecordExpr) exp;
            boolean first = true;

            if ((formatFlags & MULTI_LINE_ADS ) == 0) {
                print("<c>");
                for (Iterator i = e.attributes(); i.hasNext(); ) {
                    AttrName attr = (AttrName) i.next();
                    print("<a n=\"");
                    printQuotedString(attr.rawString(), 'q', true);
                    print("\">");
                    printXML(e.lookup(attr));
                    print("</a>");
                }
                print("</c>");
            } else {
                println("<c>");
                indentLevel++;
                for (Iterator i = e.attributes(); i.hasNext(); ) {
                    indent();
                    AttrName attr = (AttrName) i.next();
                    print("<a n=\"");
                    printQuotedString(attr.rawString(), 'q', true);
                    print("\">");
                    printXML(e.lookup(attr));
                    println("</a>");
                }
                indentLevel--;
                print("</c>");
            }
            break;
        }
        case Expr.ABSOLUTE_TIME:
            print("<at>");
            print(c.value);
            print("</at>");
            break;
        case Expr.RELATIVE_TIME:
            print("<rt>");
            print(Constant.relTimeToString(c.milliseconds()));
            print("</rt>");
            break;
        case Expr.BOOLEAN:
            print(c.isTrue()
                        ? "<b v=\"t\"/>"
                        : "<b v=\"f\"/>");
            break;
        case Expr.ERROR: {
            String a = c.annotation();
            if (a.length() > 0) {
                print("<er a=\"");
                printQuotedString(a, 'q', true);
                print("\"/>");
            } else {
                print("<er/>");
            }
            break;
        }
        case Expr.UNDEFINED: {
            String a = c.annotation();
            if (a.length() > 0) {
                print("<un a=\"");
                printQuotedString(a, 'q', true);
                print("\"/>");
            } else {
                print("<un/>");
            }
            break;
        }
        case Expr.INTEGER:
            print("<i>");
            print(c.value);
            print("</i>");
            break;
        case Expr.REAL: {
            print("<r>");
            print(Constant.doubleToString(c.realValue()));
            print("</r>");
            break;
        }
        case Expr.STRING:
            print("<s>");
            printQuotedString((String) c.value, (char) 0, true);
            print("</s>");
            break;
        default:
            print("<e>");
            printNative(exp, true);
            print("</e>");
            break;
        } // switch (exp.type)
    } // printXML(Expr)

    /** Print a string, protecting non-printable and other characters by
     * using escapes.  Inside the string, backslashes are doubled and
     * non-printing characters are replaced by sequences starting with a
     * backslash.  
     * If "escapeXML" is true, characters &lt;, &amp;, and &gt;
     * are replaced by &amp;lt;, &amp;amp;, and &amp;gt;, respectively.
     * Finally, occurences of " and ' are escaped according to the value of
     * "quote":
     * <dl>
     * <dt>"<dd>Double quotes are preceded by backslashes
     * <dt>'<dd>Single quotes (apostrophes) are preceded by backslashes
     * <dt>q</dd>Double quotes are replaced by &amp;quot;
     * <dt>\0</dd>No escaping of quotes
     * </dl>
     * The actual surrounding quotes, if any, are printed by the caller.
     * <p>
     * Note: The high-order 8 bits of each character are discarded.
     * @param s the string to be quoted.
     * @param quote information on how to escape quotes
     * @param escapeXML if true, use xml entities.
     */
    private final void printQuotedString(
                            String s, char quote, boolean escapeXML)
    {
        for (int i = 0; i < s.length(); i++) {
            int c = s.charAt(i) & 0xff;
            switch (c) {
            case '\n': print("\\n"); break;
            case '\t': print("\\t"); break;
            case '\b': print("\\b"); break;
            case '\r': print("\\r"); break;
            case '\f': print("\\f"); break;
            case '\\': print("\\\\"); break;
            case '"':
                if (quote == '"') {
                    print("\\\"");
                } else if (quote == 'q') {
                    print("&quot;");
                } else {
                    print('"');
                }
                break;
            case '\'':
                if (quote == '\'') {
                    print("\\'");
                } else {
                    print('\'');
                }
                break;
            case '<': print(escapeXML ? "&lt;" : "<"); break;
            case '>': print(escapeXML ? "&gt;" : ">"); break;
            case '&': print(escapeXML ? "&amp;" : "&"); break;
            default:
                if (c < 040 || c > 0176) {
                    print('\\');
                    print((char) ('0' + ((c >> 6) & 7)));
                    print((char) ('0' + ((c >> 3) & 7)));
                    print((char) ('0' + (c & 7)));
                } else {
                    print((char) c);
                }
                break;
            } // switch
        } // for
    } // printQuotedString(String, char, boolean)

    /** Print a double as a sequence of exactly 16 hex digits, representing
     * its internal representation, most significant byte first.
     * @param x the value to be printed.
     */
    private void printHex(double x) {
        long l = Double.doubleToLongBits(x);
        // This is equivalent to print(Long.toHexString(l)), but padded with
        // leading zeros.
        for (int i = 60; i >= 0; i -= 4) {
            print(Character.forDigit((int) ((l >> i) & 0xf), 16));
        }
    } // printHex(double)
} // ClassAdWriter
