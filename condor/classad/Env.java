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

/** An environment for evaluating expressions.
 * Logically, it is the stack of RecordExprs containing the expression
 * being evaluated, with the innermost on top.  It is constructed in such a
 * way that it can be updated "in place".  When passed as an argument e to
 * eval, e.next.recEx is treated as a pointer to the top of the stack and
 * the e.recEx is ignored.  If e.next == null, e represents an empty stack.
 * @author <a href="mailto:solomon@cs.wisc.edu">Marvin Solomon</a>
 * @version 2.2
 */
public class Env {
    /** The top (innermost) RecordExpr.  */
    private RecordExpr recEx;

    /** Pointer to the rest of the stack.  */
    private Env next;

    /** Construct a new Env from its components.
     * @param recEx the top (innermost) RecordExpr.
     * @param next the rest of the stack.
     */
    public Env(RecordExpr recEx, Env next) {
        this.recEx = recEx;
        this.next = next;
    } // Env.Env(RecordExpr,Env)

    /** Create a new "empty" Env. */
    public Env() {
    } // Env.Env()

    /** Create a "clone" of a given Env.  The components are shared.
     * @param env the Env to be copied.
     */
    public Env(Env env) {
        this.recEx = env.recEx;
        this.next = env.next;
    } // Env.Env(Env)

    /** Change this Env to the "empty" env. */
    public void clear() {
        recEx = null;
        next = null;
    } // Env.clear()

    /** Push a new RecordExpr onto the stack.
     * @param recEx the RecordExpr to be pushed.
     */
    public void push(RecordExpr recEx) {
        next = new Env(recEx, next);
    } // Env.push(RecordExpr)

    /** Pops "n" records off the stack.
     * @param n the number of records to pop.
     * @return the last record popped, or null if the stack had fewer than
     * "n" records.
     */
    public RecordExpr pop(int n) {
        RecordExpr result = null;
        while (n-- > 0) {
            if (next == null) {
                return null;
            }
            result = next.recEx;
            next = next.next;
        }
        return result;
    } // Env.pop(int)

    /** Search for a given attribute name in the RecordExprs on the
     * stack, from innermost to outermost.  If the name is found,
     * return the corresponding value, and pop all higher elements off
     * the stack in this Env.  If the name is not found, reset this
     * Env to the null environment (empty stack) and return null.
     * @param name the name to search for.
     * @return the corresponding value, or null.
     */
    public Expr search(AttrName name) {
        while (next != null) {
            Expr result = next.recEx.lookup(name);
            if (result != null) {
                return result;
            }
            next = next.next;
        }
        return null;
    } // Env.search(AttrName)

    /** String representation, for debugging.
     * @return a String representation of the Env.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer("Env{");
        String sep = "";
        for (Env p = next; p != null; p = p.next) {
            String s = p.recEx.toString();
            sb.append(sep);
            sep = "|";
            if (s.length() > 20) {
                sb.append(s.substring(0,20)).append("...");
            } else {
                sb.append(s);
            }
        }
        return sb.append('}').toString();
    } // Env.toString()
} // class Env
