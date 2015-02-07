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

/**
 * A selection from a list.
 * An internal (non-leaf) node of an expression tree representing a subscript
 * applied to base expression: <code>base[selector]</code>.  Two
 * combinations of types are possible:  Either the base is a ListExpr and
 * the selector evaluates to an integer, or the base is a RecordExpr and the
 * subscript evaluates to a string.  In the latter case, <code>base["x"]</code>
 * is equivalent to <code>base.x</code> except that the "selector" does not
 * have to be a literal string.
 * @see Expr
 * @author <a href="mailto:solomon@cs.wisc.edu">Marvin Solomon</a>
 * @version 2.2
 */
public class SubscriptExpr extends Expr {
    private static String VERSION = "$Id: SubscriptExpr.java,v 1.19 2005/05/06 20:54:07 solomon Exp $";

    /** The left argument (base expression) of the subscript operator. */
    public final Expr base;

    /** The right argument (subscript) of the selection operator. */
    public final Expr selector;

    /** Create a new SubscriptExpr.
     * @param base the left argument (base expression).
     * @param selector the right argument (subscript).
     */
    public SubscriptExpr(Expr base, Expr selector) {
        super(SUBSCRIPT);
        this.base = base;
        this.selector = selector;
    } // SubscriptExpr(Expr,Expr)

    /** The type of the expression.  This should never be called!
     * @return the string "selection".
     */
    protected String typeName() {
        return "subscript";
    } // typeName()

    /** Convert this Expr to a string, appending the result to the end of "sb".
     * The representation is the "canonical native format":
     * <code>base '[' selector ']'</code>, with no extra spaces.
     * @param sb a place to put the result.
     * @return sb.
     * @see ClassAdWriter
     */
    public StringBuffer toString(StringBuffer sb) {
        base.toString(sb).append('[');
        return selector.toString(sb).append(']');
    } // toString(StringBuffer)

    /** Compare this Expr to another expression to check for "deep structural
     * equality".
     * @param other the other expression
     * @return true of this and the other expression are isomorphic.
     */
    public boolean sameAs(Expr other) {
        if (this.type != other.type) {
            return false;
        }
        SubscriptExpr o = (SubscriptExpr) other;
        return base.sameAs(o.base) && selector.sameAs(o.selector);
    } // sameAs(Expr)

    /** The precedence of the operator in this expression node.
     * Used to print expressions without superfluous parentheses.
     * @return the precendence of this node.
     * @see ClassAdWriter#MINIMAL_PARENTHESES
     */
    public int prec() {
        return MAXPREC;
    } // prec()

    /** Evaluate this Expr.  This is the internal method used to implement
     * {@link Expr#eval()}.
     * The base and selector should evaluate to a List and an integer or
     * a Record and a string.  In the latter case, this SubscriptExpr is
     * semantically equivalent to a SelectExpr.  In either case, the
     * appropriate component of the base value is then returned.  If the types
     * of the base and selector do not mactch, the result is the ERROR
     * constant.
     * @param env an environment used to evaluate the base and selector;
     *            it is updated to the resulting environment from the
     *            evaluation of the base.
     * @return the result of the subscripting operation.
     */
    protected Expr eval1(Env env) {
        Expr i = selector.eval(new Env(env));
        Expr b = base.eval(env);
        Expr result;
        switch (b.type) {
        case LIST:
            if (i.type != INTEGER) {
                env.clear();
                return Constant.error(
                            "List[" + i.typeName() + "]: type mismatch");
            }
            return ((ListExpr) b).sub(i.intValue());
        case RECORD:
            // rec["foo"] is treated like ref.foo
            RecordExpr r = (RecordExpr) b;
            if (i.type == STRING) {
                result = r.lookup(AttrName.fromString(i.stringValue()), env);
            } else {
                env.clear();
                return Constant.error(
                            "List[" + i.typeName() + "]: type mismatch");
            }
            if (result == null) {
                result = Constant.undefined("attribute " + selector
                                        + " not found in expression " + b);
                env.clear();
            } else {
                env.push((RecordExpr) base);
            }
            return result;
        default:
            return Constant.error("[] applied to " + b.typeName());
        } // switch (b.type)
    } // eval1(Env)
} // SubscriptExpr
