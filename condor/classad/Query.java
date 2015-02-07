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

/** A parsed "query" in a very simple ad hoc query language inspired by SQL.
 * This is a hack desiged to test the ClassAd machinery by sending a query
 * to Condor Collector and interpreting the results.
 * A query has the form
 * <pre>
 *     Query: 'select' SelectClause 'from' Identifier WhereClause
 *     SelectClause: '*' | SelectionList
 *     SelectionList: [ SelectionList ',' ] Identifier [ '=' Expression ]
 *     WhereClause : [ 'where' Expression ]
 * </pre>
 * @author <a href="mailto:solomon@cs.wisc.edu">Marvin Solomon</a>
 * @version 2.2
 */
public class Query {
    private static String VERSION = "$Id: Query.java,v 1.11 2005/05/06 20:54:07 solomon Exp $";

    /** The expressions in the SelectionClause.
     * If the clause was "*", this field is null. Otherwise, it has one
     * element for each "Identifier = Expression" or "Identifier" in the
     * SelectionList.  The element is the Expression if present; otherwise,
     * it is AttrRef expression corresponding to the Identifier.
     */
    public final Expr[] selectClause;

    /** The Identifiers in the SelectionClause.
     * If the clause was "*", this field is null. Otherwise, it has one
     * element for each "Identifier = Expression" or "Identifier" in the
     * SelectionList.  If the Expression is present, the element is the
     * original Identifier (with case preserved).  Otherwise, it is null.
     */
    public final String[] selectNames;

    /** The "from" clause (original case preserved). */
    public String fromClause;

    /** The "where" clause or null if the "where" clause was omitted.  */
    public Expr whereClause;

    /** Keyword for making case-independent comparisons */
    private static final AttrName SELECT = AttrName.fromString("select");

    /** Keyword for making case-independent comparisons */
    private static final AttrName FROM = AttrName.fromString("from");

    /** Keyword for making case-independent comparisons */
    private static final AttrName WHERE = AttrName.fromString("where");

    /** Construct a query from its components.
     * @param s the "select" clause expressions.
     * @param n the "select" clause identifiers.
     * @param f the "from" clause.
     * @param w the "where" clause (or null).
     */
    public Query(Expr[] s, String[] n, String f, Expr w) {
        // A little santity checking
        boolean ok = f != null && f.length() > 0;
        if (s == null) {
            ok = ok && n == null;
        } else {
            ok = ok && n != null && s.length == n.length;
        }
        if (!ok) {
            throw new IllegalArgumentException();
        }
        selectClause = s;
        selectNames = n;
        fromClause = f;
        whereClause = w;
    } // Query(Expr[], String[], String, Expr)

    /** Create a Query by parsing input from an InputStream.
     * @param s the input source.
     * @return a new Query object.
     */
    public static Query parse(InputStream s) {
        return parse(new ClassAdParser(s));
    } // parse(InputStream)

    /** Create a Query by parsing input from a Reader.
     * @param s input source.
     * @return a new Query object.
     */
    public static Query parse(Reader s) {
        return parse(new ClassAdParser(s));
    } // parse(Reader)

    /** Create a Query by parsing a String.
     * @param s the input source.
     * @return a new Query object.
     */
    public static Query parse(String s) {
        return parse(new ClassAdParser(s));
    } // parse(Reader)

    /** Create a Query from an exsiting parser.
     * @param parser the input source.
     * @return a new Query object.
     */
    public static Query parse(ClassAdParser parser) {
        List selectExprs = null;
        List selectIds = null;
        String from;
        Expr where;

        Object v = parser.nextValue();
        if (v == null || !v.equals(SELECT)) {
            parser.printMessage("query must start with \"select\"");
            return null;
        }
        parser.getNextToken();
        if (parser.nextToken() == '*') {
            parser.getNextToken();
        } else {
            // Should be id [ '=' expr ] ( ',' id [ '=' expr ])*
            // followed by "from".
            selectExprs = new LinkedList();
            selectIds = new LinkedList();
            for (;;) {
                if (parser.nextToken() != Parser.IDENTIFIER) {
                    parser.printMessage("invalid \"select\" clause in query");
                    return null;
                }
                AttrName id = (AttrName) parser.nextValue();
                if (id.equals(FROM)) {
                    break;
                }
                parser.getNextToken();
                if (parser.nextToken() == '=') {
                    parser.getNextToken();
                    Expr expr = parser.parse();
                    if (expr == null) {
                        parser.printMessage(
                            "invalid \"select\" clause in query");
                        return null;
                    }
                    selectIds.add(id.rawString());
                    selectExprs.add(expr);
                } else {
                    selectIds.add(null);
                    selectExprs.add(new AttrRef(id));
                }
                if (parser.nextToken() == ',') {
                    parser.getNextToken();
                }
            }
        } // SelectClause != '*'

        // "from" clause
        if (parser.nextToken() != Parser.IDENTIFIER
            || !(parser.nextValue().equals(FROM)))
        {
            parser.printMessage("missing \"from\" clause in query");
            return null;
        }
        parser.getNextToken();
        if (parser.nextToken() != Parser.IDENTIFIER) {
            parser.printMessage("invalid \"from\" clause in query");
            return null;
        }
        from = ((AttrName) parser.nextValue()).rawString();
        parser.getNextToken();

        // "where" clause
        if (parser.nextToken() == 0) {
            where = null;
        } else {
            if (parser.nextToken() != Parser.IDENTIFIER
                || !(parser.nextValue().equals(WHERE)))
            {
                parser.printMessage("invalid \"where\" clause in query");
                return null;
            }
            parser.getNextToken();
            where = parser.parse();
            if (where == null) {
                parser.printMessage("invalid \"where\" clause in query");
                return null;
            }
        }
        Expr[] exprs;
        String[] ids;
        if (selectExprs == null) {
            exprs = null;
            ids = null;
        } else {
            int n = selectExprs.size();
            Iterator ei = selectExprs.iterator();
            Iterator ii = selectIds.iterator();
            exprs = new Expr[n];
            ids = new String[n];
            for (int i = 0; i < n; i++) {
                exprs[i] = (Expr) ei.next();
                ids[i] = (String) ii.next();
            }
        }
        return new Query(exprs, ids, from, where);
    } // parse(ClassAdParser)

    public String toString() {
        StringBuffer result;
        if (selectClause == null) {
            result = new StringBuffer("select *");
        } else {
            result = new StringBuffer("select ");
            for (int i = 0; i < selectClause.length; i++) {
                if (i > 0) {
                    result.append(", ");
                }
                if (selectNames[i] != null) {
                    result.append(selectNames[i]).append(" = ");
                }
                result.append(selectClause[i]);
            }
        }
        result.append("\nfrom ").append(fromClause);
        if (whereClause != null) {
            result.append("\nwhere ").append(whereClause);
        }
        return result.toString();
    } // toString()
} // Query
