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

/** An attribute name.  It behaves like a String except for its equals
 * and hashCode methods, which ignore case.  In simple cases, the string is an
 * identifier (a sequence of letters and digits starting with a letter), but in
 * fact, it may contain arbitrary ASCII characters.  If it is not a simple
 * identifier, it is displayed surrounded by single quotes (apostrophes) with
 * backslash escapes for unprintable characters and quotes.
 * <p>
 * The implementation stores the string in two or three forms, the "raw" form,
 * which is the actual sequence of characters, the "canon" form, which is
 * the raw form translated to lower case, and the "pname" form which is the
 * raw form escaped and quoted if necessary to fit the native syntax for an
 * "Identifier".  The pname is generated on demand in toString() and cached.
 * <p>
 * In any one execution, there is only one instance with a given "raw" string.
 * The contructor is private, and instances are created by the factory
 * methods {@link #fromString(String)}, which accepts the "raw" form
 * of the string, and {@link #fromText(String)}, which accepts a version
 * with optional quotes and backslash escapes.   For example,
 * <code>fromString("foo")</code> and <code>fromText("foo")</code> return the
 * same instance of AttrName, as do <code>fromString("one two\nthree")</code>
 * and <code>fromText("'one two\\012three'")</code>.
 * <p>
 * The raw and canon strings are "interned" to save space and to speed up
 * comparisons:  The {@link #equals(Object)} method simply compares canon
 * strings for pointer equality.
 * @see AttrRef
 * @author <a href="mailto:solomon@cs.wisc.edu">Marvin Solomon</a>
 * @version 2.2
 */
public class AttrName {
    private static String VERSION = "$Id: AttrName.java,v 1.10 2005/05/06 20:54:06 solomon Exp $";

    /** The value with case preserved. */
    private final String raw;

    /** The canonical version:  raw translated to lower case. */
    private final String canon;

    /** A quoted and escaped "printable" version of raw.  Allocated and
     * initialized on demand.
     */
    private String pname;

    /** The reseved word PARENT. */
    /*package*/ static final AttrName PARENT = new AttrName("parent");

    /** Constructor.  Note that it is private.  Clients should use
     * the fromString() or fromText() method rather than new.
     * @param v the original String.
     */
    private AttrName(String v) {
        raw = v.intern();
        canon = v.toLowerCase().intern();
    } // AttrName(String)

    /** Converts to a printable String.  The "raw" value is returned as is
     * if it has the form an identifier; otherwise, it is quoted and
     * escaped.
     * @return a printable version of this AttrName.
     */
    public String toString() {
        if (pname == null) {
            if (raw.matches("\\A[A-Za-z_]\\w*\\z")) {
                pname = raw;
            } else {
                pname = Constant
                            .escapeString(new StringBuffer(), raw, '\'')
                            .toString();
            }
        }
        return pname;
    } // toString()

    /** Checks to see whether this AttrName needs to be quoted.
     * @return false if rawString() matches the syntax of an identifier.
     */
    public boolean needsQuoting() {
        // To avoid an expensive pattern match, we force calculation of the
        // cached pname.  In most cases, the return value will be "false",
        // and the cost will only be a pointer comparison.
        if (pname == null) {
            toString();
        }
        return pname != raw;
    } // needsQuoting()

    /** Returns the original string without quotes or escapes and original
     * capitalization.
     * @return the original string.
     */
    public String rawString() {
        return raw;
    } // rawString()

    /** Compare this AttrName to another AttrName.
     * Since there is a unique instance for any canonicalized value,
     * the comparison is simply pointer equality.
     * @param o the other AttrName
     * @return true if the strings are equal, ignoring case.
     * @exception ClassCastException if o is not an instance of AttrName.
     * @exception NullPointerException if o is null.
     */
    public boolean equals(Object o) {
        return canon == ((AttrName) o).canon;
    } // equals

    /** Compute a hash code for this string.  Strings that are equal except
     * for case return the same hash code.
     * @return the hash code.
     */
    public int hashCode() {
        return System.identityHashCode(canon);
    } // hashCode

    /** A mapping from String values to instances of AttrName.
     * If instanceMap.get(s) is non-null, it is an instance c of AttrName such
     * c.canon is the unescaped, lower-case version of s, and it is the
     * unique such instance such that c.pname == s.
     * Note that original-case strings are stored only once and shared as
     * keys in instanceMap and values of c.pname.
     */
    private static Map instanceMap = new HashMap();

    /** Returns the instance corresponding to a given NATIVE representation.
     * @param v the NATIVE representation.
     * @return the corresponding AttrName instance, or null on errors.
     */
    public static AttrName fromText(String v) {
        if (v == null || v.length() == 0) {
            return null;
        }
        if (v.charAt(0) == '\'') {
            // Quoted format; Strip quotes and expand backslash escapes.
            int end = v.length() - 1;
            if (v.charAt(end) != '\'') {
                return null;
            }
            char[] tmp = new char[end - 1];
            v.getChars(1, end, tmp, 0);
            v = Constant.unquoteString(tmp, 0, end - 1);
        } else if (!v.matches("\\A[A-Za-z_]\\w*\\z")) {
            return null;
        }
        return fromString(v);
    } // fromText(String)

    /** Returns the instance corresponding to a given raw String.
     * @param v the original (mixed case) string.
     * @return the corresponding AttrName instance, or null on errors.
     */
    public static AttrName fromString(String v) {
        if (v == null) {
            return null;
        }
        AttrName result = (AttrName) instanceMap.get(v);
        if (result == null) {
            result = new AttrName(v);
            instanceMap.put(v, result);
        }
        return result;
    } // fromString(String)
} // AttrName
