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
 * A reference to another attribute.
 * A leaf node in an expression tree that represents an attribute reference.
 * @see Expr
 * @see RecordExpr
 * @author <a href="mailto:solomon@cs.wisc.edu">Marvin Solomon</a>
 * @version 2.2
 */
public class AttrRef extends Expr {
    private static String VERSION = "$Id: AttrRef.java,v 1.22 2005/05/06 20:54:06 solomon Exp $";

    /** The string name associated with this reference. */
    public final AttrName name;

    /** Create a new AttrRef.
     * @param name the string name associated with this reference.
     */
    public AttrRef(AttrName name) {
        super(ATTRIBUTE);
        this.name = name;
    } // AttrRef(AttrName)

    /** Create a new AttrRef.
     * @param name the string name associated with this reference.
     */
    public AttrRef(String name) {
        this(AttrName.fromString(name));
    } // AttrRef(AttrName)

    /** Convert this AttrRef to a string.
     * The representation is the "canonical native format".  If the name
     * is an identifier (a string of letters, digits, and underscores, not
     * starting with a digit), it is just the name itself.  Otherwise, it is
     * surrounded with single quotes (apostrophes) and non-printing characters,
     * backslashes and apostrophes are backslash-escaped.
     */
    public String toString() {
        return name.toString();
    } // toString()

    /** Compare this Expr to another expression to check for "deep structural
     * equality".
     * @param other the other expression
     * @return true if "this" and "other" are isomorphic.
     */
    public boolean sameAs(Expr other) {
        if (this.type != other.type) {
            return false;
        }
        AttrRef o = (AttrRef) other;
        return name.equals(o.name);
    } // sameAs(Expr)

    /** Convert this AttrRef to a string, appending the result to the end of
     * "sb".  The representation is the "canonical native format".  If the name
     * is an identifier (a string of letters, digits, and underscores, not
     * starting with a digit), it is just the name itself.  Otherwise, it is
     * surrounded with single quotes (apostrophes) and non-printing characters,
     * backslashes and apostrophes are backslash-escaped.
     * @param sb a place to put the result.
     * @return sb.
     * @see ClassAdWriter
     */
    public StringBuffer toString(StringBuffer sb) {
        return sb.append(name.toString());
    } // toString(StringBuffer)

    /** Evaluate this Expr.  This is the internal method used to implement
     * {@link Expr#eval()}.
     * The env is searched for name of this AttrRef.  Scopes that do not
     * define that name are popped off the stack, leaving the scope defining
     * the name at the top.  If the name is not found in any of the scopes,
     * the env becomes the empty environment, and the result is the UNDEFINED
     * constant.
     * @param env the environment to search for the name.  It is modified in
     * place to indicate where the name was resolved.
     * @return a Constant, AttrRef, ListExpr, or RecordExpr representing the
     * value.
     */
    protected Expr eval1(Env env) {
        Expr result;
        if (name.equals(AttrName.PARENT)) {
            // "parent" in the environment A(B(C(...))) (where C is innermost)
            // evaluates to B in the environment A.
            result = env.pop(2);
            if (result == null) {
                result =  Constant.undefined("No parent record");
            }
        } else {
            result = env.search(name);
            if (result == null) {
                result
                    = Constant.undefined("attribute " + name + " not found");
            }
        }
        return result;
    } // eval1(Env)

    /** The precedence of this expression.
     * @return the precendence of this node (MAXPREC).
     */
    public int prec() {
        return MAXPREC;
    } // prec()
} // AttrRef
