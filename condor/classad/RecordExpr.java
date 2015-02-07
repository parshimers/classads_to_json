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

/**
 * A "record" expression (also known as a "classad").
 * An internal (non-leaf) node of an expression tree representing a classified
 * ad (a.k.a. property list, record, table, map, etc.)
 * A record is logically a mapping from case-independent strings (AttrNames)
 * to expressions (Exprs).  It keeps track of the original strings (with case
 * preserved) and the order in which they were added so that the "original"
 * record can be printed.
 * @see Expr
 * @author <a href="mailto:solomon@cs.wisc.edu">Marvin Solomon</a>
 * @version 2.2
 */
public class RecordExpr extends Expr {
    private static String VERSION = "$Id: RecordExpr.java,v 1.29 2005/05/06 20:54:07 solomon Exp $";

    /** A mapping from AttrName attribute names to Expr values.  */
    private Map map;

    /** A list of all the attribute names.  These are the keys of the map
     * (objects of class AttrName) in the order they were first inserted.  For
     * the purposes of evaluation, a RecordExpression is treated as an
     * unordered set of (name, value) mappings, but for purposes of display,
     * the attributes should be shown in the order they were inserted.
     */
    private List attrNames = new LinkedList();

    /** Default constructor: a record with no fields. */
    public RecordExpr() {
        super(RECORD);
        map = new HashMap();
    } // RecordExpr()

    /** Creates a RecordExpr with a specified capacity.
     * @param n the estimated number of attributes to be inserted into
     * this RecordExpr (for efficiency only).
     */
    public RecordExpr(int n) {
        super(RECORD);
        map = new HashMap(n,1);
    } // RecordExpr(List)

    /** The type of the expression.
     * @return the string "classad".
     */
    protected String typeName() {
        return "classad";
    } // typeName()

    /** Find the attribute with the given name in this RecordExpr.
     * @param name the attribute name to look for.
     * @return the definition of "name", or null if there is none.
     */
    public Expr lookup(String name) {
        return (Expr) map.get(AttrName.fromString(name));
    } // lookup(String)

    /** Find the attribute with the given name in this RecordExpr or
     * one of its ancestors.  The parameter "env" contains the list of
     * RecordExprs containing this RecordExpression, innermost first.
     * On a successful search, "env" is updated to contain RecordExprs
     * that (properly) contain the resulting Expr.  If "name" is not found,
     * "env" is reset to the empty environment.
     * @param name the attribute nae to look for.
     * @param env the environment to search and update if this RecordExpr does
     *            not directly defined "name".
     * @return the definition of "name", or null if there is none.
     */
    public Expr lookup(AttrName name, Env env) {
        Expr res = (Expr) map.get(name);
        if (res == null) {
            res = env.search(name);
        }
        return res;
    } // lookup(String,Env)

    /** Find the attribute with the given name.
     * @param name the attribute name to look for.
     * @return the value of the attribute (null if not present).
     */
    public Expr lookup(AttrName name) {
        return (Expr) map.get(name);
    } // lookup(AttrName)

    /** Add an attribute (replacing previous value if any).
     * @param name the name of the attribute to be added.
     * @param expr the value of the attribute.
     * @return this RecordExpr.
     * @throws IllegalArgumentException if name or expr is null.
     */
    public RecordExpr insertAttribute(String name, Expr expr) {
        return insertAttribute(AttrName.fromString(name), expr);
    } // insertAttribute(String,Expr)

    /** Add an attribute (replacing previous value if any).
     * @param key the name of the attribute to be added.
     * @param expr the value of the attribute.
     * @return this RecordExpr.
     * @throws IllegalArgumentException if name or expr is null or name is a
     *                                  reserved word.
     */
    public RecordExpr insertAttribute(AttrName key, Expr expr) {
        if (key == null) {
            throw new IllegalArgumentException(
                        "null attribute name " + key);
        }
        if (key.equals(AttrName.PARENT)) {
            throw new IllegalArgumentException(
                "attribute name \"parent\" is reserved");
        }
        if (expr == null) {
            throw new IllegalArgumentException(
                        "null value for attribute " + key);
        }
        if (!map.containsKey(key)) {
            attrNames.add(key);
        }
        map.put(key, expr);
        return this;
    } // insertAttribute(AttrName,Expr)

    /** Delete an attribute.  The attribute <code>attr</code> is removed from
     * this RecordExpr.  
     * @param attr the attribute to be removed.
     * @return the value of the attribute if any.  Returns null if no matching
     * attribute was in the ad.
     */
    public Expr removeAttribute(AttrName attr) {
        attrNames.remove(attr);
        return (Expr) map.remove(attr);
    } // removeAttribute(AttrName)

    /** Count how many attributes.
     * @return the number of distinct attributes in this ClassAd.
     */
    public int size() {
        return map.size();
    } // size()

    /** Enumerate the attribute names.
     * @return an iterator of objects of type AttrName, representing the
     *    the attribute names.
     */
    public Iterator attributes() {
        return attrNames.iterator();
    } // attributes()

    /** Evaluate this Expr.  This is the internal method used to implement
     * {@link Expr#eval()}.
     * A Record evaluates to itself.
     * @param env ignored.
     * @return this RecordExpr.
     */
    protected Expr eval1(Env env) {
        return this;
    } // eval1(Env)

    /** Convert this Expr to a string, appending the result to the end of "sb".
     * The representation is the "canonical native format":
     * <code>'[' [ name '=' expr [ ';' name '=' expr ]* ] ']'</code> with no
     * extra spaces.
     * Each "name" is represented as by {@link AttrRef#toString()}.
     * @param sb a place to put the result.
     * @return sb.
     * @see ClassAdWriter
     */
    public StringBuffer toString(StringBuffer sb) {
        char sep = '[';
        for (Iterator keys = attrNames.iterator(); keys.hasNext();) {
            AttrName name = (AttrName) keys.next();
            sb.append(sep)
                .append(name)
                .append('=');
            lookup(name).toString(sb);
            sep = ';';
        }
        if (sep == '[') {
            sb.append(sep);
        }
        return sb.append(']');
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
        RecordExpr o = (RecordExpr) other;
        if (attrNames.size() != o.attrNames.size()) {
            return false;
        }
        Object[] keys1 = map.keySet().toArray();
        Object[] keys2 = o.map.keySet().toArray();
        if (keys1.length != keys2.length) {
            return false;
        }
        for (int i = 0; i < keys1.length; i++) {
            Object k1 = keys1[i];
            Object k2 = keys2[i];
            if (!k1.equals(k2)) {
                return false;
            }
            Expr e1 = (Expr) map.get(k1);
            Expr e2 = (Expr) o.map.get(k2);
            if (!e1.sameAs(e2)) {
                return false;
            }
        }
        return true;
    } // sameAs(Expr)

    /** Serialize this RecordExpr in the format currently expected by Condor.
     * <ul>
     *    <li> an integer n
     *    <li> n+2 strings representing all attributes.  All but the last two
     *         have the form "name = value", where value is the value of the
     *         attribute converted to a string by toString (currently,
     *         toString(COMPACT)).  The last two are the toString(BRIEF)
     *         verisons of attributes MyType and TargetType, in that order,
     *         which must be present in the ad.
     * </ul>
     * The integer n is serialized by the writeInt method of the output stream
     * and the n+2 strings are serialized by the writeUTF method.  Normally,
     * The output stream will be an instance of condor.cedar.CedarOutputStream,
     * which means that n is written as 8 bytes, most significant byte first,
     * and the strings are null-terminated and encoded using Java UTF-8 (which
     * just transmits the low 8 bits of each character provided the string
     * contains only ASCII characters).
     * <p>
     * To reconstitute a classad from this format, use a {@link ClassAdReader}
     * and a {@link ClassAdParser}.
     * @param out the stream to which the serialzed data should be written.
     * @exception InvalidObjectException if this add does not have MyType and
     * TargetType attributes.
     * @exception IOException if an I/O error occurs.
     * @see ClassAdParser#parse()
     * @see ClassAdReader
     */
    public void transmit(DataOutput out) throws IOException {
        Expr expr = lookup("MyType");
        if (expr == null) {
            throw new InvalidObjectException("no MyType attribute");
        }
        String myType = null;
        try {
            myType = ((Constant) expr).stringValue();
        } catch (Exception e) {
            throw new InvalidObjectException("bad MyType attribute: " + e);
        }
        expr = lookup("TargetType");
        if (expr == null) {
            throw new InvalidObjectException("no TargetType attribute");
        }
        String targetType = null;
        try {
            targetType = ((Constant) expr).stringValue();
        } catch (Exception e) {
            throw new InvalidObjectException("bad TargetType attribute: " + e);
        }
        out.writeInt(map.size() - 2);
        for (Iterator keys = attrNames.iterator(); keys.hasNext(); ) {
            String name = keys.next().toString();
            if (name.equals("MyType") || name.equals("TargetType")) {
                continue;
            }
            Expr value = lookup(name);
            out.writeUTF(name + "=" + value.toString());
        }
        out.writeUTF(myType);
        out.writeUTF(targetType);
    } // transmit(DataOutput)

    /** The precedence of the operator in this expression node.
     * Used to print expressions without superfluous parentheses.
     * @return the precendence of this node.
     * @see ClassAdWriter#MINIMAL_PARENTHESES
     */
    public int prec() {
        return MAXPREC;
    } // prec()
} // RecordExpr
