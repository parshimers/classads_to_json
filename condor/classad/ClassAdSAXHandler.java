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
import java.util.Stack;
import java.util.Date;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.helpers.DefaultHandler;

/** A handler for SAX (Simple API for XML) events from a SAXParser.
 * It uses those events to build up an Expr object.
 * Note that this class is not public; it may only be referenced within the
 * condor.classad package.  It should only be referenced by ClassAdParser.
 * @see ClassAdParser
 * @author <a href="mailto:solomon@cs.wisc.edu">Marvin Solomon</a>
 * @version 2.2
 */
class ClassAdSAXHandler extends DefaultHandler {
    private static String VERSION = "$Id: ClassAdSAXHandler.java,v 1.13 2005/05/06 20:54:07 solomon Exp $";

    // ******** Internal state ***********

    /** Stack of partially constructed expressions, innermost at top. */
    private Stack stack = new Stack();

    /** Concatenated PCDATA content of current n, s, e, or t element). */
    private StringBuffer pcdata = new StringBuffer();

    /** Result of parsing pcdata */
    private Expr element;

    /** A parser for parsing &lt;e&gt; elements. */
    private ClassAdParser caParser;

    /** Handle on the original document for reporting the locations of errors.
     */
    Locator locator;

    /** Verbosity of error messages.
     * <dl>
     * <dt>0<dd>no messages
     * <dt>1<dd>only show message and line number
     * <dt>2<dd>also show info about the input document
     * </dl>
     */
    private int verbosity = 2;

    /** Distination for error messages (if verbosity &gt; 0).
     * The invariant is maintained that if verbosity &gt; 0, then errs != null.
     */
    private PrintStream errs = System.err;

    // ******** Public interface ***********

    /** Get and remove the result of the most recent parse, if any.
     * @return the resulting Expr, or null.
     */
    public Expr getResult() {
        if (stack.empty()) {
            return null;
        } else {
            return (Expr) stack.pop();
        }
    } // getResult

    // ******** Overridden DefaultHandler methods ***********

    /** Called by SAXParser to deliver a locator.
     * @param locator An object that can return the location of any SAX
     *                document event.
     */
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    } // setDocumentLocator

    /** Called by SAXParser to announce warnings.
     * @param e The warning information encoded as an exception.
     */
    public void warning(SAXParseException e) {
        printMessage("Warning", e);
    } // warning

    /** Called by SAXParser if it discovers a (non-fatal) error. */
    public void error(SAXParseException e) {
        printMessage("Error", e);
    } // error

    /** Called by SAXParser after recognizing some "PCDATA" (parsed character
     * data).  Simply appends the characters to pcdata.
     * @param ch The characters.
     * @param start The start position in the character array.
     * @param length The number of characters to use from the character array. 
     */
    public void characters(char[] ch, int start, int length) {
        pcdata.append(ch, start, length);
    } // charaacters

    /** Called by SAXParser at the start of an element after parsing the start
     * tag.
     * @param uri currenly not documented; appears always to be ''.
     * @param localName The local name (without prefix), or the empty string if
     *                  Namespace processing is not being performed.  Currently
     *                  ignored.
     * @param name The qualified name (with prefix), or the empty string if
     *                  qualified names are not available.
     * @param attrs The specified or defaulted attributes.  For classad.dtd,
     *              there is at most one attribute per tag.
     * @throws SAXException if something goes wrong.
     */
    public void startElement(String uri,
                             String localName,
                             String name,
                             Attributes attrs)
            throws SAXException
    {
        pcdata.setLength(0);
        if ("classads".equals(name) || "l".equals(name)) {
            // <classads> is treated just like <l>: Just push an empty
            // ListExpr onto the stack.
            stack.push(new ListExpr());
        } else if ("c".equals(name)) {
            // <c>:  push an empty RecordExpr on the stack.
            stack.push(new RecordExpr());
        } else if ("a".equals(name)) {
            // <a n="x">:  save "x"
            stack.push(
                AttrName.fromString(
                    Constant.unquoteString(
                        attrs.getValue(0))));
        } else if ("b".equals(name)) {
            // <b v="t"> or <b v="f">
            addElement(attrs.getValue(0).charAt(0) == 't'
                        ? Constant.TRUE : Constant.FALSE);
        } else if ("un".equals(name)) {
            // <un/>
            addElement(Constant.undefined(attrs.getValue(0)));
        } else if ("er".equals(name)) {
            // <er/>
            addElement(Constant.error(attrs.getValue(0)));
        }
        // Ignore all other start tags.
    } // startElement

    /** Called by the SAXParser after parsing the end tag of an element.
     * @param uri currenly not documented; appears always to be ''.
     * @param localName The local name (without prefix), or the empty string if
     *                  Namespace processing is not being performed.  Currently
     *                  ignored.
     * @param name The qualified name (with prefix), or the empty string if
     *                  qualified names are not available.
     */
    public void endElement(String uri,
                             String localName,
                             String name)
    {
        try {
            if ("i".equals(name)) {
                // <i> integer </i>
                String data = pcdata.toString().trim();
                try {
                    addElement(Constant.getInstance(Integer.parseInt(data)));
                } catch (NumberFormatException e) {
                    addElement(
                        errorConstant(
                            "ill-formed integer '" + data + "'", locator));
                }
            } else if ("r".equals(name)) {
                // <r> value </r> where value is anything acceptable to
                // Double.parseDouble, as well as INF, -INF, and NaN
                // (case-independent).
                String data = pcdata.toString().trim();
                try {
                    addElement(
                        Constant.getInstance(Constant.stringToDouble(data)));
                } catch (NumberFormatException e) {
                    addElement(
                        errorConstant(
                            "ill-formed real '" + data + "'", locator));
                }
            } else if ("s".equals(name)) {
                // <s>string</s>
                addElement(
                    Constant.getInstance(
                        Constant.unquoteString(
                            pcdata.toString())));
            } else if ("at".equals(name)) {
                String data = pcdata.toString().trim();
                addElement(Constant.stringToAbsTime(data));
            } else if ("rt".equals(name)) {
                String data = pcdata.toString().trim();
                addElement(Constant.stringToRelTime(data));
            } else if ("e".equals(name)) {
                // <e>expression</e>
                String data = pcdata.toString().trim();
                if (caParser == null) {
                    caParser = new ClassAdParser(data);
                    caParser.setVerbosity(verbosity);
                    caParser.setErrorStream(errs);
                } else {
                    caParser.reset(data);
                }
                Expr value = caParser.parse();
                if (value == null) {
                    value = errorConstant("invalid expression '" + data + "'",
                                locator);
                }
                addElement(value);
            } else if ("c".equals(name) ||"l".equals(name)) {
                // <c><a...>...</a> ... </c>
                Expr elt = (Expr) stack.pop();
                addElement(elt);
            }
            // The remaining tags have no content, so they are all handled by
            // startElement().
        } catch (Throwable t) {
            // FIXME
            // The current implementation of org.apache.crimson.parser.Parser2
            // apparently wraps a call to this method inside an promiscuous
            // catch clause that somehow loses the stack trace.  You get
            // a trace that ends with Parser2.parseInternal(Parser2.java:524)
            // losing all info about the real point where the error occurred.
            t.printStackTrace();
            System.exit(1);
        }
    } // endElement

    // ******** Private helper methods ***********

    /** Helper function to dispose of a completed element.
     * If the stack is empty (this is a "top-level" element), just push it
     * onto the stack.  Otherwise, add it to the List or Record at the top
     * of the stack (after popping an AttrName that may have been pushed
     * by an &lt;a&gt; element containing this element.
     * @param e the parsed element to add.
     */
    private final void addElement(Expr e) {
        if (stack.empty()) {
            stack.push(e);
        } else {
            Object top = stack.peek();
            if (top.getClass() == AttrName.class) {
                AttrName a = (AttrName) stack.pop();
                ((RecordExpr) stack.peek()).insertAttribute(a, e);
            } else {
                ((ListExpr) top).add(e);
            }
        }
    } // addElement

    /** Helper function for error() and warning().  Print an appropriate
     * message.
     * @param msg "Error" or "Warning"
     * @param e the exception associated with the error or warning.
     */
    private void printMessage(String msg, SAXParseException e) {
        if (verbosity == 0) {
            return;
        }
        errs.print(msg + " at line " + e.getLineNumber());
        int col = e.getColumnNumber();
        if (col > 0) {
            errs.print(" col " + col);
        }
        String id = e.getPublicId();
        if (id == null) {
            id = e.getSystemId();
        }
        if (verbosity > 1) {
            errs.println(" of " + id + ":\n    " + e);
        } else {
            errs.println(e.getMessage());
        }
    } // printMessage(String,SAXParseException)

    /** Print a message, decorated with the current line and column number.
     * @param msg the message to print.
     */
    /*package*/ void printMessage(String msg) {
        if (verbosity == 0) {
            return;
        }
        errs.print(msg);
        if (locator != null) {
            errs.print(" at line ");
            errs.print(locator.getLineNumber());
            int col = locator.getColumnNumber();
            if (col > 0) {
                errs.print(" col" + col);
            }
        }
        if (verbosity > 1) {
            String id = locator.getPublicId();
            if (id == null) {
                id = locator.getSystemId();
            }
            if (id != null) {
                errs.print(" of " + id);
            }
        }
        errs.println();
    } // printMessage(String)

    /** Format a parsing error as an ERROR constant.
     * @param msg information about the error.
     * @param locator source for line-number information.
     * @return an ERROR constant.
     */
    private static Expr errorConstant(String msg, Locator locator) {
        StringBuffer sb = new StringBuffer(msg);
        if (locator != null) {
            sb.append(" at line ")
                .append(locator.getLineNumber());
            int col = locator.getColumnNumber();
            if (col > 0) {
                sb.append(" col" + col);
            }
        }
        return Constant.error(sb.toString());
    } // errorConstant(String,Locator)

    /** Sets the verbosity and error output stream.
     * Should only be called from {@link ClassAdParser}.  No sanity checking is
     * done here.
     * @param level the new verbosity.
     * @param dest the new error stream.
     */
    /*package*/ void setVerbosity(int level, PrintStream dest) {
        verbosity = level;
        errs = dest;
        if (caParser != null) {
            caParser.setVerbosity(level);
            caParser.setErrorStream(dest);
        }
    } // setVerbosity(int,PrintStream)
} // ClassAdSAXHandler
