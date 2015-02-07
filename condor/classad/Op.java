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
import java.util.Date;

/**
 * A unary or binary expression.
 * An internal (non-leaf) node of an expression tree representing a unary
 * or binary operator applied to one or two operands.
 * @see Expr
 * @author <a href="mailto:solomon@cs.wisc.edu">Marvin Solomon</a>
 * @version 2.2
 */
public class Op extends Expr {
    private static String VERSION = "$Id: Op.java,v 1.24 2005/05/06 20:54:07 solomon Exp $";

    /** The top-level operator.
     * @see Expr#opName
     */
    public final int op;

    /** The first (or only) operand. */
    public final Expr arg1;

    /** The second operand; null if the operator is unary. */
    public final Expr arg2;

    /** Classification of operators by type:
     * <dl>
     * <dt>i<dd>integer
     * <dt>n<dd>number (integer or real)
     * <dt>b<dd>bit (integer or boolean)
     * <dt>c<dd>comparison
     * <dt>s<dd>special
     * </dl>
     */
    private static final char[] opType = {
       's',  // ||
       's',  // &&
       'b',  // |
       'b',  // ^
       'b',  // &
       'c',  // ==
       'c',  // !=
       'c',  //  is 
       'c',  //  isnt 
       'c',  // <
       'c',  // >
       'c',  // <=
       'c',  // >=
       'i',  // <<
       'i',  // >>
       'i',  // >>>
       'n',  // +
       'n',  // -
       'n',  // *
       'n',  // /
       'n',  // %
       'n',  // +
       'n',  // -
       'i',  // ~
       'i'   // !
    }; // optType
    /** Map operators to precedence levels, to simplify printing */
    private static final int[] precedence = {
       0,  // ||
       1,  // &&
       2,  // |
       3,  // ^
       4,  // &
       5,  // ==
       5,  // !=
       5,  //  is 
       5,  //  isnt 
       6,  // <
       6,  // >
       6,  // <=
       6,  // >=
       7,  // <<
       7,  // >>
       7,  // >>>
       8,  // +
       8,  // -
       9,  // *
       9,  // /
       9,  // %
       10,  // +
       10,  // -
       10,  // ~
       10   // !
    }; // precedence

    /** Construct a node for a binary operator.
     * @param op the operator.
     * @param arg1 the left operand.
     * @param arg2 the right operand.
     */
    public Op(int op, Expr arg1, Expr arg2) {
        super(OP);
        this.op = op; this.arg1 = arg1; this.arg2 = arg2;
    } // Op(int,Expr,Expr)

    /** Construct a node for a unary operator.
     * @param op the operator.
     * @param arg1 the operand.
     */
    public Op(int op, Expr arg1) {
        super(OP);
        this.op = op; this.arg1 = arg1; this.arg2 = null;
    } // Op(int,Expr)

    /** Convert this Expr to a string, appending the result to the end of "sb".
     * The representation is the "canonical native format":
     * <code>'(' arg op arg ')'</code> or <code>'(' op arg ')'</code> with no
     * extra spaces.
     * @param sb a place to put the result.
     * @return sb.
     * @see ClassAdWriter
     */
    public StringBuffer toString(StringBuffer sb) {
        sb.append('(');
        if (arg2 == null) {
            // unary operator
            sb.append(opName[op]);
            arg1.toString(sb);
        } else {
            // binary operator
            arg1.toString(sb);
            sb.append(opName[op]);
            arg2.toString(sb);
        }
        return sb.append(')');
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
        Op o = (Op) other;
        if (op != o.op) {
            return false;
        }
        return arg1.sameAs(o.arg1)
            && (arg2 == null || arg2.sameAs(o.arg2));
    } // sameAs(Expr)

    /** The precedence of the operator in this expression node.
     * Used to print expressions without superfluous parentheses.
     * @return the precendence of this node.
     * @see ClassAdWriter#MINIMAL_PARENTHESES
     */
    public int prec() {
        return precedence[op];
    } // prec()

    /** Helper function to look up an entry in a truth table.
     * @param val the value to be classified
     * @return 0 for FALSE, 1 for Undefined, 2 for TRUE, and 3 for anything
     * else.
     */
    private static final int classify(Expr val) {
        switch (val.type) {
            case BOOLEAN: return val.isTrue() ? 2 : 0;
            case UNDEFINED: return 1;
            default: return 3;
        }
    } //classify(Expr)

    /** An error constant for type errors. */
    private static final Constant notError =
        Constant.error("!: argument must be boolean");

    /** An error constant for type errors. */
    private static final Constant andError =
        Constant.error("&&: argument must be boolean");

    /** An error constant for type errors. */
    private static final Constant orError =
        Constant.error("||: argument must be boolean");

    /** Truth table for evaluating the &amp;&amp; operator:
     * <pre>
     *    && | F U T E
     *    ---+--------
     *     F | F F F F
     *     U | F U U E
     *     T | F U T E
     *     E | E E E E
     * </pre>
     */
    private static final Expr[] andTable = new Expr[] {
        Constant.FALSE, Constant.FALSE, Constant.FALSE, Constant.FALSE,
        Constant.FALSE, Constant.Undef, Constant.Undef, andError,
        Constant.FALSE, Constant.Undef, Constant.TRUE,  andError,
        andError,       andError,       andError,       andError
    }; // andTable

    /** Truth table for evaluating the || operator:
     * <pre>
     *    || | F U T E
     *    ---+--------
     *     F | F U T E
     *     U | U U T E
     *     T | T T T T
     *     E | E E E E
     * </pre>
     */
    private static final Expr[] orTable = new Expr[] {
        Constant.FALSE, Constant.Undef, Constant.TRUE,  Constant.FALSE,
        Constant.Undef, Constant.Undef, Constant.TRUE,  orError,
        Constant.TRUE,  Constant.TRUE,  Constant.TRUE,  Constant.TRUE,
        orError,        orError,        orError,        orError
    }; // orTable

    /** Truth table for evaluating the ! operator:
     * <pre>
     *    ! |
     *    --+--
     *    F | T
     *    U | U
     *    T | F
     *    E | E
     * </pre>
     */
    private static final Expr[] notTable = new Expr[] {
        Constant.TRUE,  Constant.Undef, Constant.FALSE, notError
    }; // notTable

    /** Evaluate this Expr.  This is the internal method used to implement
     * {@link Expr#eval()}.
     * The operands are recursively evaluated and the operator is applied to
     * the values.
     * @param env an environment used to evaluate the operands.  It is cleared
     * to the null environment before return.
     * @return a Constant representing the value.
     */
    protected Expr eval1(Env env) {
        // Evaluate both operands in the initial environment.
        // The resulting environment is always null.
        Expr val1, val2;
        if (arg2 == null) {
            val1 = arg1.eval(env);
            val2 = null;
        } else {
            val1 = arg1.eval(new Env(env));
            val2 = arg2.eval(env);
        }
        env.clear();

        try {  // catch(ArithmeticException)
            // Special cases for SAME or DIFFERENT:
            // Simply evaluate both operands and see if they are identical.
            if (op == SAME) {
                return val1.is(val2) ?  Constant.TRUE : Constant.FALSE;
            }
            if (op == DIFFERENT) {
                return val1.is(val2) ? Constant.FALSE : Constant.TRUE;
            }

            // All operators are strict wrt ERROR for the first operand
            if (val1.type == ERROR) {
                return val1;
            }

            // Boolean operators
            if (op == AND) {
                return andTable[4*classify(val1) + classify(val2)];
            }
            if (op == OR) {
                return orTable[4*classify(val1) + classify(val2)];
            }
            if (op == NOT) {
                return notTable[classify(val1)];
            }

            // Once the operands are evaluated, they will be in normal form:
            // Constants, Records, or Lists.
            // All the remaining operators require Constants as operands.
            // (Like Java, and unlike C or C++, we do not consider a non-null
            // Record or List to be the equivalent of "true", so
            // expressions like "[A=3] || 7" evaluate to "error" rather than
            // "[A=3]"
            if (!(val1 instanceof Constant)) {
                return Constant.error(opName[op]
                        + " applied to List or ClassAd");
            }
            Constant const1 = (Constant) val1;

            // All other operators are strict wrt UNDEFINED
            if (const1.type == UNDEFINED) {
                return const1;
            }

            // Special cases for unary operators (there is no right child).
            switch (op) {
            case UPLUS:
                switch (const1.type) {
                case Constant.REAL:
                case Constant.INTEGER:
                case Constant.ABSOLUTE_TIME:
                case Constant.RELATIVE_TIME:
                    return const1;
                default:
                    return Constant.error("Unary + of "
                        + const1.typeName() + " value");
                }
            case UMINUS:
                switch (const1.type) {
                case Constant.REAL:
                    return Constant.getInstance(-const1.realValue());
                case Constant.INTEGER:
                    return Constant.getInstance(-const1.intValue());
                case Constant.ABSOLUTE_TIME:
                    return Constant.getInstance(
                        new Date(-const1.milliseconds()));
                case Constant.RELATIVE_TIME:
                    return Constant.getInstance(-const1.milliseconds());
                default:
                    return Constant.error("Unary - of "
                        + const1.typeName() + " value");
                }
            case BIT_COMPLEMENT:
                if (const1.type == INTEGER) {
                    return Constant.getInstance(~ const1.intValue());
                }
                return
                    Constant.error("Unary ~ of "+const1.typeName()+" value");
            case NOT:
                if (const1.type == BOOLEAN) {
                    return const1.isTrue()
                        ? Constant.FALSE
                        : Constant.TRUE;
                }
                return Constant.error("Unary ! of "
                                      + const1.typeName() + " value");
            // fall through on default
            } // switch (op)
            if (!(val2 instanceof Constant)) {
                return Constant.error(opName[op]
                                      + " applied to List or ClassAd");
            }
            Constant const2 = (Constant)val2;

            // All other operators are strict wrt UNDEFINED or ERROR
            if (const2.type == UNDEFINED || const2.type == ERROR) {
                return const2;
            }

            switch (opType[op]) {
            case 'b': {
                // Bit (integer or boolean)
                if (const1.type == INTEGER) {
                    int liv = const1.intValue();
                    int riv = const2.intValue(); // may throw exception
                    switch (op) {
                    case BITOR: return Constant.getInstance(liv | riv);
                    case BITXOR: return Constant.getInstance(liv ^ riv);
                    case BITAND: return Constant.getInstance(liv & riv);
                    }
                } else {
                    if (const1.type != BOOLEAN || const1.type != BOOLEAN) {
                        return Constant.error("type error: "
                            + const1.typeName() + opName[op]
                            + const2.typeName());
                    }
                    boolean liv = const1.isTrue();
                    boolean riv = const2.isTrue();
                    switch (op) {
                    case BITOR: return Constant.bool(liv | riv);
                    case BITXOR: return Constant.bool(liv ^ riv);
                    case BITAND: return Constant.bool(liv & riv);
                    }
                }
            }
            case 'i': {
                // Integer-only operations
                int liv = const1.intValue(), riv = const2.intValue();
                switch (op) {
                case BITOR: return Constant.getInstance(liv | riv);
                case BITXOR: return Constant.getInstance(liv ^ riv);
                case BITAND: return Constant.getInstance(liv & riv);
                case LEFT_SHIFT:
                    return Constant.getInstance(liv << riv);
                case RIGHT_SHIFT:
                    return Constant.getInstance(liv >> riv);
                case URIGHT_SHIFT:
                    return Constant.getInstance(liv >>> riv);
                default: // Shouldn't happen!
                    throw new java.lang.RuntimeException(
                        "unknown integer operator " + opName[op]);
                }
            } // case 'i'
            case 'n': {
                // Numeric operations (integer or real)
                if (const1.type == ABSOLUTE_TIME) {
                    // The only cases supported are
                    //   abs - abs -> rel
                    //   abs + rel -> abs
                    //   abs - rel -> abs
                    long ms1 = const1.milliseconds();
                    long ms2 = const2.milliseconds(); // may throw exception
                    if (const2.type == ABSOLUTE_TIME) {
                        if (op != MINUS) {
                            return Constant.error("type error: "
                                + const1.typeName() + opName[op]
                                + const2.typeName());
                        }
                        return Constant.getInstance(ms1 - ms2);
                    }
                    switch (op) {
                    case MINUS:
                        return Constant.getInstance(new Date(ms1 - ms2));
                    case PLUS:
                        return Constant.getInstance(new Date(ms1 + ms2));
                    default:
                        return Constant.error("type error: "
                            + const1.typeName() + opName[op]
                            + const2.typeName());
                    }
                } else if (const1.type == RELATIVE_TIME) {
                    // The only cases supported are
                    //   rel + abs -> abs
                    //   rel + rel -> rel
                    //   rel - rel -> rel
                    long ms1 = const1.milliseconds();
                    long ms2 = const2.milliseconds(); // may throw exception
                    if (const2.type == ABSOLUTE_TIME) {
                        if (op != PLUS) {
                            return Constant.error("type error: "
                                + const1.typeName() + opName[op]
                                + const2.typeName());
                        }
                        return Constant.getInstance(ms1 + ms2);
                    }
                    switch (op) {
                    case MINUS:
                        return Constant.getInstance(ms1 - ms2);
                    case PLUS:
                        return Constant.getInstance(ms1 + ms2);
                    default:
                        return Constant.error("type error: "
                            + const1.typeName() + opName[op]
                            + const2.typeName());
                    }
                } else if (const1.type == INTEGER && const2.type == INTEGER) {
                    int liv = const1.intValue(), riv = const2.intValue();
                    switch (op) {
                    case PLUS: return Constant.getInstance(liv + riv);
                    case MINUS: return Constant.getInstance(liv - riv);
                    case TIMES: return Constant.getInstance(liv * riv);
                    case DIV: return Constant.getInstance(liv / riv);
                    case MOD: return Constant.getInstance(liv % riv);
                    default: // Shouldn't happen!
                        throw new java.lang.RuntimeException(
                            "unknown numeric operator " + opName[op]);
                    }
                } else {
                    double lrv = const1.realValue();
                    double rrv = const2.realValue();
                    switch (op) {
                    case PLUS: return Constant.getInstance(lrv + rrv);
                    case MINUS: return Constant.getInstance(lrv - rrv);
                    case TIMES: return Constant.getInstance(lrv * rrv);
                    case DIV: return Constant.getInstance(lrv / rrv);
                    case MOD: return Constant.getInstance(lrv % rrv);
                    default: // Shouldn't happen!
                        throw new java.lang.RuntimeException(
                            "unknown numeric operator " + opName[op]);
                    }
                }
            } // case 'n'
            case 'c': {
                // Comparison operations
                int cmp;
                switch (const1.type) {
                case INTEGER: {
                    int v1 = const1.intValue();
                    if (const2.type == INTEGER) {
                        int v2 = const2.intValue();
                        cmp = v1 < v2 ? -1 : v1 == v2 ? 0 : 1;
                    } else {
                        double v2 = const2.realValue();
                            // may throw exception
                        cmp = v1 < v2 ? -1 : v1 == v2 ? 0 : 1;
                    }
                    break;
                }
                case REAL: {
                    double v1 = const1.realValue();
                    double v2 = const2.realValue(); // may throw exception
                    cmp = v1 < v2 ? -1 : v1 == v2 ? 0 : 1;
                    break;
                }
                case BOOLEAN: {
                    if (const2.type != BOOLEAN
                        || (op != EQUAL && op != NOT_EQUAL))
                    {
                        throw new ArithmeticException(
                                    "attempt to compare " + const1.typeName()
                                    + " " + opName[op]
                                    + " " + const2.typeName());
                    }
                    cmp = const1 == const2 ? 0 : 1;
                    break;
                }
                case STRING: {
                    String v1 = const1.stringValue();
                    String v2 = const2.stringValue(); // may throw exception
                    cmp = v1.compareToIgnoreCase(v2);
                    break;
                }
                case ABSOLUTE_TIME:
                case RELATIVE_TIME: {
                    if (const1.type != const2.type) {
                        return Constant.error("type error: "
                            + const1.typeName() + opName[op]
                            + const2.typeName());
                    }
                    long v1 = const1.milliseconds();
                    long v2 = const2.milliseconds();
                    cmp = v1 < v2 ? -1 : v1 == v2 ? 0 : 1;
                    break;
                }
                default:
                    throw new ArithmeticException(
                                    "attempt to compare " + const1.typeName()
                                    + " " + opName[op]
                                    + " " + const2.typeName());
                } // switch (const1.type)
                switch(op) {
                case EQUAL: return Constant.bool(cmp == 0);
                case NOT_EQUAL: return Constant.bool(cmp != 0);
                case LESS: return Constant.bool(cmp < 0);
                case GREATER: return Constant.bool(cmp > 0);
                case LESS_EQ: return Constant.bool(cmp <= 0);
                case GREATER_EQ: return Constant.bool(cmp >= 0);
                default: // Shouldn't happen!
                    throw new java.lang.RuntimeException(
                        "unknown comparison operator " + opName[op]);
                }
            } // case 'c'
            default: // Shouldn't happen!
                throw new java.lang.RuntimeException(
                    "unknown operator " + opName[op] +
                    " of type " + opType[op]);
            } // switch (opType[op])
        } catch (ArithmeticException ex) {
            return Constant.error(ex.getMessage());
        }
    } // eval()
} // Op
