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

import java.text.Format;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * A constant (Integer, Real, etc.).  The constant is wrapped so that it
 * may be used as a node in an expression tree.  To conserve space, there
 * is at most one Constant instance with any given value.
 * @see Expr
 * @author <a href="mailto:solomon@cs.wisc.edu">Marvin Solomon</a>
 * @version 2.2
 */
public class Constant extends Expr {
    // ------------ Public static (class) variables -----------------------
    // ------------ Private static (class) variables ----------------------
    private static String VERSION = "$Id: Constant.java,v 1.31 2005/05/07 13:11:34 solomon Exp $";

    /** The set of all extant instances of Constant, indexed by value.
     * This Map is used to save storage overhead by ensuring that there
     * is at most one Constant with the same value.
     */
    private static Map instanceMap = new HashMap();

    // ------------ Public constants  -------------------------------------
    /** A default undefined constant. */
    public static final Constant Undef
        = new Constant("", UNDEFINED);

    /** A default error constant. */
    public static final Constant Error
        = new Constant("", ERROR);

    /** The unique boolean Constant true. */
    public static final Constant TRUE
        = new Constant(null, BOOLEAN);

    /** The unique boolean Constant false. */
    public static final Constant FALSE
        = new Constant(null, BOOLEAN);

    // ------------ Public instance variables -----------------------------
    /** The actual value of this Constant.  Usage depends on type:
     * <dl>
     * <dt>UNDEFINED     <dd>A String error message.
     * <dt>ERROR         <dd>A String error message.
     * <dt>BOOLEAN       <dd>null (there are only two Boolean Constants).
     * <dt>INTEGER       <dd>An Integer value.
     * <dt>REAL          <dd>A Double value.
     * <dt>STRING        <dd>A String value.
     * <dt>ABSOLUTE_TIME <dd>A Timestamp value (milliseconds since the epoch
     *                       and seconds <em>east</em> of Greenwich)
     * <dt>RELATIVE_TIME <dd>A Long value (number of milliseconds).
     * </dl>
     */
    public final Object value;

    // ------------ Private inner class -----------------------------------
    /** A timestamp, consisting of an instant in time and the time zone (as
     * an offset from GMT) where it was created.
     */
    private static class Timestamp {
        /** Format for converting a Date (interpreted as GMT) to ISO 8601
         * format.
         */
        private static final SimpleDateFormat dateAndTimeFormat;

        /** Initialize dateAndTimeFormat. */
        static {
            dateAndTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            dateAndTimeFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        }

        /** The instant in time, in milliseconds from the epoch, UTC. */
        public long ms;

        /** The offset, in seconds <em>east</em>, from UTC. Used only for
         * displaying this Timestamp as a String.
         */
        public int offset;

        /** Creates a Timestamp value.
         * @param ms the instant in time, in milliseconds from the epoch, UTC.
         * @param offset the offset, in seconds <em>east</em>, from UTC.
         */
        public Timestamp(long ms, int offset) {
            this.ms = ms;
            this.offset = offset;
        } // Timestamp.Timestamp(long,int)

        /** Compares this timestamp to o.
         * @return true if this timestamp equals o.
         */
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null) {
                return false;
            }
            if (! (o instanceof Timestamp)) {
                return false;
            }
            Timestamp ts = (Timestamp)o;
            return ms == ts.ms && offset == ts.offset;
        } // equals(Object)

        /** Converts this Timestamp into a string of the form
         * yyyy-mm-ddThh:mm:ss[+-]zz:zz.
         * @return the string form of this timestamp.
         */
        public String toString() {
            char[] zone = new char[6];
            int off = offset;
            String dt
                = dateAndTimeFormat.format(new Date(ms + 1000 * off));
            if (off < 0) {
                off = -off;
                zone[0] = '-';
            } else {
                zone[0] = '+';
            }
            off /= 60;
            zone[5] = Character.forDigit(off % 10, 10);
            off /= 10;
            zone[4] = Character.forDigit(off % 6, 10);
            zone[3] = ':';
            off /= 6;
            zone[2] = Character.forDigit(off % 10, 10);
            off /= 10;
            zone[1] = Character.forDigit(off % 6, 10);
            off /= 60;
            return dt + new String(zone);
        } // Timestamp.toString()
    } // Timestamp

    /** Compare this Expr to another expression to check for "deep structural
     * equality".  For constants, this is just like "is" except that
     * numeric constants are compared like "==".
     * @param other the other expression
     * @return true of this and the other expression are isomorphic.
     */
    public boolean sameAs(Expr other) {
        try {
            if (other == null) {
                return false;
            }
            if (type == REAL || type == INTEGER) {
                return this.realValue() == other.realValue();
            } else {
                return this.is(other);
            }
        } catch (ArithmeticException e) {
            return false;
        }
    } // sameAs(Expr)

    // ------------ Private constants  ------------------------------------
    /** An INTEGER Constant with the value 0, to expedite common cases. */
    private static final Constant INT_ZERO =
        new Constant(new Integer(0), INTEGER);

    /** An INTEGER Constant with the value 1, to expedite common cases. */
    private static final Constant INT_ONE =
        new Constant(new Integer(1), INTEGER);

    /** A REAL Constant with the value 0.0, to expedite common cases. */
    private static final Constant REAL_ZERO =
        new Constant(new Double(0.0), REAL);

    /** A REAL Constant with the value -0.0, to expedite common cases. */
    private static final Constant REAL_NEG_ZERO =
        new Constant(new Double(-0.0), REAL);

    /** A REAL Constant with the value 1, to expedite common cases. */
    private static final Constant REAL_ONE =
        new Constant(new Double(1.0), REAL);

    /** A STRING Constant with the value "", to expedite common cases. */
    private static final Constant EMPTY_STRING = new Constant("", STRING);

    /** Helper table for relTimeToString and stringToRelTime.  */
    private static final int[] threshold = {
        24*60*60*1000,
        60*60*1000,
        60*1000,
        1000
    };

    /** Format used by doubleToString. */
    private static DecimalFormat fmt;
    static {
        fmt = new DecimalFormat("0.000000000000000E00");
        DecimalFormatSymbols syms = new DecimalFormatSymbols();
        syms.setInfinity("INF");
        syms.setNaN("NaN");
        fmt.setDecimalFormatSymbols(syms);
    }

    /** Helper table for relTimeToString. */
    private static final char[] seperator = { '+', ':', ':' };

    /** Pattern for parsing relTime constants. */
    private static final Pattern reltimePattern
        = Pattern.compile(
                "^\\s*(-)"
                + "|\\G"
                + "\\s*(\\d+)\\s*(([+:.dDhHmMsS]))?"
            );

    /** Pattern for parsing absTime constants. */
    private static final Pattern timeZonePattern = Pattern.compile(
                        "\\D*([+-]\\d\\d):?(\\d\\d)$"
                        );

    /** Pattern for parsing absTime constants. */
    private static final Pattern dateTimePattern = Pattern.compile(
                        "^\\D*(\\d\\d\\d\\d)"         // year
                        + "(?:\\D*(\\d\\d)"           // month
                        + "(?:\\D*(\\d\\d)"           // day
                        + "(?:\\D*(\\d\\d)"           // hour
                        + "(?:\\D*(\\d\\d)"           // minute
                        + "(?:\\D*(\\d\\d)"           // second
                        + ")?)?)?)?)?"
                        );

    /** Helper table for stringToAbsTime.  Maps groups in dateTimePattern
     * to fields in a Calendar.  Note that group 0 is the entire match.
     */
    private static final int[] groupToField = {
                                            -1,
                                            Calendar.YEAR,
                                            Calendar.MONTH,
                                            Calendar.DATE,
                                            Calendar.HOUR_OF_DAY,
                                            Calendar.MINUTE,
                                            Calendar.SECOND };

    // ------------ Constructors ------------------------------------------

    /** Constructs an arbitrary constant.
     * Note that this is the only constructor and it is private.
     * Clients create new instances by calling the factory methods
     * newInstance, error, undefined, or bool.
     * @param value the value of this constant.
     * @param type the type of this constant.
     */
    private Constant(Object value, int type) {
        super(type);
        this.value = value;
    } // Constant(Object,int)

    // ------------ Methods -----------------------------------------------

    // --- Factory methods

    /** Common implementation of factory methods.
     * Returns the unique constant of type "type" and value "val", creating
     * one if necessary.  This is the internal method used to implement most of
     * the other getInstance methods
     * @param val the value.
     * @param type the type.
     * @return a new constant.
     */
    private static final Constant getInstance(Object val, int type) {
        Constant result = (Constant) instanceMap.get(val);
        if (result == null) {
            result = new Constant(val, type);
            instanceMap.put(val, result);
        }
        return result;
    } // getInstance(Object, int)

    /** Returns the unique integer constant with value "value", creating one if
     * necessary.
     * @param value the value of the Constant.
     * @return a new INTEGER constant.
     */
    public static final Constant getInstance(int value) {
        if (value == 0) {
            return INT_ZERO;
        }
        if (value == 1) {
            return INT_ONE;
        }
        return getInstance(new Integer(value), INTEGER);
    } // getInstance(int)

    /** Returns the unique real constant with value "value", creating one if
     * necessary.
     * @param value the value of the Constant.
     * @return a new REAL constant.
     */
    public static final Constant getInstance(double value) {
        if (value == 0.0) {
            return ((Double.doubleToLongBits(value) & 0x8000000000000000L) != 0)
                ? REAL_NEG_ZERO
                : REAL_ZERO;
        }
        if (value == 1.0) {
            return REAL_ONE;
        }
        return getInstance(new Double(value), REAL);
    } // getInstance(double)

    /** Returns the boolean constant with value "value", creating one if
     * necessary.  Equivalent to bool(value).
     * @param value the value (true or false)
     * @return Constant.TRUE or Constant.FALSE
     * @see #bool(boolean)
     */
    public static final Constant getInstance(boolean value) {
        return value ? TRUE : FALSE;
    } // getInstance(boolean)

    /** Returns the unique String constant with value "value", creating one if
     * necessary.
     * @param value the value of the Constant, not including any surrounding
     *              quotes.
     * @return a new STRING constant.
     */
    public static final Constant getInstance(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() == 0) {
            return EMPTY_STRING;
        }
        return getInstance(value, STRING);
    } // getInstance(String)

    /** Creates a String Constant from a character array, processing backslash
     * escapes.  Any surrounding quotes have already been stripped.
     * @param buf the array of characters.
     * @param start the index preceding the first character to be used.
     * @param stop the index following the last characater to be used.
     * @return a new STRING constant, or null for errors.
     */
    public static final Constant getInstance(char[] buf, int start, int stop) {
        return getInstance(unquoteString(buf, start, stop));
    } // getInstance(char[],int,int)

    /** Returns the unique absolute time constant corresponding to date "date",
     * in the local timezone, creating one if necessary.
     * @param date the date.
     * @return a new ABSTIME constant.
     */
    public static final Constant getInstance(Date date) {
        int off = TimeZone.getDefault().getOffset(date.getTime()) / 1000;
        return getInstance(new Timestamp(date.getTime(), off),
                           ABSOLUTE_TIME);
    } // getInstance(Date)

    /** Returns the unique absolute time constant corresponding to date "date",
     * in the indicated timezone, creating one if necessary.
     * @param date the date.
     * @param tz the timezone, as a number of seconds <em>east</em> of
     * Greenwich.
     * @return a new ABSTIME constant.
     */
    public static final Constant getInstance(Date date, int tz) {
        return getInstance(new Timestamp(date.getTime(), tz), ABSOLUTE_TIME);
    } // getInstance(Date,int)

    /** Returns the unique absolute time constant with time "t" and offset "z",
     * creating one if necessary.
     * @param t offset from the epoch, in milliseconds.
     * @param z seconds east of Greenwich, in seconds.
     * @return a new ABSTIME constant.
     */
    public static final Constant getInstance(long t, int z) {
        return getInstance(new Timestamp(t, z), ABSOLUTE_TIME);
    } // getInstance(long, int)

    /** Returns the unique relative time constant with value "value",
     * creating one if necessary.
     * @param value the length of the time interval in milliseconds
     * @return a new RELTIME constant.
     */
    public static final Constant getInstance(long value) {
        return getInstance(new Long(value), RELATIVE_TIME);
    } // getInstance(long)

    /** Convenience function that creates an ERROR constant containing a
     * particular message.
     * @param msg the message.
     * @return the Constant.
     */
    public static Constant error(String msg) {
        if (msg == null) {
            msg = "";
        }
        return new Constant(msg, ERROR);
    } // error(String)

    /** Convenience function that creates an UNDEFINED constant containing a
     * particular message.
     * @param msg the message.
     * @return the Constant.
     */
    public static Constant undefined(String msg) {
        if (msg == null) {
            msg = "";
        }
        return new Constant(msg, UNDEFINED);
    } // undefined(String)

    /** Convenience function that converts a boolean to Constant.TRUE or
     * Constant.FALSE.  Equivalent to getInstance(b).
     * @param b the boolean value.
     * @return the Constant.
     * @see #getInstance(boolean)
     */
    public static Constant bool(boolean b) {
        return b ? TRUE : FALSE; 
    } // bool(boolean)

    // --- Expr methods

    /** Evaluates this Expr.  This is the internal method used to implement
     * {@link Expr#eval()}.
     * @param env an environment used to evaluate the operands.  It is cleared
     * to the null environment before return.
     * @return this Constant.
     */
    protected Expr eval1(Env env) {
        env.clear();
        return this;
    } // eval1(Env)

    /** The precedence of this expression node (MAXPREC).
     * Used to print expressions without superfluous parentheses.
     * @return the precendence of this node.
     * @see ClassAdWriter#MINIMAL_PARENTHESES
     */
    public int prec() {
        return MAXPREC + 1;
    } // prec()

    // --- Object methods

    /** Converts this Expr to a string, appending the result to the end of "sb".
     * The representation is the "canonical native format".
     * <dl>
     * <dt>Undefined    <dd>"UNDEFINED"
     * <dt>Error        <dd>"ERROR"
     * <dt>Boolean      <dd>"true" or "false"
     * <dt>Integer      <dd> [-]d+
     * <dt>Real         <dd> [-]d.d+Ed+ or 0, -0, INF, -INF, NaN
     * <dt>String       <dd>'"'.*'"'
     * <dt>AbsTime      <dd>absTime("yyyy-mm-ddThh:mm:ss[+-]zz:zz")
     * <dt>RelTime      <dd>relTime("[-]days+hh:mm:ss.fff")
     * </dl>
     * @param sb a place to put the result.
     * @return sb.
     * @see ClassAdWriter
     */
    public StringBuffer toString(StringBuffer sb) {
        switch (type) {
            case UNDEFINED:
                return sb.append("UNDEFINED");
            case ERROR:
                return sb.append("ERROR");
            case BOOLEAN:
                return sb.append(this == TRUE ? "true" : "false");
            case INTEGER:
                return sb.append(value);
            case REAL:
                return sb.append(doubleToString(realValue()));
            case STRING:
                return escapeString(sb, (String) value, '"');
            case ABSOLUTE_TIME:
                return sb.append("absTime(\"")
                    .append(value)
                    .append("\")");
            case RELATIVE_TIME:
            {
                long v = ((Long) value).longValue();
                return sb.append("relTime(\"")
                    .append(relTimeToString(v))
                    .append("\")");
            }
            default: throw new RuntimeException("unknown type");
        }
    } // toString(StringBuffer)

    // --- Accessors.

    // Each method coverts the value to the indicated type if
    // possible.  Otherwise, it throws ArithmeticException.

    /** Gets the integer value of this Constant if possible.
     * @exception ArithmeticException if type != INTEGER.
     * @return the integer value of this Constant.
     */
    public int intValue() throws ArithmeticException {
        if (type != INTEGER) {
            throw new ArithmeticException(typeName() + " " + this
                + " in integer context");
        }
        return ((Integer)value).intValue();
    } // intValue()

    /** Gets the double floating point value of this Constant if possible.
     * @exception ArithmeticException if type != INTEGER and type != REAL.
     * @return the real value of this Constant converted to double.
     */
    public double realValue() throws ArithmeticException {
        switch (type) {
            case REAL:
                return ((Double)value).doubleValue();
            case INTEGER:
                return ((Integer)value).doubleValue();
        }
        throw new ArithmeticException(typeName() + " " + this
            + " in real context");
    } // realValue()

    /** Gets the string value of this Constant if possible.
     * @exception ArithmeticException if type != STRING.
     * @return the String value of this Constant.
     */
    public String stringValue() throws ArithmeticException {
        if (type != STRING) {
            throw new ArithmeticException(typeName() + " " + this
                + " in string context");
        }
        return (String)value;
    } // stringValue()

    /** Gets the boolean value of this Constant if possible.
     * @exception ArithmeticException if TYPE != BOOLEAN.
     * @return the boolean value of this Constant.
     * @see #isTrue()
     */
    public boolean booleanValue() throws ArithmeticException {
        if (type != BOOLEAN) {
            throw new ArithmeticException(typeName() + " " + this
                + " in boolean context");
        }
        return this == TRUE;
    } // booleanValue()

    /** Convenience function to test whether an expression is the constant
     * TRUE.  Note that unlike booleanValue(), this method never throws an
     * exception.
     * @return true if this Expr is a constant of type boolean with value true,
     * and false in all other cases.
     * @see #booleanValue()
     */
    public boolean isTrue() {
        return this == TRUE;
    } // isTrue()

    /** Converts a time value to milliseconds.
     * @exception ArithmeticException if this Constant is not a time value.
     * @return the number of milliseconds (RELATIVE_TIME) or the number of
     *         milliseconds since 1970-01-01 UTC (ABSOLUTE_TIME).
     */
    public long milliseconds() throws ArithmeticException {
        if (type == ABSOLUTE_TIME) {
            return ((Timestamp) value).ms;
        }
        if (type == RELATIVE_TIME ) {
            return ((Long) value).longValue();
        }
        throw new ArithmeticException(typeName() + " " + this
            + " in integer context");
    } // milliseconds()

    /** Returns the time zone of an absolute time value.
     * @exception ArithmeticException if type != ABSOLUTE_TIME.
     * @return the zone, in seconds <em>east</em> of Greenwich
     */
    public int zone() throws ArithmeticException {
        if (type != ABSOLUTE_TIME) {
            throw new ArithmeticException("zone(" + typeName() + ")");
        }
        return ((Timestamp) value).offset;
    } // zone()

    /** Returns the annotation of an UNDEFINED or ERROR value.
     * @exception ArithmeticException if type != ERROR and type != UNDEFINED.
     * @return the annotation.
     */
    public String annotation() throws ArithmeticException {
        if (type != ERROR && type != UNDEFINED) {
            throw new ArithmeticException("annotation(" + typeName() + ")");
        }
        return (String) value;
    } // annotation()

    // --- Misc static methods for string processing.   These methods
    // all have "package" access.

    /** Converts a double value to a String.
     * The canonical unparsing of a real literal is one of the strings
     * <code>Real("INF")</code>, <code>Real("-INF")</code>,
     * <code>Real("NaN")</code>, <code>"0.0"</code>, or <code>"-0.0"</code> or
     * a normalized "scientific" representation such as
     * <code>6.020000000000000E+24</code>, or
     * <code>-0.000000000000000E+00</code>, as produced by the C printf format
     * "%1.15E":  an optional leading minus sign, one digit before the decimal
     * point (which must be non-zero unless the value is plus or minus zero),
     * 15 digits following the decimal point, the letter 'E', a (mandatory)
     * sign, and exponent of at least two digits.
     * @param d the value to convert.
     * @return a String representation of d.
     */
    /*package*/ static final String doubleToString(double d) {
        String v = fmt.format(d);
        // Special cases:
        if (Double.isNaN(d)) {
            return "real(\"NaN\")";
        }
        if (Double.isInfinite(d)) {
            return d < 0 ? "real(\"-INF\")" : "real(\"INF\")";
        }
        if (d == 0.0) {
            return
                (Double.doubleToLongBits(d) & 0x8000000000000000L) == 0
                    ? "0.0"
                    : "-0.0";
            }
        // For compatibility with printf %E format, we need a sign following
        // the 'E'.
        int ePos = v.indexOf('E');
        if (ePos >= 0 && v.charAt(ePos + 1) != '-') {
            ePos++;
            v = v.substring(0, ePos) + '+' + v.substring(ePos);
        }
        return v;
    } // doubleToString(double)

    /** Parses a string representation of a real value.
     * The string may be any of the specific strings <code>INF</code>,
     * <code>-INF</code>, or <code>Nan</code> in any combination of upper
     * and lower case, or any string that can be handled by Double.parseDouble.
     * @param s the string to parse.
     * @return the corresponding double value.
     * @throws NumberFormatException if s cannot be parsed.
     */
    /*package*/ static final double stringToDouble(String s) {
        s = s.trim();
        if (s.equalsIgnoreCase("inf")) {
            return Double.POSITIVE_INFINITY;
        }
        if (s.equalsIgnoreCase("-inf")) {
            return Double.NEGATIVE_INFINITY;
        }
        if (s.equalsIgnoreCase("nan")) {
            return Double.NaN;
        }
        // NB:  java.lang.Double.parseDouble accepts "NaN", but not "nan" or
        // "NAN".  It also excepts "Infinity" but only in that specific
        // combination of upper and lower cases.
        return Double.parseDouble(s);
    } // stringToDouble(String)

    /** Generates a string from an array of characters, processing backslash
     * escapes.  The the characters buf[start+1] ... buf[stop-1] are
     * converted to a String, after replacing backslash escapes such as \n by
     * their values.
     * @param buf the array of characters.
     * @param start the index of the first character to be used.
     * @param stop the index following the last characater to be used.
     * @return the converted String, or null on errors.
     */
    /*package*/ static final String unquoteString(char[] buf,
                                        int start, int stop)
    {
        char[] result = new char[stop - start];
        int i = start;
        int j = 0;
        while (i < stop) {
            char c = buf[i++];
            if (c == '\\') {
                if (i >= stop) {
                    // Backslash not permitted at end of string
                    return null;
                }
                c = buf[i++];
                // Now c is the character following the backslash
                int val = Character.digit(c,8);
                if (val >= 0) {
                    // A backslash followed by an octal digit:  Up to three
                    // octal digits (two if the first digit is greater than 3)
                    // are combined to compute the value.
                    int maxDigits = (val < '4') ? 2 : 1;
                    if (stop - i < maxDigits) {
                        maxDigits = stop - i;
                    }
                    for (int k = 0; k < maxDigits; k++) {
                        c = buf[i];
                        int n = Character.digit(c, 8);
                        if (n < 0) {
                            break;
                        }
                        val = (val << 3) + n;
                        i++;
                    }
                    // Now val is the value of the escape sequence.
                    if (val == 0) {
                        // Currently, nulls are not allowed inside strings
                        return null;
                    }
                    c = (char) val;
                } else switch (c) {
                    case 'n':
                        c = '\n';
                        break;
                    case 't':
                        c = '\t';
                        break;
                    case 'b':
                        c = '\b';
                        break;
                    case 'r':
                        c = '\r';
                        break;
                    case 'f':
                        c = '\f';
                        break;
                    case '\\':
                    case '\'':
                    case '"':
                        break;
                    default:
                        // Backslash followed by an "illegal" character
                        return null;
                }
            } // if (c == '\\')
            result[j++] = c;
        }
        return new String(result, 0, j);
    } // unquoteString(char[],int,int)

    /** Process backslash escapes in a string.
     * @param s the string to be processed.
     * @return the converted String, or null on errors.
     */
    /*package*/ static final String unquoteString(String s) {
        int l = s.length();
        char[] v = new char[l];
        s.getChars(0, l, v, 0);
        return unquoteString(v, 0, l);
    } // unquoteString(String)

    /** Appends to sb a quoted version of string, replacing non-printable
     * values by backslash escapes.  The result starts and ends with quote; any
     * embedded occurrences of quote are preceded by backslashes.  Any
     * non-printing characters are escaped with a single-character escape
     * (such as \n) if possible, otherwise \ooo, where ooo is a three-digit
     * octal code.
     * <p>
     * Note that Unicode is not supported: We <em>assume</em> the value of
     * every character is in the range 0..0377.
     * @param sb a place to put the result.
     * @param s the string to be quoted.
     * @param quote the quote character (' or ").
     * @return sb.
     */
    /*package*/ static StringBuffer escapeString(StringBuffer sb,
                                                 String s,
                                                 char quote)
    {
        sb.append(quote);
        for (int i=0; i<s.length(); i++) {
            int c = s.charAt(i) & 0xff;
            if (c == quote) {
                sb.append('\\');
            }
            switch (c) {
            case '\n': sb.append('\\').append('n'); break;
            case '\t': sb.append('\\').append('t'); break;
            case '\b': sb.append('\\').append('b'); break;
            case '\r': sb.append('\\').append('r'); break;
            case '\f': sb.append('\\').append('f'); break;
            case '\\': sb.append('\\').append('\\'); break;
            default:
                if (c < 040 || c > 0176) {
                    sb.append('\\');
                    sb.append((char) ('0' + ((c >> 6) & 7)));
                    sb.append((char) ('0' + ((c >> 3) & 7)));
                    sb.append((char) ('0' + (c & 7)));
                } else {
                    sb.append((char) c);
                }
                break;
            }
        }
        return sb.append(quote);
    } // escapeString(String)

    /** Converts a number of milliseconds to a string representation of a
     * relative time.
     * The result is a string of the form [-]days+hh:mm:ss.fff, where
     * leading components and the fraction .fff are omitted if they are zero.
     * There is no provision for leap seconds (each minute is assumed to be 60
     * seconds).
     * For example,
     * <ul>
     * <li> ten milliseconds becomes ".010".
     * <li> twelve minutes and thirty-four seconds becomes "12:34"
     * <li> 3 days, 15 millisecons becomes 3+00:00:00.015
     * </ul>
     * @param t the number to be converted, in milliseconds.
     * @return the resulting String.
     */
    /*package*/ static final String relTimeToString(long t) {
        if (t == 0) {
            return "0";
        }
        StringBuffer buf = new StringBuffer();
        if (t < 0) {
            buf.append('-');
            t = -t;
        }
        int i = 0;
        if (t < 1000) {
            // only milliseconds
            buf.append("0");
        } else {
            // Skip leading fields with value zero
            while (t < threshold[i]) {
                i++;
            }
            formatDecimal(buf, (int) (t / threshold[i]), 1);
            for (;;) {
                t %= threshold[i];
                if (i >= seperator.length) {
                    break;
                }
                buf.append(seperator[i]);
                i++;
                formatDecimal(buf, (int) (t / threshold[i]), 10);
            }
        }
        if (t > 0) {
            // append milliseconds
            buf.append('.');
            formatDecimal(buf, (int)t, 100);
        }
        return buf.toString();
    } // relTimeToString(long)

    /** Parses a relative time specification.
     * This method accepts strings generated by relTimeToString(long), but also
     * accepts minor variations in format.
     * <p>
     * The default format is [-]days+hh:mm:ss.fff, where
     * leading components and the fraction .fff are omitted if they are zero.
     * In the default syntax, days is a sequence of digits starting with a
     * non-zero digit, hh, mm, and ss are strings of exactly two digits (padded
     * on the left with zeros if necessary) with values less than 24, 60, and
     * 60, respectively and fff is a string of exactly three digits.  In the
     * relaxed syntax,
     * <ul>
     * <li>Whitespace may be added anywhere except inside the numeric
     *     fields days, hh, etc.
     * <li>Numeric fields may have any number of digits and any non-negative
     *     value.
     * <li>The '+' may be replaced by 'd' or 'D'.
     * <li>The first ':' may be replaced by 'h' or 'H'.
     * <li>The second ':' may be replaced by 'm' or 'M'.
     * <li>The letter 's' or 'S' may follow the last numeric field.
     * <li>If field i is terminated with one of the letters 'dDhHmMsS' and
     *     the value of field i-1 is zero, field i-1, together with its
     *     terminating field name ('+', ':', 'h", etc.) may be omitted even if
     *     field i-2 is not omitted.
     * </ul>
     * For example, one day, two minutes and three milliseconds may have any of
     * the forms
     * <ul>
     * <li> 1+00:02:00.003      (the result of relTimeToString)
     * <li> 1d0h2m0.003s        (similar to ISO 8601)
     * <li> 1d 2m 0.003s        (add spaces, omit hours field)
     * <li> 1d 00:02:00.003     (mixed representations)
     * <li> 1d 00:00:120.003    (number of seconds greater than 59)
     * <li> 86520.002991        (seconds, excess precision in fractions)
     * </ul>
     * @param s the string to parse.
     * @return a RelTime constant or an Error constant.
     */
    public static Constant stringToRelTime(String s) {
        if (s == null) {
            return error("invalid RelTime string: null");
        }
        boolean negative = false;
        String[] val = new String[5]; // contents of numeric fields
        String[] sep = new String[5]; // separators following each field
        int nFields = 0;
        boolean dotSeen = false;
        int end = 0;

        /* Split the string into fields */
        Matcher m = reltimePattern.matcher(s);
        while (m.find()) {
            if (m.group(1) != null) {
                negative = true;
            } else {
                if (nFields >= val.length) {
                    return error("invalid RelTime string \"" + s
                                + "\": too many fields");
                }
                val[nFields] = m.group(2);
                String g = m.group(3);
                if (g != null && g.charAt(0) == '.') {
                    if (dotSeen) {
                        return error("invalid RelTime string \"" + s
                                    + "\": two dots");
                    }
                    dotSeen = true;
                }
                sep[nFields] = g;
                nFields++;
            }
            end = m.end();
        }
        if (end != s.length()) {
            return error("invalid RelTime string \"" + s
                        + "\": extra characters at the end");
        }

        // Assign fields to postions: 
        // 0 = day, 1 = hour, 2 = min, 3 = sec, 4 = ms,
        // Default (terminated by ':', ' ',  etc) is one slot lower than
        // the immediately following field in the input.
        String[] pos = new String[5];
        int field = dotSeen ? 5 : 4;
        for (int i = nFields - 1; i >= 0; --i) {
            field--;
            if (sep[i] != null) {
                switch (sep[i].charAt(0)) {
                case 'd':
                case 'D':
                case '+':
                    field = 0;
                    break;
                case 'h':
                case 'H':
                    field = 1;
                    break;
                case 'm':
                case 'M':
                    field = 2;
                    break;
                case '.':
                    field = 3;
                    break;
                }
            }
            pos[field] = val[i];
        }

        long result = 0;
        String v = pos[4];
        if (v != null) {
            int l = v.length();
            for (int i = 0; i < 3; i++) {
                if (i < l) {
                    result = 10 * result
                                + Character.getNumericValue(v.charAt(i));
                } else {
                    result *= 10;
                }
            }
            if (l > 3 && v.charAt(3) >= '5') {
                result++;
            }
        }
        for (int i = 3; i >= 0; --i) {
            if (pos[i] != null) {
                result += Integer.parseInt(pos[i]) * threshold[i];
            }
        }
        if (negative) {
            result = -result;
        }

        return Constant.getInstance(result);
    } // stringToRelTime(String)

    /** Parses an absolute time specification.
     * This method accepts strings generated by Timestam.toString(), but also
     * accepts minor variations in format.
     * <p>
     * The default format is yyyy-mm-ddThh:mm:sszzzzzz where zzzzzz is a time
     * zone in the format +hh:mm or -hh:mm, but variations are allowed.
     * <ul>
     * <li> Each separator character '-', ':', and 'T' may be omitted or
     *      replaced by any sequence of non-digits.  Note, however, that the
     *      '-' in a time zone of the form -hhmm may not be omitted.
     * <li> An arbitrary sequence of non-digit characaters may precede zzzzz
     *      or yyyy.
     * <li> The colon between hh and mm in zzzzzz may be omitted.
     * <li> The zone may be omitted, in which case the local time zone is used.
     * <li> The zone may be replaced by the character 'z' or 'Z', which is
     *      equivalent to -00:00.
     * <li> The fields ss, mm, hh, etc. may be omitted (from right to left),
     *      in which case the omitted fields are assumed to be zero.
     * </ul> 
     * More precisely, the string must match the regular expression
     * <pre>
     *  D* dddd [D* dd [D* dd [D* dd [D* dd [D* dd D*]]]]] [-dd:dd|+dd:dd|z|Z]
     * </pre>
     * Where 'd' stands for a digit and 'D' stands for a non-digit.
     * <p>
     * For example, in the United States central time zone, the date and time 9
     * am Jan 25, 2003 CST may be written in any of the forms
     * <dl>
     * <dt>2003-01-25T09:00:00-06:00 <dd>output of Timestamp.toString()
     * <dt>2003-01-25   09:00:00 -0600 <dd>different separators
     * <dt>20030125090000-0600       <dd>compact format
     * <dt>2003-01-25 16:00:00 +0100 <dd>different time zone
     * <dt>2003-01-25 15:00Z         <dd>omitted seconds, UTC time zone
     * <dt>2003-01-25 09:00:00       <dd>default time zone (local)
     * <dt>2003-01-25 09             <dd>omitted minutes and seconds 
     * </dl>
     * and the date Jan 25, 2003 (implcitly midnight, UTC) may be written
     * <dl>
     * <dt>2003-01-24T18:00:00-0600  <dd>output of Timestamp.toString()
     * <dt>2003-01-25T00:00:00       <dd>default time zone: UTC
     * <dt>2003-01-25                <dd>omitted time of day
     * <dt>2003/01/25                <dd>different separators
     * <dt>20030125                  <dd>compact format
     * </dl>
     * The following strings are invalid.
     * <dl>
     * <dt>2003-01-25T09:00:00-06    <dd>incomplete time zone
     * <dt>2003-01-25T09:00:00- 0600 <dd>space in time zone
     * <dt>2003-1-25                 <dd>missing digit in dd field
     * </dl>
     * @param s the string to parse.
     * @return an AbsTime constant or an Error constant.
     */
    public static Constant stringToAbsTime(String s) {
        if (s == null) {
            return error("Null abstime");
        }
        Matcher m = timeZonePattern.matcher(s);
        Calendar cal;

        // Check for a trailing time zone:  Z or z or dddd or dd:dd,
        // and allocate an appropriate "calendar" object.
        int last = s.length() - 1;
        if (last < 0) {
            return error("Invalid absTime \"" + s + "\"");
        }
        if (s.charAt(last) == 'z' || s.charAt(last) == 'Z') {
            cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
            s = s.substring(0, last);
        } else if (m.find()) {
            String zone = m.group(1) + m.group(2);
            cal = new GregorianCalendar(TimeZone.getTimeZone("GMT" + zone));
            s = s.substring(0, m.start(0));
        } else {
            cal = new GregorianCalendar();
        }

        // Fill in the calendar values
        cal.clear();
        m = dateTimePattern.matcher(s);
        if (!m.matches()) {
            return error("Invalid absTime \"" + s + "\"");
        }
        for (int i = 1; i <= 6; i++) {
            String g = m.group(i);
            if (g != null) {
                int v = Integer.parseInt(g);
                if (i == 2) {
                    // month of year: cal.set wants Jan = 0 (grumble)
                    v--;
                }
                cal.set(groupToField[i], v);
            }
        }

        // Use the calendar object to compute the milliseconds and tz offset
        int offset =
            cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET);
        return getInstance(
                    new Timestamp(cal.getTimeInMillis(), offset / 1000),
                    ABSOLUTE_TIME);
    } // stringToAbsTime(String)

    /** Helper method for relTimeToString.
     * Converts n to a decimal string, optionally with leading zeros.
     * @param buf the place to put the result.
     * @param n the number to be converted (must be non-negative).
     * @param m a power of 10 indicating the minimum number of digits in the
     *    result.  For example, if m==100, the result will have at least three
     *    digits.
     */
    private static final void formatDecimal(StringBuffer buf, int n, int m) {
        buf.append((int)(n/m));
        while (m > 1) {
            n %= m;
            m /= 10;
            buf.append((int)(n/m));
        }
    } // formatDecimal(StringBuffer,int,int)

    /** Compares this Constant to another Constant.  Returns true if they
     * represent equal constant values--that is, c1 == c2 evaluates to true
     * according to the semantics of the ClassAd language.  In particular, case
     * is ignored in string comparisons.  Integers are comparable to reals but
     * otherwise the comparison fails with an ArithmeticException if "other" is
     * not a Constant of the same type as "this".
     *
     * @param other another object.
     * @return true if other represents the same constant value as this.
     * @throws ArithmeticException if other is not a Constant of a compatible
     *                             type, or if this Constant is ERROR or
     *                             UNDEFINED.
     */
    public boolean equals(Object other) {
        Constant c;
        try {
            c = (Constant) other;
        } catch (ClassCastException e) {
            throw new ArithmeticException(
                        "attempt to compare " + typeName()
                        + " == " + other);
        }
        switch (type) {
        case INTEGER:
            if (c.type == INTEGER) {
                return intValue() == c.intValue();
            } else {
                return intValue() == c.realValue(); // may throw exception
            }
        case REAL:
            return realValue() == c.realValue(); // may throw exception
        case BOOLEAN:
            if (c.type != BOOLEAN) {
                throw new ArithmeticException(
                            "attempt to compare " + typeName()
                            + " == " + c.typeName());
            }
            return this == c;
        case STRING:
            return stringValue().equalsIgnoreCase(c.stringValue());
                                            // may throw exception
        case ABSOLUTE_TIME:
        case RELATIVE_TIME:
            if (type != c.type) {
                throw new ArithmeticException(
                                "attempt to compare " + typeName()
                                + " == " + c.typeName());
            }
            return milliseconds() == c.milliseconds();
        default:
            throw new ArithmeticException(
                            "attempt to compare " + typeName()
                            + " == " + c.typeName());
        } // switch (type)
    } // equals(Expr)

    /** Returns a hash code for this Constant. Obeys the general contract
     * for hashCode:  equal constants have the same hash code.
     * @return a hash code value for this object.
     */
    public int hashCode() {
        switch (type) {
        case INTEGER:
            return intValue();
        case REAL:
            return (int) Double.doubleToLongBits(realValue());
        case BOOLEAN:
            return booleanValue() ? 1 : 0;
        case STRING:
            return stringValue().toLowerCase().hashCode();
        case ABSOLUTE_TIME:
        case RELATIVE_TIME:
            return (int) milliseconds();
        case UNDEFINED:
            return Integer.MAX_VALUE; // totally arbitrary
        case ERROR:
            return Integer.MIN_VALUE; // totally arbitrary
        default: throw new RuntimeException("unknown type");
        } // switch (type)
    } // hashCode()
} // Constant
