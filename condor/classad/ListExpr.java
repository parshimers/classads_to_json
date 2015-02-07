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
 * A list of expressions.
 * An internal (non-leaf) node of an expression tree representing the
 * list-construction operator <code>{ expr, expr, ... }</code>.
 * @see Expr
 * @author <a href="mailto:solomon@cs.wisc.edu">Marvin Solomon</a>
 * @version 2.2
 */
public class ListExpr extends Expr {
    private static String VERSION = "$Id: ListExpr.java,v 1.21 2005/05/06 20:54:07 solomon Exp $";

    /** The component expressions of the list */
    private final List contents;

    /** Get the length of this list.
     * @return the number of elements in this list
     */
    public int size() {
        return contents.size();
    } // size()

    /** Construct an empty list. */
    public ListExpr() {
        super(LIST);
        contents = new ArrayList();
    } // ListExpr()

    /** Construct a list from a list of expressions.
     * @param l the array of expressions.
     */
    public ListExpr(List l) {
        super(LIST);
        contents = l == null ? new ArrayList(0) : new ArrayList(l);
    } // ListExpr(List)

    /** Get a member of this list.
     * @param i the member to be retrieved.
     * @return the ith member of this list, or an error Constant if i is out of
     * bounds.
     */
    public Expr sub(int i) {
        try {
            return (Expr) contents.get(i);
        } catch (IndexOutOfBoundsException e) {
            return Constant.error("subscript " + i + " is out of bounds");
        }
    } // sub(int)

    /** Appends the specified Expr to the end of this list.
     * @param e the Expr to add.
     * @return this ListExpr.
     */
    public Expr add(Expr e) {
        contents.add(e);
        return this;
    } // add(Expr)

    /** Get an iterator for iterating throught the members of this list.
     * @return an iterator for enumerating the Expr members of this list.
     */
    public Iterator iterator() {
        return contents.iterator();
    } // iterator

    /** Get the type of the expression.
     * @return the string "list".
     */
    protected String typeName() {
        return "list";
    } // typeName()

    /** Evaluate this Expr.  This is the internal method used to implement
     * {@link Expr#eval()}.
     * A List evaluates to itself.
     * @param env ignored.
     * @return this ListExpr.
     */
    protected Expr eval1(Env env) {
        return this;
    } // eval1(Env)

    /** Convert this Expr to a string, appending the result to the end of "sb".
     * The representation is the "canonical native format":
     * <code>'{' [ element [ ',' element ]* ] '}'</code> with no extra spaces.
     * @param sb a place to put the result.
     * @return sb.
     * @see ClassAdWriter
     */
    public StringBuffer toString(StringBuffer sb) {
        char sep = '{';
        for (Iterator exprs = contents.iterator(); exprs.hasNext();) {
            Expr expr = (Expr) exprs.next();
            expr.toString(sb.append(sep));
            sep = ',';
        }
        if (sep == '{') {
            sb.append(sep);
        }
        return sb.append('}');
    } // toString()

    /** Compare this Expr to another expression to check for "deep structural
     * equality".
     * @param other the other expression
     * @return true of this and the other expression are isomorphic.
     */
    public boolean sameAs(Expr other) {
        if (this.type != other.type) {
            return false;
        }
        ListExpr o = (ListExpr) other;
        if (contents.size() != o.contents.size()) {
            return false;
        }
        for (Iterator i = contents.iterator(), j = o.contents.iterator();
            i.hasNext(); )
        {
            Expr e1 = (Expr) i.next();
            Expr e2 = (Expr) j.next();
            if (!e1.sameAs(e2)) {
                return false;
            }
        }
        return true;
    } // sameAs(Expr)

    /** The precedence of the operator in this expression node.
     * Used to print expressions without superfluous parentheses.
     * @return the precendence of this node.
     * @see ClassAdWriter#MINIMAL_PARENTHESES
     */
    public int prec() {
        return MAXPREC+1;
    } // prec()
} // ListExpr
