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

/**
 * An internal (non-leaf) node of an expression tree, representing a selection
 * of component of a record.  It has the form from a class ad in the form
 * <pre>
 *    &lt;expr&gt; . &lt;identifier&gt;.
 * </pre>
 * @see Expr
 * @author <a href="mailto:solomon@cs.wisc.edu">Marvin Solomon</a>
 * @version 2.2
 */
public class SelectExpr extends Expr {
    private static String VERSION = "$Id: SelectExpr.java,v 1.25 2005/05/06 20:54:07 solomon Exp $";

    /** The left argument (base expression) of the selection operator. */
    public final Expr base;

    /** The right argument (identifier) of the selection operator. */
    public final AttrName selector;

    /** Create a new SelectExpr corresponding to <code>base.selector</code>.
     * @param base the left argument (base expression).
     * @param selector the right argument (identifier).
     */
    public SelectExpr(Expr base, String selector) {
        this(base, AttrName.fromString(selector));
    } // SelectExpr(Expr,String)

    /** Create a new SelectExpr corresponding to <code>base.selector</code>.
     * @param base the left argument (base expression).
     * @param selector the right argument (identifier).
     */
    public SelectExpr(Expr base, AttrName selector) {
        super(SELECTION);
        this.base = base;
        this.selector = selector;
    } // SelectExpr(Expr,AttrName)

    /** A convenience function for creating a SelectExpression corresponding
     * to "base.a.b.c".
     * @param base the base Expr.
     * @param sel the sequence of selectors <code>{"a", "b", "c"}</code>
     * @return a new SelectExpr or <code>base</code> if <code>sel</code> is
     * empty.
     */
    public static Expr select(Expr base, String[] sel) {
        for (int i=0; i<sel.length; i++) {
            base = new SelectExpr(base, AttrName.fromString(sel[i]));
        }
        return base;
    } // select(Expr,String)

    /** The type of the expression.  This should never be called!
     * @return the string "selection".
     */
    protected String typeName() {
        return "selection";
    } // typeName()

    /** Convert this Expr to a string, appending the result to the end of "sb".
     * The representation is the "canonical native format":
     * <code>base '.' selector</code>, with no extra spaces.
     * @param sb a place to put the result.
     * @return sb.
     * @see ClassAdWriter
     */
    public StringBuffer toString(StringBuffer sb) {
        return 
            base.toString(sb).append('.').append(selector);
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
        SelectExpr o = (SelectExpr) other;
        return selector.equals(o.selector) && base.sameAs(o.base);
    } // sameAs(Expr)

    /** The precedence of the operator in this expression node.
     * Used to print expressions without superfluous parentheses.
     * @return the precendence of this node.
     * @see ClassAdWriter#MINIMAL_PARENTHESES
     */
    public int prec() {
        return MAXPREC+1;
    } // prec()

    /** Evaluate this Expr.  This is the internal method used to implement
     * {@link Expr#eval()}.
     * The base is evaluated to yield a record or list.  In the case of a
     * record, the selector is used to choose the appropriate component,
     * and the record is added to the environment.  In the case of a list,
     * the selection is distributed across the list.   In all other cases,
     * the result is the ERROR constant.
     * @param env an environment used to evaluate the base.  If the base
     * evaluates to a RecordExpr, it is added to the env (as the first
     * (innermost) scope) before returning.
     * @return the result of the selection.
     */
    protected Expr eval1(Env env) {
        Expr b = base.eval(env);
        Expr result;
        if (b.type == UNDEFINED || b.type == ERROR) {
            result = b;
            env.clear();
        } else if (b.type == RECORD) {
            // If base evaluates to b in the environment
            //   E = [ ... [ ... [ ... b ... ] ... ]; ... x = e;  ... ], 
            // then b.x evaluates to e in environment E.
            result = ((RecordExpr) b).lookup(selector, env);
            if (result == null) {
                result = Constant.undefined("attribute " + selector
                                        + " not found in expression " + b);
                env.clear();
            } else {
                env.push((RecordExpr) b);
            }
        } else if (b.type == LIST) {
            // { a, b, c }.x ==> { eval(a.x), eval(b.x), eval(c.x)}
            // Evaluation is "eager" and the result is in the empty
            // environment.
	    ListExpr l = new ListExpr();
	    for(Iterator i = ((ListExpr) b).iterator(); i.hasNext();) {
		Expr e = (Expr)i.next();
                l.add((new SelectExpr(e,selector)).eval(new Env(env)));
	    }
	    result = l;
            env.clear();
	} else {
            // something_else.x 
            env.clear();
            result = Constant.error(
                        b + "." + selector
                        + ": argument is not a record or list");
        }
        return result;
    } // eval1(Env)
} // SelectExpr
