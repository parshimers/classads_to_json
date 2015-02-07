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
 * A library of handy methods for manipulating classad expressions.
 * The class contains only static methods for performing various operations on
 * classified advertisements.  See {@link Expr} and its various subclasses,
 * especially {@link RecordExpr} for the actual objects that are manipulated.
 * @see Expr
 * @see RecordExpr
 * @author <a href="mailto:solomon@cs.wisc.edu">Marvin Solomon</a>
 * @version 2.2
 */
public class ClassAd {
    private static String VERSION = "$Id: ClassAd.java,v 1.17 2005/05/06 20:54:06 solomon Exp $";

    // Convenience functions for matching class ads.

    /** The default constructor is made private to prevent anybody from trying
     * to create an instance of this class.
     */
    private ClassAd() {
        throw new RuntimeException();
    } // ClassAd()

    /** Wrap a pair of ads in an environment defining "self" and "other".
     * The result is the classad
     * <pre>
     *     env = [
     *          Ad1 = [ other = Ad2.self; self = ad1 ]
     *          Ad2 = [ other = Ad1.self; self = ad2 ]
     *     ]
     * </pre>
     * Inside ad1, <code>self</code> refers to <code>ad1</code> and
     * <code>other</code> refers to <code>ad2</code>, and in inside ad2,
     * <code>self</code> refers to <code>ad2</code> and <code>other</code>
     * refers to <code>ad1</code>.
     * @param ad1 the first ClassAd
     * @param ad2 the second ClassAd
     * @return a <code>RecordExpr env</code> such that
     *         <code>env.Ad1.self</code> is <code>ad1</code>, and
     *         <code>env.Ad2.self</code> is <code>ad2</code>.
     */
     public static RecordExpr bind(RecordExpr ad1, RecordExpr ad2) {
        // Create the classad env =
        // [
        //    Ad1 = [other = Ad2.self; self = <ad1>]
        //    Ad2 = [other = Ad1.self; self = <ad2>]
        // ]
        RecordExpr env = new RecordExpr();
        RecordExpr env1 = new RecordExpr();
        env1.insertAttribute("other", (new AttrRef("Ad2")).selectExpr("self"))
            .insertAttribute("self", ad1);
        RecordExpr env2 = new RecordExpr();
        env2.insertAttribute("other", (new AttrRef("Ad1")).selectExpr("self"))
            .insertAttribute("self", ad2);
        env.insertAttribute("Ad1", env1);
        env.insertAttribute("Ad2", env2);

        return env;
    } // bind(RecordExpr,RecordExpr)

    /** Evaluate a selection from a ClassAd.
     * @param ad a ClassAd from which to select.
     * @param attrs a sequence <code>attr1, attr2, ..., attrn</code> of
     * attribute names.
     * @return the result of evaluating <code>ad.attr1.attr2...attrn</code>.
     */
    public static Expr eval(RecordExpr ad, String[] attrs) {
        Expr sel = ad;
        for (int i=0; i<attrs.length; i++) {
            sel = sel.selectExpr(attrs[i]);
        }
        return sel.eval();
    } // eval(RecordExpr,String[])

    /** Evaluate a selection from a ClassAd.
     * @param ad a ClassAd from which to select.
     * @param attr an attribute name.
     * @return the result of evaluating <code>ad.attr</code>.
     */
    public static Expr eval(RecordExpr ad, String attr) {
        Expr sel = ad.selectExpr(attr);
        return sel.eval();
    } // eval(RecordExpr,String[])

    /** Evaluate an expression in the context of a ClassAd.
     * The <code>expr</code> is evaluated as if it were a the value of an
     * attribute named <code>name</code> in <code>ad</code>.
     * <p>
     * <b>Note</b>: the RecordExpr <code>ad</code> is modified and then
     * returned to its original state, so this method is not thread-safe.
     * @param name the name of the attribute.
     * @param expr the expression to evaluate.
     * @param ad the context for evaluation.
     * @return the result of evaluating <code>expr</code> in the context of
     *         <code>ad</code>.
     */
    public static Expr eval(String name, Expr expr, RecordExpr ad) {
        AttrName n = AttrName.fromString(name);
        ad.insertAttribute(n, expr);
        Expr result = ad.selectExpr(name).eval();
        ad.removeAttribute(n);
        return result;
    } // eval(Expr,RecordExpr)

    /** Evaluate an attribute of a ClassAd in an environment consisting of
     * a pair of ads.  The result is <code>ad1.attr</code> evaluated in
     * the environment produced by <code>bind(ad1, ad2)</code>.
     * @param attr the name of the attribute used to initiate the evaluation
     * @param ad1 the ClassAd used to initiate the evaluation
     * @param ad2 the other ClassAd
     * @return the result of the evaluation
     * @see #bind(RecordExpr,RecordExpr)
     */
    public static Expr eval(String attr, RecordExpr ad1, RecordExpr ad2) {
        return eval(bind(ad1, ad2), new String[] { "Ad1", "self", attr });
    } // eval(String,RecordExpr,RecordExpr)

    /** Match two ClassAds.  The expressions <code>expr1.requirements</code>
     * and <code>expr2.requirments</code> both evaluate to <b>true</b> in
     * the environment produced by <code>bind(expr1, expr2)</code>, then
     * the result is the list <code>{ rank1, rank2 }</code>, where
     * <code>rank<sub>i</sub></code> is the result of evaluating
     * <code>expr<sub>i</sub></code> in <code>bind(expr1, expr2)</code>.
     * If either of the <code>requirements</code> attributes is missing or
     * evaluates to a value other than <b>true</b>, or if either of the
     * <code>rank</code> attributes is missing or evaluates to something other
     * than an Integer, the result is <b>null</b>.
     * @param expr1 the first expression.
     * @param expr2 the second expression.
     * @return the result of the match, either a pair of integers or
     * <b>null</b>.
     * @see #bind(RecordExpr,RecordExpr)
     */
    public static int[] match(Expr expr1, Expr expr2) {
        if (expr1.type != Expr.RECORD || expr2.type != Expr.RECORD) {
            return null;
        }

        // Create an evaluation environment
        RecordExpr ad = bind((RecordExpr) expr1, (RecordExpr) expr2);

        // Check requirements
        if (!eval(ad, AD1_SELF_REQUIREMENTS).isTrue()
            || !eval(ad, AD2_SELF_REQUIREMENTS).isTrue())
        {
            return null;
        }

        // Evaluate ranks
        try {
            return new int[] {
                eval(ad, AD1_SELF_RANK).intValue(),
                eval(ad, AD2_SELF_RANK).intValue()
            };
        } catch (ArithmeticException e) {
            return null;
        }
    } // match(Expr,Expr)

    /** Loads a "library" of externally defined Java functions.
     * For the purposes of this method, a "library" is simply a Java class.
     * Only static methods of the class with certain signatures are considered.
     * <p>
     * <b>Note:</b>
     * <em> Methods not obeying these restrictions will be silently ignored</em>
     * <p>
     * The methods must be static and return an expression (more precisely,
     * condor.classad.Expr) and have an optional Env parameter followed by
     * either one parameter of type Expr[] or any number of parameters
     * (including zero) all of which are of type Expr.  In the former case, the
     * method defines a "varargs" function:  It will be called with an array of
     * Expr values representing the (evaluated) actual parameters.  In the
     * latter case, a call with the wrong number of arguments will evaluate to
     * ERROR without invoking the method at all.  Otherwise, the actual
     * parameters are evaluated and passed to the arguments of the method and
     * the value returned from the method is the value of the call.
     * <p>
     * If a method has an initial Env parameter, evaluation of the actual
     * parameters is inhibited.  The arguments are passed <em>as is</em> to the
     * the Expr or Expr[] formal parameters, and the Env parameter is set to
     * an environment (stack of enclosing Record expressions) that the method
     * can use to evaluate the expressions.
     * @param className the fully qualified name of the class (e.g.
     *                  condor.classad.Builtin)
     * @return false if the load fails.
     * @see Env
     */
    public static boolean loadJavaLibrary(String className) {
        return FuncCall.loadJavaLibrary(className);
    } // loadJavaLibrary(String)

    /** Path expression for use in match(). */
    private static final String[] AD1_SELF_REQUIREMENTS =
                                new String[] { "Ad1", "self", "requirements" };
    /** Path expression for use in match(). */
    private static final String[] AD2_SELF_REQUIREMENTS =
                                new String[] { "Ad2", "self", "requirements" };
    /** Path expression for use in match(). */
    private static final String[] AD1_SELF_RANK =
                                new String[] { "Ad1", "self", "rank" };
    /** Path expression for use in match(). */
    private static final String[] AD2_SELF_RANK =
                                new String[] { "Ad2", "self", "rank" };

    // Factories (static methods returning new objects)
    // See also ClassAdParser

    /** Create a constant expression from an integer value.
     * @param i the value.
     * @return the resulting Constant object.
     */
    public static Constant constant(int i) {
        return Constant.getInstance(i);
    } // constant(int)

    /** Create a constant expression from a real value.
     * @param x the value.
     * @return the resulting Constant object.
     */
    public static Constant constant(double x) {
        return Constant.getInstance(x);
    } // constant(double)

    /** Create a constant expression from a String value.
     * @param s the value.
     * @return the resulting Constant object.
     */
    public static Constant constant(String s) {
        return Constant.getInstance(s);
    } // constant(String)

} // ClassAd
