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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * A function call.
 * An internal (non-leaf) node of an expression tree representing a function
 * name and list of arguments.
 * @see Expr
 * @author <a href="mailto:solomon@cs.wisc.edu">Marvin Solomon</a>
 * @version 2.2
 */
public class FuncCall extends Expr {
    private static String VERSION = "$Id: FuncCall.java,v 1.11 2005/05/06 20:54:07 solomon Exp $";

    /** Mapping from names of functions encoded as AttrName objects to
     * FunctionInfo objects.
     */
    private static Map functions = new HashMap();

    /** Information about a function. */
    private static class FunctionInfo {
        /** The method implementing the function. */
        public final Method method;

        /** The number of arguments required for this function.
         * The value -1 means varargs.
         */
        public final int argc;

        /** If true, arguments should be evaluated before calling the function.
         */
        public final boolean strict;

        /** Creates a FunctionInfo object.
         * @param method the method
         * @param argc the argc
         * @param strict true if arguments to the function should be evaluated
         */
        public FunctionInfo(Method method, int argc, boolean strict) {
            this.method = method;
            this.argc = argc;
            this.strict = strict;
        } // FunctionInfo(Method, int, boolean)

        /** For debugging.
         * @return a string version of this info.
         */
        public String toString() {
            return "(" + method.getName() + "," + argc + "," + strict + ")";
        } // toString()
    } // FunctionInfo

    /** The name of the function as supplied in the original source.  */
    public final AttrName func;

    /** The number of arguments.  -1 means varargs. */
    private int argc;

    /** If true, arguments should be evaluated before calling the function. */
    private boolean strict;

    /** The method that implements this function.  */
    private Method method;

    /** A pre-allocated error constant to return for unknown functions.
     * Non-null iff method is null.
     */
    private Constant unknownFunction = null;

    /** The raw parameters to the function. */
    public final List args;

    /** Pre-load the "built-in" functions. */
    static {
        if (!loadJavaLibrary("condor.classad.Builtin")) {
            throw new RuntimeException(
                        "Cannot load the library condor.classad.Builtin");
        }
    }

    /** Create a function node from a name and a list of paramters.
     * This constructor is private.  Callers should use getInstance instead.
     * @param func the name of the function (original case).
     * @param params the parameters supplied.
     */
    private FuncCall(AttrName func, List params) {
        super(CALL);
        this.func = func;
        this.args = params;

        // Look up the function in those loaded from libraries.
        FunctionInfo info = (FunctionInfo) functions.get(func);
        if (info != null) {
            this.method = info.method;
            this.argc = info.argc;
            this.strict = info.strict;
            return;
        }

        // In other cases, just return an error on any attempt to evaluate
        // this call.
        unknownFunction = Constant.error("unknown function " + func);
    } // FuncCall(AttrName,List)

    /** "relTime" for constant folding. */
    private static final AttrName REL_TIME = AttrName.fromString("relTime");

    /** "absTime" for constant folding. */
    private static final AttrName ABS_TIME = AttrName.fromString("absTime");

    /** "real" for constant folding. */
    private static final AttrName REAL = AttrName.fromString("real");

    /** Create a function node from a name and a list of paramters.
     * This method has a special hack for "constant folding" of certain
     * calls.  If the function name is absTime or relTime (ignoring
     * case) and there is exactly one argument, of type String, then return the
     * appropriate constant.  In all other cases, just return a new FuncCall
     * object.
     * @param func the name of the function (original case).
     * @param params the parameters supplied.
     * @return an Exp of type CALL, ABS_TIME, REL_TIME, REAL, or ERROR.
     */
    public static Expr getInstance(AttrName func, List params) {
        if (params != null && params.size() == 1) {
            Expr arg = (Expr) params.get(0);
            if (arg.type == STRING) {
                if (func.equals(REL_TIME)) {
                    return Builtin.relTime(arg);
                }
                if (func.equals(ABS_TIME)) {
                    return Constant.stringToAbsTime(arg.stringValue());
                }
            }
        }
        return new FuncCall(func, params);
    } // getInstance(AttrName,List)

    /** Convert this Expr to a string, appending the result to the end of "sb".
     * The representation is the "canonical native format":
     * <code>name '(' [ arg [ ',' arg ]* ')'</code> with no extra spaces.
     * @param sb a place to put the result.
     * @return sb.
     * @see ClassAdWriter
     */
    public StringBuffer toString(StringBuffer sb) {
        sb.append(func).append("(");
        String comma = "";
        for (Iterator i = args.iterator(); i.hasNext(); ) {
            Expr arg = (Expr) i.next();
            sb.append(comma);
            comma = ",";
            arg.toString(sb);
        }
        return sb.append(")");
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
        FuncCall o = (FuncCall) other;
        if (!func.equals(o.func) || args.size() != o.args.size()) {
            return false;
        }
        Iterator i1 = args.iterator();
        Iterator i2 = o.args.iterator();
        while (i1.hasNext()) {
            Expr e1 = (Expr) i1.next();
            Expr e2 = (Expr) i2.next();
            if (!e1.sameAs(e2)) {
                return false;
            }
        }
        return true;
    } // sameAs(Expr)

    /** Evaluate this Expr.  This is the internal method used to implement
     * {@link Expr#eval()}.
     * By default, all functions are "strict" in all their arguments. That is,
     * all arguments are evaluated before the function is called, and if any
     * argument evaluates to an undefined or error Constant, the value of the
     * call is undefined or error, with error taking precendence over
     * undefined.  In this case, the message is taken from the first argument
     * that evaluates to an error or undefined.
     * Functions can be flagged as "non-strict", in which case all evaluation
     * of arguments is inhibited, and the function implementation is
     * responsible for evaluating arguments as necessary.
     * <p>We assume at this time that there are no built-in strict functions
     * that take a RecordExpr or ListExpr as an argument.
     * @param env an environment used to evaluate the operands.  It is cleared
     * to the null environment before return.
     * @return a Constant representing the value.
     */
    protected Expr eval1(Env env) {
        if (method == null) {
            return unknownFunction;
        }
        //////////////////////////////////////////////////////

        // Non-strict functions expect an extra initial argument of type Env.
        // The paramter passed to method.invoke is
        //  strict varags
        //     N     N     { true, arg1, arg1 }
        //     N     Y     { true, { arg1, arg1 } }
        //     Y     N     { arg1, arg1 }
        //     Y     Y     { { arg1, arg1 } }
        // The value of argc does not include this extra argument.

        int nargs = args.size();
        if (argc >= 0 && argc != nargs) {
            return Constant.error("wrong number of args to " + func);
        }

        Object[] outer, inner;
        int argp;

        if (!strict) {
            // Inhibit evaluation of arguments.  Just construct an array of
            // pointers and pass it to method.invoke.
            try {
                if (argc < 0) {
                    // { true, { arg1, arg1 } }
                    outer = new Object[2];
                    inner = new Object[args.size()];
                    outer[1] = inner;
                    argp = 0;
                } else {
                    // { true, arg1, arg1 }
                    outer = new Object[args.size() + 1];
                    inner = outer;
                    argp = 1;
                }
                outer[0] = env;
                for (Iterator itr = args.iterator(); itr.hasNext(); ) {
                    inner[argp++] = (Expr) itr.next();
                }
                return (Expr) method.invoke(null, outer);
            } catch (Exception e) {
                return Constant.error(func + ": " + e.getMessage());
            }
        }

        // Normal (strict) case.

        Expr errVal = null;
        Expr undefVal = null;
        Iterator itr = args.iterator();
        inner = new Expr[nargs];
        if (argc < 0) {
            // { { arg1, arg1 } }
            outer = new Object[] { inner };
        } else {
            // { arg1, arg1 }
            outer = inner;
        }
        // Evaluate all the args in the original environment
        // This rather complex way of doing it avoids the overhead of
        // cloning the environment if there is exactly one argument.
        for (int i = 0; i < nargs; i++) {
            Expr arg = (Expr) itr.next();
            // Avoid the overhead of cloning the environement for the last
            // (or only) argument
            Expr argval = arg.eval((i < nargs - 1) ? new Env(env) : env);
            if (argval.type == ERROR && errVal == null) {
                errVal = argval;
            }
            if (argval.type == UNDEFINED && undefVal == null) {
                undefVal = argval;
            }
            inner[i] = argval;
        }
        if (errVal != null) {
            return errVal;
        }
        if (undefVal != null) {
            return undefVal;
        }

        try {
            return (Expr) method.invoke(null, outer);
        } catch (Exception e) {
            return Constant.error(e.getMessage());
        }
    } // eval1(Env)

    /** The precedence of the operator in this expression node.
     * Used to print expressions without superfluous parentheses.
     * Function calls have maximal precedence (higher than any operator).
     * @return the precendence of this node.
     * @see ClassAdWriter#MINIMAL_PARENTHESES
     */
    public int prec() {
        return MAXPREC+1;
    } // prec()

    /** Loads a library of externally defined Java functions.
     * Meant to be called from ClassAd.loadJavaLibrary.
     * @param className the fully qualified name of the class (e.g.
     *                  condor.classad.Builtin)
     * @return false if the load fails.
     * @see ClassAd#loadJavaLibrary(java.lang.String)
     */
    /*package*/ static boolean loadJavaLibrary(String className) {
        try {
            // Note that getMethods only returns public methods.
            Method[] methods = Class.forName(className).getMethods();
        METHODS_LOOP:
            for (int i = 0; i < methods.length; i++) {
                Method m = methods[i];
                // Only static methods are considered
                if (!Modifier.isStatic(m.getModifiers())) {
                    continue;
                }
                // Only methods that return Expr
                if (m.getReturnType() != Expr.class) {
                    continue;
                }
                Class[] param = m.getParameterTypes();
                int argc = param.length;
                int argp = 0;
                boolean strict = true;
                if (argc > 0 && param[0] == Env.class) {
                    // Env first parameter indicates a non-strict function,
                    // but it otherwise ignored
                    strict = false;
                    argp++;
                    argc--;
                }
                if (argp + 1 == param.length
                        && param[argp].getComponentType() == Expr.class)
                {
                    // one param of type Expr[]: varargs
                    argc = -1;
                } else {
                    for (; argp < param.length; argp++) {
                        if (param[argp] != Expr.class)  {
                            continue METHODS_LOOP;
                        }
                    }
                }
                AttrName name = AttrName.fromString(m.getName());
                if (functions.containsKey(name)) {
                    db("warning: ignoring duplicate function " + name);
                    continue;
                }
                functions.put(name, new FunctionInfo(m, argc, strict));
            }
            return true;
        } catch (ExceptionInInitializerError e) {
            e.printStackTrace(); // debug
            return false;
        } catch (ClassNotFoundException e) {
            e.printStackTrace(); // debug
            return false;
        }
    } // loadJavaLibrary(String)
} // FuncCall
