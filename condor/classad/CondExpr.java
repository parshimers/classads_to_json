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
 * A conditional expression.
 * An internal (non-leaf) node of an expression tree representing the ternary
 * operator <i>cond</i> ? <cond>expr1</cond> : <cond>expr2</cond>.
 * @see Expr
 * @author <a href="mailto:solomon@cs.wisc.edu">Marvin Solomon</a>
 * @version 2.2
 */
public class CondExpr extends Expr {
    private static String VERSION = "$Id: CondExpr.java,v 1.21 2005/05/06 20:54:07 solomon Exp $";

    /** The condition (first component of the expression). */
    public final Expr ec;

    /** The true part (second component of the expression). */
    public final Expr et;

    /** The false part (third component of the expression). */
    public final Expr ef;

    /** Construct a conditional expression from its three components.
     * @param ec the condition (first component of the expression)
     * @param et the true part (second component of the expression)
     * @param ef the false part (third component of the expression)
     */
    public CondExpr(Expr ec, Expr et, Expr ef) {
        super(COND);
        this.ec = ec; this.et = et; this.ef = ef;
    } // CondExpr(Expr,Expr,Expr)

    /** Convert this Expr to a string, appending the result to the end of "sb".
     * The representation is the "canonical native format":
     * '(' condition '?' true-part ':' false-part ')', with no extra spaces. 
     * @param sb a place to put the result.
     * @return sb.
     * @see ClassAdWriter
     */
    public StringBuffer toString(StringBuffer sb) {
        sb.append('(');
        ec.toString(sb);
        sb.append('?');
        et.toString(sb);
        sb.append(':');
        ef.toString(sb);
        return sb.append(')');
    } // toString()

    /** Compare this Expr to another expression to check for "deep structural
     * equality".
     * @param other the other expression.
     * @return true of this and the other expression are isomorphic.
     */
    public boolean sameAs(Expr other) {
        if (this.type != other.type) {
            return false;
        }
        CondExpr o = (CondExpr) other;
        return ec.sameAs(o.ec) && et.sameAs(o.et) && ef.sameAs(o.ef);
    } // sameAs(Expr)

    /** The precedence of the operator in this expression node.
     * Used to print expressions without superfluous parentheses.
     * @return the precendence of this node.
     * @see ClassAdWriter#MINIMAL_PARENTHESES
     */
    public int prec() {
        return -1;
    } // prec()

    /** Evaluate this Expr.  This is the internal method used to implement
     * {@link Expr#eval()}.
     * @param env an environment used to evaluate the operands.  It is cleared
     * to the null environment before return.
     * @return a Constant representing the value.
     */
    protected Expr eval1(Env env) {
        Env env1 = new Env(env);
        Expr ecv = ec.eval(env1);

        switch (ecv.type) {
        case BOOLEAN:
            return ecv.isTrue() ? et.eval(env) : ef.eval(env);
        case UNDEFINED:
        case ERROR:
            env.clear();
            return ecv;
        default:
            env.clear();
            return Constant.error("type " + ecv.typeName()
                                + " found where boolean expected"
                                + " in conditional expression");
        }
    } // eval1(Env)
} // CondExpr
