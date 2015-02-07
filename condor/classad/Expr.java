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
import java.text.Collator;
import java.text.CollationKey;
import java.util.*;

/**
 * A node of an expression tree.  An Expr is one of
 * <dl>
 *   <dt>{@link RecordExpr}
 *       <dd>a "classad" of the form <code>[name = expr, ... ]</code>,
 *   <dt>{@link SelectExpr}
 *       <dd>a selection from a record as in <code>a.b</code>,
 *   <dt>{@link ListExpr}
 *       <dd>a list of the form <code>{ expr1, expr2, ... }</code>,
 *   <dt>{@link SubscriptExpr}
 *       <dd>a selection from a list as in <code>a[i]</code>,
 *   <dt>{@link CondExpr}
 *       <dd>a ternary conditional expression as in <code>a ? b : c</code>,
 *   <dt>{@link Op}
 *       <dd>a binary or unary operator applied to operand(s),
 *   <dt>{@link FuncCall}
 *       <dd>a call to a function with a list of argument expressions,
 *   <dt>{@link AttrRef}
 *       <dd>a reference to an attribute of a ClassAd in the form an
 *           identifier, or
 *   <dt>{@link Constant}
 *       <dd>a constant (literal) value
 * </dl>
 *
 * @see RecordExpr
 * @see SelectExpr
 * @see ListExpr
 * @see SubscriptExpr
 * @see CondExpr
 * @see Op
 * @see FuncCall
 * @see AttrRef
 * @see Constant
 * @author <a href="mailto:solomon@cs.wisc.edu">Marvin Solomon</a>
 * @version 2.2
 */
public abstract class Expr {
    private static String VERSION = "$Id: Expr.java,v 1.30 2005/05/06 20:54:07 solomon Exp $";

    // ---------------------------------- FIELDS -----------------------------

    /** The "type" of this expression.  For constants, the type is one of
     * UNDEFINED,
     * ERROR,
     * BOOLEAN,
     * INTEGER,
     * REAL,
     * STRING,
     * ABSOLUTE_TIME, or
     * RELATIVE_TIME.
     * <p>
     * For non-atomic values, the type is one of 
     * COND,
     * OP,
     * CALL,
     * SELECTION,
     * SUBSCRIPT,
     * ATTRIBUTE,
     * LIST, or
     * RECORD.
     * <p>
     * The values of these constants are chosen to facilitate classifying
     * an expression as a constant, constructed value (LIST or RECORD) or
     * other expression.
     * @see #isConstant()
     * @see #eval(condor.classad.Env)
     */
    public final int type;

    /** Flag to control level of verbosity of debugging input.  Higher values
     * are more verbose.  If the value is zero, no debugging output is printed.
     */
    public static int dblevel = 0;

    // ---------------------------------- CONSTANTS --------------------------

    /** A type flag indicating that this is a conditional expression. */
    public static final int COND = -8;

    /** A type flag indicating that this is a unary or binary operator. */
    public static final int OP = -7;

    /** A type flag indicating that this is a function call. */
    public static final int CALL = -6;

    /** A type flag indicating that this is a selection (a.b). */
    public static final int SELECTION = -5;

    /** A type flag indicating that this is a selection (a.b). */
    public static final int SUBSCRIPT = -4;

    /** A type flag indicating that this is an attribute reference. */
    public static final int ATTRIBUTE = -3;

    /** A type flag indicating that this is a list. */
    public static final int LIST = -2;

    /** A type flag indicating that this is a record (classad). */
    public static final int RECORD = -1;

    /** A type flag indicating that this is an undefined value. */
    public static final int UNDEFINED = 0;

    /** A type flag indicating that this is an error value. */
    public static final int ERROR = 1;

    /** A type flag indicating that this is a boolean value. */
    public static final int BOOLEAN = 2;

    /** A type flag indicating that this is an integer value. */
    public static final int INTEGER = 3;

    /** A type flag indicating that this is a real value. */
    public static final int REAL = 4;

    /** A type flag indicating that this is a string value. */
    public static final int STRING = 5;

    /** A type flag indicating that this is an absolute time value. */
    public static final int ABSOLUTE_TIME = 6;

    /** A type flag indicating that this is a relative time value. */
    public static final int RELATIVE_TIME = 7;

    /** Mapping of type codes to descriptive strings, used by typeName(). */
    private static final String[] tNames = {
        "conditional expression",
        "operator expression",
        "function call",
        "selection",
        "subscripted expression",
        "attribute reference",
        "list",
        "record",
        "undefined",
        "error",
        "boolean",
        "integer",
        "real",
        "string",
        "timestamp",
        "time interval"
    }; // tNames

    /** An undefined constant that represents the result a attempting to
     * resovle a cyclic attribute reference as in <code>[a=b;b=a]</code>.
     */
    private static final Constant CYCLIC_REF
                    = Constant.undefined("Cyclic attribute definition");

    /** A table mapping operator codes to character-string names.  For example,
     * <code>opName[NOT_EQUAL] = "!="</code>.
     */
    public static final String[] opName = {
        "||", "&&", "|", "^", "&", "==", "!=",
        " is ", " isnt ", "<", ">", "<=", ">=",
        "<<", ">>", ">>>", "+", "-", "*", "/",
        "%", "+", "-", "~", "!"
    }; // opName

    /** A table mapping operator codes to character-string names, escaped for
     * XML. For example,
     * <code>opName[LEFT_SHIFT] = "&amp;lt;&amp;lt;"</code>.
     */
    public static final String[] opNameXML = {
        "||", "&amp;&amp;", "|", "^", "&amp;", "==", "!=",
        " is ", " isnt ", "&lt;", "&gt;", "&lt;=", "&gt;=",
        "&lt;&lt;", "&gt;&gt;", "&gt;&gt;&gt;", "+", "-", "*", "/",
        "%", "+", "-", "~", "!"
    }; //opNameXML

    /** Token ID corresponding to the operator <b>||</b>. */
    public static final int OR = 0;
    /** Token ID corresponding to the operator <b>&amp;&amp;</b>. */
    public static final int AND = 1;
    /** Token ID corresponding to the operator <b>|</b>. */
    public static final int BITOR = 2;
    /** Token ID corresponding to the operator <b>^</b>. */
    public static final int BITXOR = 3;
    /** Token ID corresponding to the operator <b>&amp;</b>. */
    public static final int BITAND = 4;
    /** Token ID corresponding to the operator <b>==</b>. */
    public static final int EQUAL = 5;
    /** Token ID corresponding to the operator <b>&#33;=</b>. */
    public static final int NOT_EQUAL = 6;
    /** Token ID corresponding to the operator <b> is </b>. */
    public static final int SAME = 7;
    /** Token ID corresponding to the operator <b> isnt </b>. */
    public static final int DIFFERENT = 8;
    /** Token ID corresponding to the operator <b>&lt;</b>. */
    public static final int LESS = 9;
    /** Token ID corresponding to the operator <b>&gt;</b>. */
    public static final int GREATER = 10;
    /** Token ID corresponding to the operator <b>&lt;=</b>. */
    public static final int LESS_EQ = 11;
    /** Token ID corresponding to the operator <b>&gt;=</b>. */
    public static final int GREATER_EQ = 12;
    /** Token ID corresponding to the operator <b>&lt;&lt;</b>. */
    public static final int LEFT_SHIFT = 13;
    /** Token ID corresponding to the operator <b>&gt;&gt;</b>. */
    public static final int RIGHT_SHIFT = 14;
    /** Token ID corresponding to the operator <b>&gt;&gt;&gt;</b>. */
    public static final int URIGHT_SHIFT = 15;
    /** Token ID corresponding to the operator <b>+</b>. */
    public static final int PLUS = 16;
    /** Token ID corresponding to the operator <b>-</b>. */
    public static final int MINUS = 17;
    /** Token ID corresponding to the operator <b>*</b>. */
    public static final int TIMES = 18;
    /** Token ID corresponding to the operator <b>/</b>. */
    public static final int DIV = 19;
    /** Token ID corresponding to the operator <b>%</b>. */
    public static final int MOD = 20;
    /** Token ID corresponding to the operator <b>unary +</b>. */
    public static final int UPLUS = 21;
    /** Token ID corresponding to the operator <b>unary -</b>. */
    public static final int UMINUS = 22;
    /** Token ID corresponding to the operator <b>unary ~</b>. */
    public static final int BIT_COMPLEMENT = 23;
    /** Token ID corresponding to the operator <b>unary &#33;</b>. */
    public static final int NOT = 24;

    /** The maximum precedence in the grammar for ClassAd expressions. */
    static protected final int MAXPREC = 11;

    // ---------------------------------- CONSTRUCTORS -----------------------

    /** Create an Expr node of a given type.  Used in constructors of
     * subclasses.
     * @param type the type of Expr to create.
     */
    protected Expr(int type) {
        this.type = type;
    } // Expr(int)

    // ---------------------------------- FACTORY METHODS --------------------

    /** A convenience function for creating a SelectExpression corresponding
     * to "this.selector".
     * @param selector a selector.
     * @return a new SelectExpr corresponding to "this.selector".
     */
    public SelectExpr selectExpr(String selector) {
        return new SelectExpr(this, selector);
    } // selectExpr(String)

    /** A convenience function for creating a SubscriptExpression corresponding
     * to "this[subscript]".
     * @param subscript a subscript.
     * @return a new SelectExpr corresponding to "this[subscript]".
     */
    public SubscriptExpr subExpr(int subscript) {
        return new SubscriptExpr(this, ClassAd.constant(subscript));
    } // subExpr(int)

    // ---------------------------------- TYPES AND COERSION -----------------

    /** Determine the type of this expression (for printing messages). 
     * @return a string identifing the type of this constant.
     */
    protected String typeName() {
        return tNames[type - COND];
    } // typeName()

    /** Test whether this expr is a "constant".
     * @return true if this expr is a "constant" in the sense that it evaluates
     * to itself -- That is, if it is a Contant, List, or Record.
     */
    public final boolean isConstant() {
        // Note that constant types are non-negative, and the types LIST and
        // RECORD are -2 and -1, resp.
        return type >= LIST;
    } // isConstant()

    /** Convert to an integer constant if possible.
     * @exception ArithmeticException if this Expr is not an integer Constant.
     * @return the integer value of this Expr.
     */
    public int intValue() throws ArithmeticException {
        throw new ArithmeticException(typeName() + " " + this
                + " in integer context");
    } // intValue()

    /** Convert to a double value if possible.
     * @exception ArithmeticException if this Expr is not an integer or
     *                                real constant.
     * @return the value of of this Expr, converted to double.
     */
    public double realValue() throws ArithmeticException {
        throw new ArithmeticException(typeName() + " " + this
            + " in real context");
    } // realValue()

    /** Convert to a String value, if possible.
     * @exception ArithmeticException if this Expr is not an String Constant.
     * @return the string value of this Expr.
     */
    public String stringValue() throws ArithmeticException {
        throw new ArithmeticException(typeName() + " " + this
                + " in string context");
    } // stringValue()

    /** Convert to a boolean value.
     * Note that unlike intValue, etc., this method never throws an
     * exception.
     * @return true if this Expr is a constant of type boolean with value true,
     * and false in all other cases.
     */
    public boolean isTrue() {
        return false;
    } // isTrue()

    // ---------------------------------- COMPARISON -------------------------

    /** Compare this Expr to another expression to check for "deep structural
     * equality".
     * @param other the other expression
     * @return true if this and the other expression are isomorphic.
     */
    public abstract boolean sameAs(Expr other);

    /** Compare this Expr to another expression in the sense of the "is"
     * operator.
     * @param other the other expression
     * @return true if this expression is identical to the other expression.
     */
    public boolean is(Expr other) {
        if (! (other instanceof Expr)) {
            return false;
        }

        // Must be the same type (in particular, "3 is 3.0" is false).
        // Note that this also handles cases like "3 is undefined" etc.
        if (type != other.type) {
            return false;
        }

        // Non-constants are compared for identity
        if (!(this instanceof Constant)) {
            return this == other;
        }
        
        Constant const1 = (Constant) this;
        Constant const2 = (Constant) other;

        switch (const1.type) {
        // Strings must be identical (including case)
        case STRING:
            String s1 = const1.stringValue();
            String s2 = const2.stringValue();
            return s1.equals(s2);

        // Numeric constants are compared by comparing their values
        case INTEGER:
            return const1.intValue() == const2.intValue();
        case REAL:
            return const1.realValue() == const2.realValue();
        // There are only two boolean constants
        case BOOLEAN:
            return this == other;

        // The remaining cases (Undefined and Error) have already
        // been checked by the type != other.type check above.
        default:
            return true;
        } // switch (const1.type)
    } // is(Expr)

    // Accessors.  Each method throws an ArithmeticException if this Expr
    // is not a Constant of the appropriate type.

    // ---------------------------------- EVALUATION -------------------------

    /** Evaluate this Expr.  This is the internal method used to implement
     * {@link Expr#eval()}.
     * Each type of expression implements it differently.  Lists, records, and
     * constants evaluate to themselves.  Function calls and operators evaluate
     * to the result of applying the operartor or function to the (recursively
     * evaluated) arguments.  Attribute references are evaluated by looking up
     * the reference in the env.
     * @param env an environment consisting of a list of RecordExprs (innermost
     * first) used to resolve AttrRefs.  In the case of AttrRef, env is updated
     * to remove all scopes preceding the one where the reference was resovled.
     * @return a Constant, AttrRef, ListExpr, or RecordExpr representing the
     * value.
     */
    protected abstract Expr eval1(Env env);

    /** Evaluate this Expr in a given environment.
     * Lists, records, and constants evaluate to themselves.  Function calls
     * and operators evaluate to the result of applying the operartor or
     * function to the (recursively evaluated) arguments.  Attribute references
     * are evaluated by looking up the reference in the env.
     * If the result is an AttrRef, it is re-evalutated.  If this re-evaluation
     * leads to a cycle (an attribute defined in terms of itself), the result
     * is the UNDEFINED constant.
     * <p><b>Warning</b>: This method may update the env parameter in place.
     * The caller may want to create a copy of the parameter by
     * calling {@link Env#Env(condor.classad.Env) new Env(env)}.
     * @param env an environment consisting of a list of RecordExprs (innermost
     * first) used to resolve AttrRefs.  In the case of AttrRef, env is updated
     * to remove all scopes preceding the one where the reference was resovled.
     * @return a Constant, ListExpr, or RecordExpr representing the value.
     */
    public Expr eval(Env env) {
        // Special case, for efficiency.
        if (isConstant()) {
            return this;
        }
        Expr e = this;
        // Keep a set of AttrRef nodes that have already been seen.
        Set seen = new HashSet();
        while (e.type < LIST) {
            if (seen.contains(e)) {
                return CYCLIC_REF;
            }
            seen.add(e);
            e = e.eval1(env);
        }
        return e;
    } // eval(Env)

    /** Evaluate this Expr in a "top-level" (empty) environment.
     * Lists, records, and constants evaluate to themselves.  Function calls
     * and operators evaluate to the result of applying the operartor or
     * function to the (recursively evaluated) arguments.  Attribute references
     * evaluate to the UNDEFINED constant.
     * @return a Constant, ListExpr, or RecordExpr representing the value.
     */
    public Expr eval() {
        // Special cases, for efficiency.
        if (type == ATTRIBUTE) {
            return Constant.undefined("attribute " + this + " not found");
        }
        if (isConstant()) {
            return this;
        }
        return eval(new Env());
    } // eval()

    // ---------------------------------- DEBUGGING --------------------------

    /** Convert this Expr to a string.
     * The representation is the "canonical native format".
     * Use a ClassAdWriter for other representations and for finer conctrol
     * over formatting.
     * @return a string representation of this Expr
     * @see ClassAdWriter
     */
    public String toString() {
        return toString(new StringBuffer()).toString();
    } // toString()

    /** Convert this Expr to a string, appending the result to the end of "sb".
     * The representation is the "canonical native format".
     * @param sb a place to put the result.
     * @return sb.
     * @see ClassAdWriter
     */
    public abstract StringBuffer toString(StringBuffer sb);

    /** The precedence of the operator in this expression node.
     * Used to print expressions without superfluous parentheses.
     * @return the precendence of this node.
     * @see ClassAdWriter#MINIMAL_PARENTHESES
     */
    /*package*/ abstract int prec();

    /** Debugging print.
     * The message is printed to System.out, followed by a newline.
     * Intended usage is something like
     * <pre>
     *    if (dblevel &gt; 1) db("The value of foo is " +foo);
     * </pre>
     * @param msg the message to be printed.
     * @see #dblevel
     */
    public static void db(Object msg) {
        System.out.println(msg);
    } // db(Object)

    /** Debugging print.
     * The message is printed to System.out.
     * @param newline if true, terminate the message with a newline.
     * @param msg the message to be printed.
     * @see #db(java.lang.Object)
     */
    public static void db(boolean newline, Object msg) {
        if (newline) {
            System.out.println(msg);
        } else {
            System.out.print(msg);
        }
    } // db(boolean,Object)

} // Expr
