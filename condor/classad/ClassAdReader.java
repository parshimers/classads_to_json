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
import java.io.*;
import condor.cedar.*;

/** Convert a serialized classified advertisement into a stream of characters.
 * The current Condor C++ classad library uses a rather peculiar representation
 * for transmitting classads (see {@link #read()} for details).  ClassAdReader
 * may be used to wrap a CedarInputStream and turn it into a Reader (a stream
 * of input characters).  A sequence of classads appears to be a stream of
 * ASCII characters containing the ASCII representations of the classads, each
 * followed by a semicolon and a newline.  For example:
 * <pre>
 *         [myType="job";name="ad1";targetType="machine"];
 *         [myType="job";name="ad2";targetType="machine"];
 * </pre>
 * There are plans to simplify the format used for transporting classads in a
 * future release of Condor.  When that happens, this class can be replaced by
 * {@link java.io.InputStreamReader}.
 * <p>The inverse transformation, from a classified advertisement to the "old"
 * serialized form, is performed by
 * {@link RecordExpr#transmit(java.io.DataOutput) RecordExpr.transmit}.
 * @version 2.2
 * @author <a href="mailto:solomon@cs.wisc.edu">Marvin Solomon</a>
 */
public class ClassAdReader extends Reader {
    private static String VERSION = "$Id: ClassAdReader.java,v 1.9 2005/05/06 20:54:06 solomon Exp $";

    /** The underlying source of bytes. */
    private CedarInputStream in;

    /** A stream of characters "pushed back" to the input.  Return these
     * first before generating any new characters from the underlying input
     * stream.  May be null.
     */
    private String pushBack = null;

    /** If pushBack != null, this is the index in pushBack of the next
     * character to return.
     */
    private int pushBackPos = 0;

    /** Number of strings expected in the current input ad, not including the
     * string currently being processed.  The value -1 means we are between
     * input ads.
     */
    private int attrCount = -1;

    /** A flag to indicate that the underlying input stream has indicated
     * end-of-file.
     */
    private boolean atEOF = false;

    /** Create a new ClassAdReader.
     * @param in the underlying input stream.
     */
    public ClassAdReader(CedarInputStream in) {
        this.in = in;
    }

    /** Returns one character from the translated input stream.  Input consists
     * of zero or more ads, each preceded by two Cedar integers.  The first
     * integer is non-zero and indicates that an ad follows.  The second is an
     * attrtibute count n.  The body of the ad consists of n+2 null-terminated
     * strings.  The input stream ends with a zero Cedar integer (meaning "no
     * more ads").
     * <p>
     * Each character of input generates one or more characters of output,
     * the first character of which is returned immediately, with  the remaining
     * characters (if any) stored in pushBack.  The field attrCount
     * indicates the number of strings remaining in the current ad, not
     * including the current string.  In particular, when the attribute count
     * n is read, attrCount is set to n+1, attrCount==0 while the last string
     * is being processed, and attrCount is -1 between ads.
     * <p>
     * Here is an example.  In the input, the notation "{n}" represents the
     * number n encoded as a Cedar integer (8 bytes binary, most significant
     * byte first).
     * <pre>
     *     input   {1}{2}a=b\0foo=bar\0xxx\0yyy\0{1}{0}uuu\0vvv\0{0}
     *     output  [a=b;
     *             foo=bar;
     *             MyType="xxx";
     *             TargetType="yyy"];
     *             [MyType="uuu";
     *             TargetType"vvv"];
     *             EOF
     * </pre>
     * <p>
     * Here is a trace of the finite-state machine that does the translation.
     * The state consists of the values of attrCount and pushBack.
     * <pre>
     *     input   output  attrCont  pushBack
     *     -----   ------  --------  --------
     *                        -1
     *     {1}{2}  [           3
     *     a=b     a=b         3
     *     \0      ;           2     \n
     *     foo=bar foo=bar     2
     *     \0      ;           1     \nMyType="
     *     xxx     xxx         1
     *     \0      "           0     ;\nTargetType="
     *     yyy     yyy         0
     *     \0      "          -1     ];\n
     *     {1}{0}  [           1     MyType="
     *     uuu     uuu         1    
     *     \0      "           0     ;\nTargetType="
     *     vvv     vvv         0   
     *     \0      "          -1     ];\n
     *     {0}     EOF
     * </pre>
     * Note that "special actions" only occur when attrCount == -1 at the
     * start of this method, or when the input stream returns a null.
     * Otherwise, this method simply returns the next character from pushBack,
     * or the next character from the input stream if pushBack is empty.
     * <p>
     * Characters are retrieved from the input stream using
     * readUTFchar, which has the same contract as Reader.read():  A return
     * value of 0 means an encoded null was found.  A return value of -1 means
     * and "end-of-file" condition was encountered--in this case, a naked null
     * byte indicating "end-of-string".
     *
     * @return the next character of the transformed input stream, or -1 for
     * an end-of-file indication.
     * @exception IOException if an I/O error occurs on the underlying input
     * stream.
     */
    public int read() throws IOException {
        if (atEOF) {
            return -1;
        }

        // First check for pre-generated characters
        if (pushBack != null) {
            int result = pushBack.charAt(pushBackPos++);
            if (pushBackPos >= pushBack.length()) {
                pushBack = null;
                pushBackPos = 0;
            }
            return result;
        }

        // If we are between ads, read the header of the next ad.
        if (attrCount < 0) {
            try {
                if (in.readInt() == 0) {
                    // no more ads; return an EOF indication now and henceforth
                    atEOF = true;
                    return -1;
                }
            } catch (EOFException e) {
                // no more ads; return an EOF indication now and henceforth
                atEOF = true;
                return -1;
            }
            attrCount = in.readInt() + 1;
            if (attrCount == 1) {
                pushBack = "MyType=\"";
            }
            return '[';
        }

        // Get a character from the current input string.
        int c = in.readUTFchar();
        if (c >= 0) {
            // Got an ordinary character. Just return it.
            return c;
        }

        // Reached the end of the current string.  Advance to the next one.
        switch (attrCount--) {
        case 0:
            // Special case after the last string.  Follow it by "];\n".
            pushBack = "];\n";
            return '"';
        case 1:
            // Special case before the last string.  Precede it by
            // ";TargetType=".
            pushBack = ";\nTargetType=\"";
            return '"';
        case 2:
            // Special case before the penultimate string.  Precede it by
            // ";MyType=".
            pushBack = "\nMyType=\"";
            return ';';
        default:
            // All other cases.  The null between strings becomes ";\n".
            pushBack = "\n";
            return ';';
        }
    } // read()

    /* Read multiple characters.  This method repeatedly calls the no-argument
     * version of read.
     * <p>
     * Interesting note:  The default implementation of
     * {@link java.io.InputStream#read(byte[], int, int)} does something
     * similar, but the default implementation of class Reader does the
     * opposite.  It implements read() * using read(byte[], int, int) rather
     * than the converse.
     * @param buf the place to put the data.
     * @param offset the starting offset in buf to store the data.
     * @len the number of characters to read.
     * @return the number of characters actually read, or -1 for eof.
     * @exception IOException if an I/O error occurs on the underlying input
     * stream.
     */
    public int read(char[] buf, int offset, int len) throws IOException {
        if (atEOF) {
            return -1;
        }
        for (int i = 0; i < len; i++) {
            int c = read();
            if (c < 0) {
                return i > 0 ? i : -1;
            }
            buf[offset + i] = (char) c;
        }
        return len;
    } // read(byte[], int, int)

    /** Throw away all remaining input and close the underlying input stream.
     * @exception IOException if an I/O error occurs on the underlying input
     * stream.
     */
    public void close() throws IOException {
        in.close();
        atEOF = true;
    }

    /** Return the number of "raw" bytes read from the original input stream,
     * including Cedar overhead.
     * @return the number of bytes read.
     */
    public int getByteCount() {
        return in.getByteCount();
    }
} // ClassAdReader
