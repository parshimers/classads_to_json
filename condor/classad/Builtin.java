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
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

/**
 * A library of builtin functions.
 * All functions are static and public and match the signature restrictions
 * documented by {@link ClassAd#loadJavaLibrary(java.lang.String)
                  ClassAd.loadJavaLibrary(String)}.
 * Functions that violate these restrictions would be silently ignored.
 * @see FuncCall
 * @see ClassAd#loadJavaLibrary(java.lang.String)
 * @author <a href="mailto:solomon@cs.wisc.edu">Marvin Solomon</a>
 * @version 2.2
 */
public class Builtin {
    private static String VERSION = "$Id: Builtin.java,v 1.16 2005/05/06 20:54:06 solomon Exp $";


    /** The default constructor is made private to prevent anybody from trying
     * to create an instance of this class.
     */
    private Builtin() {
        throw new RuntimeException();
    } // Builtin()

    /** A generator of random numbers to be used by random(). */
    private static Random rand = new Random();

    //============= type queries (see also data conversion operations below) ==

    /** Tests whether "expr" evaluates to undefined.
     * @param env environment for evaluating "expr"
     * @param expr the expression to test
     * @return TRUE if "expr" evaluates to undefined
     */
    public static Expr isUndefined(Env env, Expr expr) {
        if (!expr.isConstant()) {
            expr = expr.eval(env);
        }
        return Constant.bool(expr.type == Expr.UNDEFINED);
    } // isUndefined

    /** Tests whether "expr" evaluates to an error.
     * @param env environment for evaluating "expr"
     * @param expr the expression to test
     * @return TRUE if "expr" evaluates to an error
     */
    public static Expr isError(Env env, Expr expr) {
        if (!expr.isConstant()) {
            expr = expr.eval(env);
        }
        return Constant.bool(expr.type == Expr.ERROR);
    } // isError

    /** Tests whether "expr" evaluates to a string.
     * @param env environment for evaluating "expr"
     * @param expr the expression to test
     * @return TRUE if "expr" evaluates to a string
     */
    public static Expr isString(Env env, Expr expr) {
        if (!expr.isConstant()) {
            expr = expr.eval(env);
        }
        return Constant.bool(expr.type == Expr.STRING);
    } // isString

    /** Tests whether "expr" evaluates to an integer.
     * @param env environment for evaluating "expr"
     * @param expr the expression to test
     * @return TRUE if "expr" evaluates to an integer
     */
    public static Expr isInteger(Env env, Expr expr) {
        if (!expr.isConstant()) {
            expr = expr.eval(env);
        }
        return Constant.bool(expr.type == Expr.INTEGER);
    } // isInteger

    /** Tests whether "expr" evaluates to a real.
     * @param env environment for evaluating "expr"
     * @param expr the expression to test
     * @return TRUE if "expr" evaluates to a real
     */
    public static Expr isReal(Env env, Expr expr) {
        if (!expr.isConstant()) {
            expr = expr.eval(env);
        }
        return Constant.bool(expr.type == Expr.REAL);
    } // isReal

    /** Tests whether "expr" evaluates to a list.
     * @param env environment for evaluating "expr"
     * @param expr the expression to test
     * @return TRUE if "expr" evaluates to a list
     */
    public static Expr isList(Env env, Expr expr) {
        if (!expr.isConstant()) {
            expr = expr.eval(env);
        }
        return Constant.bool(expr.type == Expr.LIST);
    } // isList

    /** Tests whether "expr" evaluates to a record expression.
     * @param env environment for evaluating "expr"
     * @param expr the expression to test
     * @return TRUE if "expr" evaluates to a record expression
     */
    public static Expr isClassad(Env env, Expr expr) {
        if (!expr.isConstant()) {
            expr = expr.eval(env);
        }
        return Constant.bool(expr.type == Expr.RECORD);
    } // isClassad

    /** Tests whether "expr" evaluates to a boolean (TRUE or FALSE).
     * @param env environment for evaluating "expr"
     * @param expr the expression to test
     * @return TRUE if "expr" evaluates to a boolean
     */
    public static Expr isBoolean(Env env, Expr expr) {
        if (!expr.isConstant()) {
            expr = expr.eval(env);
        }
        return Constant.bool(expr.type == Expr.BOOLEAN);
    } // isBoolean

    /** Tests whether "expr" evaluates to an absTime constant.
     * @param env environment for evaluating "expr"
     * @param expr the expression to test
     * @return TRUE if "expr" evaluates to an absTime constant
     */
    public static Expr isAbstime(Env env, Expr expr) {
        if (!expr.isConstant()) {
            expr = expr.eval(env);
        }
        return Constant.bool(expr.type == Expr.ABSOLUTE_TIME);
    } // isAbstime

    /** Tests whether "expr" evaluates to a relTime constant.
     * @param env environment for evaluating "expr"
     * @param expr the expression to test
     * @return TRUE if "expr" evaluates to a relTime constant
     */
    public static Expr isReltime(Env env, Expr expr) {
        if (!expr.isConstant()) {
            expr = expr.eval(env);
        }
        return Constant.bool(expr.type == Expr.RELATIVE_TIME);
    } // isReltime

    //============= data conversion operations ===============================

    /** Convert to integer.  The arg is convered to an integer if possible.
     * Note: This method corresponds to int() in the ClassAd language, because
     * case is ignored in function names.  The name "int" cannot be used
     * because it is a reserved word in Java.
     * @param arg the value to be converted.
     * @return an integer constant corresponding to arg, or ERROR.
     */
    public static Expr Int(Expr arg) {
        switch (arg.type) {
        case Expr.INTEGER:
            return arg;
        case Expr.REAL:
            return Constant.getInstance((int)arg.realValue());
        case Expr.BOOLEAN:
            return Constant.getInstance(arg.isTrue() ? 1 : 0);
        case Expr.ABSOLUTE_TIME:
        case Expr.RELATIVE_TIME:
            long ms = ((Constant) arg).milliseconds();
            int secs = (int) (ms / 1000);
            return Constant.getInstance(secs);
        case Expr.STRING:
            String s = arg.stringValue();
            try {
                return Constant.getInstance((int) Constant.stringToDouble(s));
            } catch (NumberFormatException e) {
                return Constant.error("ill-formed integer " + s);
            }
        default:
            return Constant.error(
                "int(" + arg.typeName() + " " + arg.type + ")");
        }
    } // Int(Expr)

    /** Convert to real.  The arg is convered to a real if possible.
     * @param arg the value to be converted.
     * @return a real constant corresponding to arg, or ERROR.
     */
    public static Expr real(Expr arg) {
        if (arg.type == Expr.REAL) {
            return arg;
        }
        try {
            return Constant.getInstance(toReal(arg));
        } catch (NumberFormatException e) {
            return Constant.error("ill-formed real " + arg.stringValue());
        } catch (ClassCastException e) {
            return Constant.error("real(" + arg.typeName() + ")");
        }
    } // real(Expr)

    /** Converts the argument to a string.
     * @param s str the string to convert.
     * @return a string represenation of the Expr
     */
    public static Expr string(Expr s) {
        return s.type == Expr.STRING ? s : Constant.getInstance(getString(s));
    } // string(Expr)

    // See also absTime and relTime below

    //============= numeric operations =======================================

    /** Returns an integer constant corresponding to the floor of the value
     * of arg.
     * @param arg the input value.
     * @return the floor of arg.
     */
    public static Expr floor(Expr arg) {
        if (arg.type == Expr.INTEGER) {
            return arg;
        }
        try {
            return Constant.getInstance((int)Math.floor(toReal(arg)));
        } catch (NumberFormatException e) {
            return Constant.error("floor(invalid number " + arg + ")");
        } catch (ClassCastException e) {
            return Constant.error("floor(" + arg.typeName() + ")");
        }
    } // floor(Expr)

    /** Returns an integer constant corresponding to the ceiling of the value
     * of arg.
     * @param arg the input value.
     * @return the ceiling of arg.
     */
    public static Expr ceiling(Expr arg) {
        if (arg.type == Expr.INTEGER) {
            return arg;
        }
        try {
            return Constant.getInstance((int)Math.ceil(toReal(arg)));
        } catch (NumberFormatException e) {
            return Constant.error("ceiling(invalid number " + arg + ")");
        } catch (ClassCastException e) {
            return Constant.error("ceiling(" + arg.typeName() + ")");
        }
    } // ceiling(Expr)

    /** Returns an integer constant corresponding to the value of arg rounded
     * to the nearest integer.
     * @param arg the input value.
     * @return the rounded arg.
     */
    public static Expr round(Expr arg) {
        if (arg.type == Expr.INTEGER) {
            return arg;
        }
        try {
            return Constant.getInstance((int)Math.round(toReal(arg)));
        } catch (NumberFormatException e) {
            return Constant.error("round(invalid number " + arg + ")");
        } catch (ClassCastException e) {
            return Constant.error("round(" + arg.typeName() + ")");
        }
    } // round(Expr)

    /** Returns a random value.
     * The value r is uniformly destributed over the range 0 &lt;= r &lt; x,
     * where x is the argument.  The result is an Integer if x is an Integer,
     * and a Real if x is a real.  An omitted argument is treated like 1.0.
     * Returns ERROR if the argument is not a positive Intger or positive Real.
     * @param args the arguments
     * @return a random value.
     */
    public static Expr random(Expr[] args) {
        if (args.length == 0) {
            return Constant.getInstance(rand.nextDouble());
        }
        if (args.length != 1) {
            return Constant.error("too many arguments to random()");
        }
        switch (args[0].type) {
        case Expr.INTEGER: {
            int n = args[0].intValue();
            return n > 0
                ? Constant.getInstance(rand.nextInt(n))
                : Constant.error("non-positive argument to random()");
        }
        case Expr.REAL:
            double x = args[0].realValue();
            return x > 0
                ? Constant.getInstance(rand.nextDouble() * x)
                : Constant.error("non-positive argument to random()");
        default:
            return Constant.error("invalid argument random(" + args[0] + ")");
        }
    } // random

    //============= string operations ========================================
        
    /** Converts each argument to a string and returns the concatenation
     * of the values.
     * @param args the arguments
     * @return the concatenation of the values.
     */
    public static Expr strcat(Expr[] args) {
        String res = "";
        for (int i = 0; i < args.length; i++) {
            res += getString(args[i]);
        }
        return Constant.getInstance(res);
    } // strcat(Expr[])

    /** Returns a substring of a string.
     * This is like the Perl version of substr.
     * Offset is the start of the substr, negative is from the end:
     * 0: first char of string
     * -1: last char of string
     * If length is omitted, it means the rest of the string.
     * If length is negative, that many characters are dropped.
     * @param args the arguments
     * @return the designated substring
     */
    public static Expr substr(Expr[] args) {
        if (args.length < 2 || args.length > 3) {
            return Constant.error("wrong number of args to substr");
        }
        if (args[0].type != Expr.STRING) {
            return Constant.error("substr: arg 1 must be string, not "
                                  + args[0].typeName());
        }
        String str = args[0].stringValue();

        // offset
        if (!(args[1].type == Expr.INTEGER)) {
            return Constant.error("substr: arg 2 must be int, not "
                                  + args[1].typeName());
        }
        int offset = args[1].intValue();
        // negative offset is from end of original string
        int strlen = str.length();
        if (offset < 0) {
            offset += strlen;
        }
        if (offset > strlen) {
            return Constant.getInstance("");
        }
        if (offset < 0) {
            offset = 0;
        }

        // length
        if (args.length == 3) {
            if (!(args[2].type == Expr.INTEGER)) {
                return Constant.error(
                    "substr: arg 3 must be int, not "
                    + args[2].typeName());
            }
            int len = args[2].intValue();
            // negative length means bytes to drop
            if (len < 0) {
                len += strlen - offset;
            }

            return
                Constant.getInstance(
                    len + offset > strlen
                        ? str.substring(offset)
                        : str.substring(offset,len+offset));
        } else return Constant.getInstance(str.substring(offset));
    } // substr(Expr[])

    /** Compares two strings.  The arguments are first converted to strings
     * as if by string().
     * @param s1 the first string.
     * @param s2 the second string.
     * @return an Integer expression with a value  that is less than zero if
     *         string(s1) &lt; string(s2), greater than zero if string(s1) &gt;
     *         string(s2), and equal to zero if string(s1) &eq; string(s2).
     */
    public static Expr strcmp(Expr s1, Expr s2) {
        return Constant.getInstance(getString(s1).compareTo(getString(s2)));
    } // strcmp(Expr, Expr)

    /** Compares two strings ignoring differences in case.  The arguments are
     * first converted to strings as if by string().
     * @param s1 the first string.
     * @param s2 the second string.
     * @return an Integer expression with a value  that is less than zero if
     *         string(s1) &lt; string(s2), greater than zero if string(s1) &gt;
     *         string(s2), and equal to zero if string(s1) &eq; string(s2).
     */
    public static Expr stricmp(Expr s1, Expr s2) {
        return Constant.getInstance(getString(s1)
                    .compareToIgnoreCase(getString(s2)));
    } // stricmp(Expr, Expr)

    /** Converts a string to upper case.
     * The argument is converted to a string as if by string().  The
     * result is a copy of the string in which all lower case letters are
     * converted to upper case.
     * @param expr the expression to be converted
     * @return an upper case string representing "expr".
     */
    public static Expr toUpper(Expr expr) {
        return Constant.getInstance(getString(expr).toUpperCase());
    } // toUpper

    /** Converts a string to lower case.
     * The argument is converted to a string as if by string().  The
     * result is a copy of the string in which all upper case letters are
     * converted to lower case.
     * @param expr the expression to be converted
     * @return a lower case string representing "expr".
     */
    public static Expr toLower(Expr expr) {
        return Constant.getInstance(getString(expr).toLowerCase());
    } // toLower

    // See also string() above.

    //============= misc operations ===========================================

    /** Returns the size of an object.
     * The size of a String is the number of characters.
     * The size of a List or Record is the number of elements.
     * For any other type of object, the result is ERROR.
     * @param obj the object to be tested.
     * @return and Integer containing its size, or ERROR.
     */
    public static Expr size(Expr obj) {
        switch (obj.type) {
        case Expr.STRING:
            return Constant.getInstance(getString(obj).length());
        case Expr.LIST:
            return Constant.getInstance(((ListExpr) obj).size());
        case Expr.RECORD:
            return Constant.getInstance(((RecordExpr) obj).size());
        default:
            return Constant.error("invalid argument to size: " + obj);
        }
    } // size

    /** Computes the sum of a list of numbers.
     * The elements of l are evaluated, producing a list l' of values.  If l'
     * is composed only of numbers, the result is the sum of the values, as a
     * Real if any value is Real, and as an Integer otherwise.  If the list is
     * empty, the result is 0. In other cases, the result is ERROR.
     * @param l a list of expressions.
     * @return the sum of the values.
     */
    public static Expr sum(Expr l) {
        if (l.type != Expr.LIST) {
            return Constant.error("invalid argument to sum: " + l);
        }
        ListExpr le = (ListExpr) l;
        int ival = 0;
        double rval = 0;
        boolean anyReal = false;
        for (Iterator i = le.iterator(); i.hasNext(); ) {
            Expr e = ((Expr) i.next()).eval();
            switch (e.type) {
            case Expr.INTEGER:
                ival += e.intValue();
                break;
            case Expr.REAL:
                rval += e.realValue();
                anyReal = true;
                break;
            default:
                return Constant.error("non-number in sum of list: " + e);
            }
        }
        return
            anyReal
                ? Constant.getInstance(ival + rval)
                : Constant.getInstance(ival);
    } /* sum */

    /** Computes the average of a list of numbers.
     * The elements of l are evaluated, producing a list l' of values.  If l'
     * is composed only of numbers, the result is the average of the values, as
     * a Real.  If the list is empty, the result is 0.0. In other cases, the
     * result is ERROR.
     * @param l a list of expressions.
     * @return the average of the values as a Real.
     */
    public static Expr avg(Expr l) {
        if (l.type != Expr.LIST) {
            return Constant.error("invalid argument to avg: " + l);
        }
        ListExpr le = (ListExpr) l;
        if (le.size() == 0) {
            return Constant.getInstance(0.0);
        }
        double sum = 0;
        for (Iterator i = le.iterator(); i.hasNext(); ) {
            Expr e = ((Expr) i.next()).eval();
            switch (e.type) {
            case Expr.INTEGER:
                sum += e.intValue();
                break;
            case Expr.REAL:
                sum += e.realValue();
                break;
            default:
                return Constant.error("non-number in avg of list: " + e);
            }
        }
        return Constant.getInstance(sum / le.size());
    } /* avg */

    /** Computes the minimum of a list of numbers.
     * The elements of l are evaluated, producing a list l' of values.  If l'
     * is composed only of numbers, the result is the minimum of the values, as
     * a Real if any value is Real, and as an Integer otherwise.  If the list
     * is empty, the result is UNDEFINED. In other cases, the result is ERROR.
     * @param l a list of expressions.
     * @return the minimum of the values.
     */
    public static Expr min(Expr l) {
        if (l.type != Expr.LIST) {
            return Constant.error("invalid argument to min: " + l);
        }
        ListExpr le = (ListExpr) l;
        if (le.size() == 0) {
            return Constant.undefined("min of empty list");
        }
        int ival = Integer.MAX_VALUE;
        double rval = Double.MAX_VALUE;
        boolean anyReal = false;
        for (Iterator i = le.iterator(); i.hasNext(); ) {
            Expr e = ((Expr) i.next()).eval();
            switch (e.type) {
            case Expr.INTEGER:
                ival = Math.min(ival, e.intValue());
                break;
            case Expr.REAL:
                rval = Math.min(rval, e.realValue());
                break;
            default:
                return Constant.error("non-number in min of list: " + e);
            }
        }
        return
            anyReal
                ? Constant.getInstance(Math.min(rval, ival))
                : Constant.getInstance(ival);
    } /* min */

    /** Computes the maximum of a list of numbers.
     * The elements of l are evaluated, producing a list l' of values.  If l'
     * is composed only of numbers, the result is the maximum of the values, as
     * a Real if any value is Real, and as an Integer otherwise.  If the list
     * is empty, the result is UNDEFINED. In other cases, the result is ERROR.
     * @param l a list of expressions.
     * @return the maximum of the values.
     */
    public static Expr max(Expr l) {
        if (l.type != Expr.LIST) {
            return Constant.error("invalid argument to max: " + l);
        }
        ListExpr le = (ListExpr) l;
        if (le.size() == 0) {
            return Constant.undefined("max of empty list");
        }
        int ival = Integer.MIN_VALUE;
        double rval = Double.MIN_VALUE;
        boolean anyReal = false;
        for (Iterator i = le.iterator(); i.hasNext(); ) {
            Expr e = ((Expr) i.next()).eval();
            switch (e.type) {
            case Expr.INTEGER:
                ival = Math.max(ival, e.intValue());
                break;
            case Expr.REAL:
                rval = Math.max(rval, e.realValue());
                break;
            default:
                return Constant.error("non-number in max of list: " + e);
            }
        }
        return
            anyReal
                ? Constant.getInstance(Math.max(rval, ival))
                : Constant.getInstance(ival);
    } /* max */

    /** Checks whether "list" is a ListExpr containing a member that
     * is equal to "expr" in the sense of "==".
     * If "expr" is not a constant or "list" is not a list, then the result is
     * an error.  Otherwise, the elements of "list" are evaluated and if any of
     * the values are equal to "expr" in the sense of the "==" operator,
     * then the result is true, otherwise it is false.
     * @param env an environment for evaluating "expr" and the members of "list"
     * @param expr the expression to search for
     * @param list the list to search for "expr"
     * @return a boolean constant or ERROR.
     */
    public static Expr member(Env env, Expr expr, Expr list) {
        expr = expr.eval(env);
        list = list.eval(env);
        if (!(list.type == Expr.LIST)) {
            return Constant.error("member: arg 2 must be list, not "
                                  + list.typeName());
        }
        if (!(expr instanceof Constant)) {
            return Constant.error("member: arg 1 must be constant, not "
                                  + list.typeName());
        }
        Constant e = (Constant) expr;
        ListExpr l = (ListExpr) list;
        for (Iterator i = l.iterator(); i.hasNext(); ) {
            try {
                Expr x = (Expr) i.next();
                if (e.equals(x.eval(env))) {
                    return Constant.TRUE;
                }
            } catch (ArithmeticException ignore) {
                // Treat this like "false"
                continue;
            }
        }
        return Constant.FALSE;
    } // member(Expr[])

    /** Checks whether "list" is a ListExpr containing a member that
     * is equal to "expr" in the sense of "IS".
     * If "expr" is not a constant or "list" is not a list, then the result is
     * an error.  Otherwise, the elements of "list" are evaluated and if any of
     * the values are equal to "expr" in the sense of the "IS" operator,
     * then the result is true, otherwise it is false.
     * @param env an environment for evaluating "expr" and the members of "list"
     * @param expr the expression to search for
     * @param list the list to search for "expr"
     * @return a boolean constant or ERROR.
     */
    public static Expr identicalMember(Env env, Expr expr, Expr list) {
        expr = expr.eval(env);
        list = list.eval(env);
        if (!(list.type == Expr.LIST)) {
            return Constant.error("identicalMember: arg 2 must be list, not "
                                  + list.typeName());
        }
        if (!(expr instanceof Constant)) {
            return Constant.error(
                "identicalMember: arg 1 must be constant, not "
                                  + list.typeName());
        }
        ListExpr l = (ListExpr) list;
        for (Iterator i = l.iterator(); i.hasNext(); ) {
            try {
                Expr x = (Expr) i.next();
                if (expr.is(x.eval(env))) {
                    return Constant.TRUE;
                }
            } catch (ArithmeticException ignore) {
                // Treat this like "false"
                continue;
            }
        }
        return Constant.FALSE;
    } // identicalMember(Expr[])

    /** Checks whether "list" is a ListExpr containing a member that
     * matches "expr" in the sense of "regexp".
     * If "expr" is not a constant or "list" is not a list, then the result is
     * an error.  Otherwise, the elements of "list" are evaluated and if any of
     * them evaluates to anything other than a String, the result is an error.
     * Otherwise, if any of values in the list matches the pattern according to
     * the "regexp" function, the result is TRUE.   If there is no match, then
     * the result is FALSE.
     * @param env an environment for evaluating "expr" and the members of "list"
     * @param expr the expression to search for
     * @param list the list to search for "expr"
     * @return a boolean constant or ERROR.
     */
    public static Expr regExpMember(Env env, Expr expr, Expr list) {
        expr = expr.eval(env);
        list = list.eval(env);
        if (list.type != Expr.LIST) {
            return Constant.error("regexpMember: arg 2 must be a list, not "
                                  + list.typeName());
        }
        if (expr.type != Expr.STRING) {
            return Constant.error("regexpMember: arg 1 must be a String, not "
                                  + list.typeName());
        }
        Pattern p = compilePattern(expr.stringValue(), null);
        ListExpr l = (ListExpr) list;
        for (Iterator i = l.iterator(); i.hasNext(); ) {
            Expr e = (Expr) i.next();
            e = e.eval(env);
            if (e.type != Expr.STRING) {
                return Constant.error("regexpMember: non-string in list");
            }
            if (p.matcher(e.stringValue()).find()) {
                return Constant.TRUE;
            }
        }
        return Constant.FALSE;
    } // regExpMember(Expr[])

    /** Helper function for anycompare and allcompare.  Returns the
     * constant Expr.LESS, Expr.SAME, etc. matching the operator "op" ("&lt;",
     * "is"), etc.  Returns -1 if there is no match.
     * @param op an operator token.
     * @return the corresponding constant, or -1.
     */
    private static int opDecode(String op) {
        if (op.equals("<")) return Expr.LESS;
        if (op.equals("<=")) return Expr.LESS_EQ;
        if (op.equals(">")) return Expr.GREATER;
        if (op.equals(">=")) return Expr.GREATER_EQ;
        if (op.equals("==")) return Expr.EQUAL;
        if (op.equals("!=")) return Expr.NOT_EQUAL;
        if (op.equalsIgnoreCase("is")) return Expr.SAME;
        if (op.equalsIgnoreCase("isnt")) return Expr.DIFFERENT;
        return -1;
    } // opDecode

    /** Compares a value to the elements of a list.
     * If $s$ is not a string equal (ignoring case) to one of "&lt;", "&lt;=",
     * "==", "!=", "&gt;", "&gt;=", "is", or "isnt", or "l" is not a list, the
     * result is an error.  Otherwise, the elements of "l" are evaluated and
     * compared to "t" using the ClassAd operator corresponding to "s".  If any
     * of the comparisons evaluate to TRUE the result is TRUE.  Otherwise, the
     * result is FALSE.
     * @param env an environment for evaluating "t" and the members of "l"
     * @param s an operator
     * @param l a list
     * @param t value to compare to members of the list.
     * @return TRUE if the value matches any member of the list.
     */
    public static Expr anycompare(Env env, Expr s, Expr l, Expr t) {
        s = s.eval(env);
        l = l.eval(env);
        t = t.eval(env);
        if (s.type != Expr.STRING) {
            return Constant.error("anycompare: arg 1 must be a string, not "
                                  + s.typeName());
        }
        int op = opDecode(s.stringValue());
        if (op < 0) {
            return Constant.error("anycompare: unrecognized operator " + s);
        }
        if (l.type != Expr.LIST) {
            return Constant.error("anycompare: arg 2 must be a string, not "
                                  + l.typeName());
        }
        ListExpr le = (ListExpr) l;
        for (Iterator i = le.iterator(); i.hasNext(); ) {
            Expr e = (Expr) i.next();
            e = new Op(op, e, t);

            if (e.eval(env) == Constant.TRUE) {
                return Constant.TRUE;
            }
        }
        return Constant.FALSE;
    } /* anycompare */

    /** Compares a value to the elements of a list.
     * If $s$ is not a string equal (ignoring case) to one of "&lt;", "&lt;=",
     * "==", "!=", "&gt;", "&gt;=", "is", or "isnt", or "l" is not a list, the
     * result is an error.  Otherwise, the elements of "l" are evaluated and
     * compared to "t" using the ClassAd operator corresponding to "s".  If all
     * of the comparisons evaluate to TRUE the result is TRUE.  Otherwise, the
     * result is FALSE.
     * @param s an operator
     * @param l a list
     * @param t value to compare to members of the list.
     * @return TRUE if the value matches all members of the list.
     */
    public static Expr allcompare(Expr s, Expr l, Expr t) {
        if (s.type != Expr.STRING) {
            return Constant.error("allcompare: arg 1 must be a string, not "
                                  + s.typeName());
        }
        int op = opDecode(s.stringValue());
        if (op < 0) {
            return Constant.error("allcompare: unrecognized operator " + s);
        }
        if (l.type != Expr.LIST) {
            return Constant.error("allcompare: arg 2 must be a string, not "
                                  + l.typeName());
        }
        ListExpr le = (ListExpr) l;
        for (Iterator i = le.iterator(); i.hasNext(); ) {
            Expr e = (Expr) i.next();
            e = new Op(op, e.eval(), t);
            if (e.eval() != Constant.TRUE) {
                return Constant.FALSE;
            }
        }
        return Constant.TRUE;
    } /* allcompare */

    /** Compares a string with a regular expression.
     * Invoked as regexp(pat, str [, options]).
     * The result is true if pat matches str and false otherwise.
     * The options argument if present may contain the following characters.
     * <dl>
     *   <dt>i or I
     *      <dd>Ignore case.
     *   <dt>m or M
     *      <dd>A carat (^) matches not only the start of the subject string,
     *          but also after each newline.  Similarly, dollar ($) matches
     *          before a newline.
     *   <dt>s or S
     *      <dd>Dot (.) matches any character, including newline.
     *   <dt>x or X
     *      <dd>Use "extended" syntax:  spaces and comments are allowed inside
     *          the pattern.
     * </dl>
     * All other characters in the options argument are ignored.
     * @param args the arguments.
     * @return <b>true</b> if the pattern matches, <b>false</b> if it doesn't
     *         match, or <b>error</b> for invalid invocations (wrong number or
     *         types of arguments, etc.)
     */
    public static Expr regexp(Expr[] args) {
        if (args.length < 2 || args.length > 3) {
            return Constant.error("wrong number of args to regexp");
        }
        if (args[0].type != Expr.STRING) {
            return Constant.error("regexp: arg 1 must be string, not "
                                  + args[0].typeName());
        }
        String pat = args[0].stringValue();

        if (args[1].type != Expr.STRING) {
            return Constant.error("regexp: arg 2 must be string, not "
                                  + args[1].typeName());
        }
        String str = args[1].stringValue();
        String opts = null;
        if (args.length > 2) {
            if (args[2].type != Expr.STRING) {
                return Constant.error("regexp: arg 3 must be string, not "
                                      + args[2].typeName());
            }
            opts = args[2].stringValue();
        }
        Pattern p;
        try {
            p = compilePattern(pat, opts);
            return Constant.bool(p.matcher(str).find());
        } catch (IllegalArgumentException ex) {
            return Constant.error(ex.getMessage());
        }
    } // regexp(Expr[])

    /** Compares a string with a shell-style "glob" pattern.
     * The pat string may contain instances of * and/or ?.  The result is true
     * if pat matches str and false otherwise.  The pattern character ? matches
     * any character; * matches any sequence of zero or more characters.  A
     * backslash preceding any pattern character removes any special meaning
     * from that character and makes it match only itself.  For example \?
     * matches a question mark and \\ matches a single backslash.  Any other
     * character matches only itself.  Note that case is significant in the
     * comparison.
     * @deprecated Should be replaced by regexp pattern match. 
     * @param str the string to be compared against
     * @param pat the pattern to match
     * @return a boolean constant with value true if the pattern matches and
     * false otherwise.
    */
    public static Expr glob(Expr str, Expr pat) {
        if (!(str.type == Expr.STRING)) {
            return Constant.error("glob: arg 1 must be string, not "
                                  + str.typeName());
        }
        if (!(pat.type == Expr.STRING)) {
            return Constant.error("glob: arg 2 must be string, not "
                                  + pat.typeName());
        }
        return Constant.bool(glob(str.stringValue(),pat.stringValue(),false));
    } // glob(Expr, Expr)

    /** Compares a string with a shell-style "glob" pattern, ignoring case.
     * The pat string may contain instances of * and/or ?.  The result is true
     * if pat matches str and false otherwise.  The pattern character ? matches
     * any character; * matches any sequence of zero or more characters.  A
     * backslash preceding any pattern character removes any special meaning
     * from that character and makes it match only itself.  For example \?
     * matches a question mark and \\ matches a single backslash.  A lower-case
     * letter mathches the upper or lower case version of that letter.  Any
     * other character (including an upper-case letter) matches only itself.
     * @deprecated Should be replaced by regexp pattern match. 
     * @param str the string to be compared against
     * @param pat the pattern to match
     * @return a boolean constant with value true if the pattern matches and
     * false otherwise.
     */
    public static Expr iglob(Expr str, Expr pat) {
        if (!(str.type == Expr.STRING)) {
            return Constant.error("glob: arg 1 must be string, not "
                                  + str.typeName());
        }
        if (!(pat.type == Expr.STRING)) {
            return Constant.error("glob: arg 2 must be string, not "
                                  + pat.typeName());
        }
        return Constant.bool(glob(str.stringValue(),pat.stringValue(),true));
    } // iglob(Expr, Expr)

    //============= time and date functions ==================================

    /** Get the current time in seconds since the epoch.
     * @return the current time in seconds.
     */
    public static Expr time() {
        return Constant.getInstance((int)(System.currentTimeMillis() / 1000));
    } // time()

    /** Convert a number of seconds into a time interval string of the form
     * "days+hh:mm:ss".
     * @param secs the time interval in seconds.
     * @return a string version of the interval.
     */
    public static Expr interval(Expr secs) {
        if (secs.type != Expr.INTEGER) {
            return Constant.error("timeInterval(" + secs.typeName() + ")");
        }
        int time = secs.intValue();
        int[] base = { 60, 60, 24 };
        char[] sep = { ':', ':', '+' };
        String res = ""; 
        for (int i = 0; i < 3; i++) {
            int n = time % base[i];
            time /= base[i];
            res = n + res;
            if (time==0) { 
                break;
            }
            if (n < 10) {
                res = "0" + res;
            }
            res = sep[i] + res;
        }
        if (time > 0) {
            res = time + res;
        }
        return Constant.getInstance(res);
    } // interval(Expr)

    /** Convert to absolute time.
     * @param args the arguments.  May be either a single string (which is
     *        parsed as an ISO 8601 date spec), or an optional number of seconds
     *        offset from the epoch and optional number of seconds east of
     *        Greenwhich.
     * @return an absolute time or error constant.
     */
    public static Expr absTime(Expr[] args) {
        // If the first arg is a string, it must be the only argument.
        // The call is absTime("yyyy-mm-ddThh:mm:ss.mmm+zz:zz") or some such.
        if (args.length > 0 && args[0].type == Expr.STRING) {
            if (args.length > 1) {
                return Constant.error("wrong number of args to absTime");
            }
            return Constant.stringToAbsTime(args[0].stringValue());
        }
        if (args.length > 2) {
            return Constant.error("wrong number of args to absTime");
        }

        long t = 0;
        int z = 0;
        if (args.length == 0) {
            t = System.currentTimeMillis();
            z = TimeZone.getDefault().getOffset(t) / 1000;
        } else {
            try {
                t = Math.round(toReal(args[0]) * 1000);
            } catch (NumberFormatException e) {
                return Constant.error(
                            "absTime(invalid number " + args[0] + ")");
            } catch (ClassCastException e) {
                return Constant.error("absTime(" + args[0].typeName() + ")");
            }
            if (args.length == 1) {
                z = TimeZone.getDefault().getOffset(t) / 1000;
            } else {
                if (args[1].type != Expr.INTEGER) {
                    return Constant.error(
                        "invalid " + args[1].typeName()
                        + " second argument to absTime");
                }
                z = args[1].intValue();
            }
        }
        return Constant.getInstance(t,z);
    } // absTime(Expr)

    /** Convert to relative time.
     * @param arg the value to be converted.
     * @return a relative time or error constant.
     */
    public static Expr relTime(Expr arg) {
        if (arg.type == Expr.STRING) {
            return Constant.stringToRelTime(arg.stringValue());
        } else {
            try {
                return Constant.getInstance(
                    (long) Math.round(toReal(arg) * 1000));
            } catch (NumberFormatException e) {
                return Constant.error(
                            "relTime(invalid number " + arg + ")");
            } catch (ClassCastException e) {
                return Constant.error("relTime(" + arg.typeName() + ")");
            }
        }
    } // relTime(Expr)

    /** Split a time constant into its components.
     * @param args the arguments.  May be either an AbsTime or a Realtime.
     *        For AbsTime the result is a Record expression with attributes
     *          Type, Year, Month, Day, Hours, Minutes, Seconds, Offset
     *        For RelTime the result is a Record expression with attributes
     *          Type, Days, Hours, Minutes, Seconds.
     *        Type is "RelativeTime" or "AbsoluteTime".
     * @return a Record expression.
     */
    public static Expr splitTime(Expr[] args) {
        // If the first arg is a string, it must be the only argument.
        // The call is absTime("yyyy-mm-ddThh:mm:ss.mmm+zz:zz") or some such.
        if (args.length != 1) {
            return Constant.error("wrong number of args to splitTime");
        }
        if (!(args[0] instanceof Constant)) {
            return Constant.error("splitTime arg is not a constant");
        }
        Constant arg = (Constant) args[0];
        switch (arg.type) {
        case Expr.ABSOLUTE_TIME: {
            Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
            cal.setTimeInMillis(arg.milliseconds() + 1000 * arg.zone());
            RecordExpr res = new RecordExpr(8);
            res.insertAttribute("Type", Constant.getInstance("AbsoluteTime"));
            res.insertAttribute("Year",
                        Constant.getInstance(cal.get(Calendar.YEAR)));
            res.insertAttribute("Month",
                        Constant.getInstance(cal.get(Calendar.MONTH) + 1));
            res.insertAttribute("Day",
                        Constant.getInstance(cal.get(Calendar.DATE)));
            res.insertAttribute("Hours",
                        Constant.getInstance(cal.get(Calendar.HOUR_OF_DAY)));
            res.insertAttribute("Minutes",
                        Constant.getInstance(cal.get(Calendar.MINUTE)));
            res.insertAttribute("Seconds",
                        Constant.getInstance(
                            cal.get(Calendar.SECOND)
                                + cal.get(Calendar.MILLISECOND) / 1000.0));
            res.insertAttribute("Offset",
                        Constant.getInstance(arg.zone()));
            return res;
        }
        case Expr.RELATIVE_TIME: {
            RecordExpr res = new RecordExpr(5);
            long l = arg.milliseconds();
            int i;
            Expr d, hr, min, sec;

            if ((l % 1000) == 0) {
                l /= 1000;
                sec = Constant.getInstance(l % 60);
                i = (int) (l / 60);
            } else {
                sec = Constant.getInstance((l % 60000) / 1000.0);
                i = (int) (l / 60000);
            }
            min = Constant.getInstance(i % 60);
            i /= 60;
            hr = Constant.getInstance(i % 24);
            d = Constant.getInstance(i / 24);

            res.insertAttribute("Type", Constant.getInstance("RelativeTime"));
            res.insertAttribute("Days", d);
            res.insertAttribute("Hours", hr);
            res.insertAttribute("Minutes", min);
            res.insertAttribute("Seconds", sec);
            return res;
        }
        default:
            return Constant.error("splitTime arg is not a time constant");
        }
    } // absTime(Expr)

    /** Formats an AbsTime with strftime-like escapes.
     * Supports the the ANSI C strftime escapes %x, where x is any of the
     * characters aAbBcdHIjmMpSwxXyYZ%.
     * @param t the absolute time to format.
     * @param f the format
     * @return a String constant representing the formatted value.
     */
    public static Expr formatTime(Expr t, Expr f) {
        switch (t.type) {
        case Expr.INTEGER:
            t = absTime(new Expr[] { t });
            if (t.type != Expr.ABSOLUTE_TIME) {
                return t;
            }
            break;
        case Expr.ABSOLUTE_TIME:
            break;
        default:
            return Constant.error("invalid first argument to formatTime: " + t);
        }
        Constant time = (Constant) t;
        if (f.type != Expr.STRING) {
            return Constant.error("invalid second argument to formatTime: "
                                    + f);
        }
        String fmt = f.stringValue();
        fmt = strftimeToSimpleDateFormat(fmt);
        SimpleDateFormat df = new SimpleDateFormat(fmt);
        return Constant.getInstance(df.format(new Date(time.milliseconds())));
    } // formatTime(Expr, Expr);

    /** Convert a time constant to a string in "Unix" format, using GMT.
     * @deprecated Replaced by formatTime().
     * @param secs the time in seconds from the epoch.
     * @return a string in Unix "ctime" format.
     */
    public static Expr gmtTimeString(Expr secs) {
        if (secs.type != Expr.INTEGER) {
            return Constant.error("gmtTimeString(" + secs.typeName() + ")");
        }
        Date when = new Date(1000L * secs.intValue());
        SimpleDateFormat df
            = new SimpleDateFormat("EEE MMM dd HH:mm:ss 'UTC' yyyy");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        String s = df.format(when);
        // SimpleDateFormat appears to have no way of specifying that
        // the day of the month is to be formatted with a leading space
        // rather than a leading zero if it is less than 10.
        if (s.charAt(8)=='0') {
            s = s.substring(0, 8) + ' ' + s.substring(9);
        }
        return Constant.getInstance(s);
    } // gmtTime(Expr)

    /** Convert a time constant to a string in "Unix" format, using the local
     * time zone.
     * @deprecated Replaced by formatTime().
     * @param secs the time in seconds from the epoch.
     * @return a string in Unix "ctime" format.
     */
    public static Expr localTimeString(Expr secs) {
        if (secs.type != Expr.INTEGER) {
            return Constant.error("localTimeString(" + secs.typeName() + ")");
        }
        Date when = new Date(1000L * secs.intValue());
        SimpleDateFormat df
            = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
        String s = df.format(when);
        // SimpleDateFormat appears to have no way of specifying that
        // the day of the month is to be formatted with a leading space
        // rather than a leading zero if it is less than 10.
        if (s.charAt(8)=='0') {
            s = s.substring(0, 8) + ' ' + s.substring(9);
        }
        return Constant.getInstance(s);
    } // localTime(Expr)
        
    //============= private helper methods ===================================

    /** Helper function:  Convert an arbitrary Expr into a String.
     * If the Expr is already a string Constant, just return its value.
     * Otherwise, convert it to a String.
     * @param e the expression to convert.
     * @return the corresponding string.
     */
    private static String getString(Expr e) {
        if (e.type == Expr.STRING) {
            return e.stringValue();
        } else {
            return e.toString();
        }
    } // getString(Expr)

    /** Match pattern against string.
     *  For now, this is pretty simple: "*" matches any sequence of characters,
     *  ? matches any one character, \c matches c, \ at the end matches \ and
     *  any other char matches only itself.
     *  For now, we use a recursive algorithm for simplicity.  This really
     *  ought to be replaced by a more efficient iterative algorithm.
     *  <p><b>TODO:</b>  Add full POSIX glob semantics.
     * @param pat the pattern.
     * @param str the string to be tested for a match.
     * @param ignoreCase if true, lower case in pattern matches upper
     *          case in string (but not vice versa).
     * @return true if the pattern matches the string.
     */
    private static boolean glob(String pat, String str, boolean ignoreCase) {
        if (pat.length() == 0) {
            return str.length()==0;
        }
        char c = pat.charAt(0);
        switch (c) {
        case '?':
            return
                str.length() > 0
                && glob(pat.substring(1),str.substring(1),ignoreCase);
        case '*':
            return
                glob(pat.substring(1),str,ignoreCase)
                || (str.length()>0
                    && glob(pat,str.substring(1),ignoreCase));
        case '\\':
            if (pat.length() == 1)
                return str.equals("\\");
            c = pat.charAt(1);
            return
                str.length()>0
                && str.charAt(0) == c
                && glob(pat.substring(2),str.substring(1),ignoreCase);
        default:
            return
                str.length()>0
                && (str.charAt(0) == c
                    || (ignoreCase
                        && str.charAt(0) == Character.toUpperCase(c)))
                && glob(pat.substring(1),str.substring(1),ignoreCase);
        }
    } // glob(String,String,boolean)

    /** Helper function:  Convert an arbitrary constant into a double value.
     * This is like the method real(), but the result is returned as a double
     * rather than an Expr.
     * @param arg the constant to convert.
     * @exception NumberFormatException
     *      if the constant can not be converted to a real number.
     * @return the corresponding real value
     * @throws ClassCastException if c has the wrong type.
     * @throws NumberFormatException if c cannot be converted to a double.
     */
    private static double toReal(Expr arg) {
        switch (arg.type) {
        case Expr.REAL:
            return arg.realValue();
        case Expr.INTEGER:
            return arg.intValue();
        case Expr.BOOLEAN:
            return arg.isTrue() ? 1.0 : 0.0;
        case Expr.ABSOLUTE_TIME:
            return ((Constant) arg).milliseconds() / 1000;
        case Expr.RELATIVE_TIME:
            Long l = (Long) ((Constant) arg).value;
            return (double) l.longValue() / 1000;
        case Expr.STRING:
            return Constant.stringToDouble(arg.stringValue());
        default:
            throw new ClassCastException("unknown type");
        }
    } // toReal(Constant)

    /** Helper function for formatTime.
     * Translates a strftime style format, e.g. "month %b year %Y" to a
     * a  SimpleDateFormat style pattern, .e.g. "'month 'MMM' year 'yyyy".
     *
     * @param fmt the strftime format
     * @return the SimpleDateFormat pattern.
     */
    private static String strftimeToSimpleDateFormat(String fmt) {
        StringBuffer sb = new StringBuffer();
        boolean literal = false;  // true if we are inside '...'
        for (int i = 0; i < fmt.length(); i++) {
            char c = fmt.charAt(i);
            if (c == '%' && i < fmt.length() - 1) {
                c = fmt.charAt(++i);
                String op = null;
                switch (fmt.charAt(i)) {
                case 'a': // abbreviated weekday name
                    op = "EEE";
                    break;
                case 'A': // full weekday name
                    op = "EEEE";
                    break;
                case 'b': // abbreviated month name
                    op = "MMM";
                    break;
                case 'B': // full month name
                    op = "MMMM";
                    break;
                case 'c': // local date and time representation
                    op = "EEE MMM dd HH:mm:ss yyyy";
                    break;
                case 'd': // day of the month (01-31
                    op = "dd";
                    break;
                case 'H': // hour in the 24-hour clock (0-23
                    op = "HH";
                    break;
                case 'I': // hour in the 12-hour clock (01-12
                    op = "hh";
                    break;
                case 'j': // day of the year (001-366
                    op = "D";
                    break;
                case 'm': // month (01-12
                    op = "MM";
                    break;
                case 'M': // minute (00-59
                    op = "mm";
                    break;
                case 'p': // local equivalent of AM or PM
                    op = "a";
                    break;
                case 'S': // second (00-59
                    op = "ss";
                    break;
                case 'w': // weekday (0-6, Sunday is 0
                    op = "E";
                    break;
                case 'x': // local date representation
                    op = "MM/dd/yy";
                    break;
                case 'X': // local time representation
                    op = "HH:mm:ss";
                    break;
                case 'y': // year without century (00-99
                    op = "yy";
                    break;
                case 'Y': // year with century
                    op = "yyyy";
                    break;
                case 'Z': // time zone name, if any
                    op = "zzz";
                    break;
                }
                if (op == null) {
                    // not %x for known x
                    // Drop into literal mode
                    if (!literal) {
                        sb.append('\'');
                        literal = true;
                    }
                    if (c == '%') {
                        // %% => %
                        sb.append('%');
                    } else {
                        // Unknown escape.  %? => %?
                        sb.append(c);
                        if (c == '\'') {
                            // %' => %''
                            sb.append('\'');
                        }
                    }
                } else {
                    // Get out of literal mode
                    if (literal) {
                        sb.append('\'');
                        literal = false;
                    }
                    sb.append(op);
                }
            } else {
                // Not %; output c in literal mode
                if (!literal) {
                    sb.append('\'');
                    literal = true;
                }
                sb.append(c);
                if (c == '\'') {
                    sb.append(c);
                }
            }
        } // for (int i = 0; i < fmt.length; i++)
        // Must end up back in non-literal mode
        if (literal) {
            sb.append('\'');
            literal = false;
        }
        return sb.toString();
    } // strftimeToSimpleDateFormat(String);

    /**
     * Helper function for regexp and regexpMember.
     * Compiles a pattern.
     * @param pat the regular expression to compile.
     * @param opts a strings of options including any of the characters
     *             'imsx'.
     * @return the compiled pattern
     * @throws IllegalArgumentException if the pattern is not valid.
     */
    private static Pattern compilePattern(String pat, String opts) {
        int flags = 0;
        if (opts != null) {
            for (int i = 0; i < opts.length(); i++) {
                switch (opts.charAt(i)) {
                case 'i':
                case 'I':
                    flags |= Pattern.CASE_INSENSITIVE;
                    break;
                case 'm':
                case 'M':
                    flags |= Pattern.MULTILINE;
                    break;
                case 's':
                case 'S':
                    flags |= Pattern.DOTALL;
                    break;
                case 'x':
                case 'X':
                    flags |= Pattern.COMMENTS;
                    break;
                default:
                    // Ignore
                    break;
                }
            }
        }
        try {
            return Pattern.compile(pat, flags);
        } catch (Exception ex) {
            // Only show the first line of the message
            String msg = ex.getMessage();
            int i = msg.indexOf('\n');
            if (i >= 0) {
                msg = msg.substring(0, i);
            }
            throw new IllegalArgumentException(
                        "regexp: bad pattern '" + pat + "': " + msg);
        }
    } // compilePattern(String, String)

} // Builtin
