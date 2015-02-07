package condor.cedar;

/* **************************Copyright-DO-NOT-REMOVE-THIS-LINE**
 * Condor Copyright Notice
 *
 * See LICENSE.TXT for additional notices and disclaimers.
 *
 * Copyright (c)1990-2003 Condor Team, Computer Sciences Department,
 * University of Wisconsin-Madison, Madison, WI.  All Rights Reserved.
 * Use of the CONDOR Software Program Source Code is authorized
 * solely under the terms of the Condor Public License (see LICENSE.TXT).
 * For more information contact:
 * CONDOR Team, Attention: Professor Miron Livny,
 * 7367 Computer Sciences, 1210 W. Dayton St., Madison, WI 53706-1685,
 * (608) 262-0856 or miron@cs.wisc.edu.
 * ***************************Copyright-DO-NOT-REMOVE-THIS-LINE**/

import java.io.*;

/** A Java input stream that more-or-less corresponds to the C++ class ReliSock.
 * Except for floats, this class attempts to be compatible with the existing
 * Condor conventions for sending data over a connection.
 * See {@link <a href="package-summary.html#package_description">The package description</a>} for details.
 * @author Marvin solomon
 * @version $Revision: 1.14 $
 */
public class CedarInputStream extends FilterInputStream implements DataInput {
    private static String VERSION = "$Id: CedarInputStream.java,v 1.14 2003/08/04 01:13:04 solomon Exp $";

    /** debugging hack */
    private static void pl(Object o) { System.err.println(o); }

    /** A buffer for incoming packets, allocated on demand. */
    private byte[] buffer = new byte[0];

    /** A small temporary buffer */
    private byte[] tmpbuf = new byte[8];

    /** A string buffer used by readLine() and readUTF() */
    private StringBuffer sb = new StringBuffer();

    /** The index of the first unread byte in buffer. */
    private int bufpos;

    /** The number of bytes of data in buffer */
    private int buflen;

    /** Flag to indicate that the current input packet was sent with its
     * end-of-message flag set.
     */
    private boolean eomFlag;

    /** A count of bytes read from the underlying InputStream. */
    private int byteCount = 0;

    /** Creates a new CedarInputStream wrapping an exising stream.
     * @param in the source of raw bytes.
     */
    public CedarInputStream(InputStream in) {
        super(in);
    } // CedarInputStream(InputStream)

    /** Advances the input stream to the start of the next message.
     * If the stream is already in the last message, the buffer is left looking
     * as if the stream is positioned inside a zero-length message.
     * Note that this is the only method of this class that will read beyond
     * the end of the current message.
     * @return false if the stream is positioned inside the last message of
     * input at the point of call.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public boolean nextMessage() throws IOException {
        // Advance to the end of the current message
        while (!eomFlag) {
            if (!fillBuf()) {
                break;
            }
        }
        if (eomFlag) {
            // get the first packet of the next message
            eomFlag = false;
            if (fillBuf()) {
                return true;
            }
        }
        // We get here either because we hit EOF trying to find the end of the
        // current message (which shouldn't happen), or because we hit eof
        // trying to read the first packet of the next message.  In either
        // case, leave the state looking like we are in a zero-length message.
        eomFlag = true;
        buflen = 0;
        bufpos = 0;
        return false;
    } // nextMessage()

    //========== Methods overridden from FilterInputStream ====================

    /** Reads one byte from the input stream.
     * @return the next input byte, or -1 if the stream is positioned at
     * end-of-message.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public int read() throws IOException {
        while (bufpos >= buflen) {
            if (!fillBuf()) {
                return -1;
            }
        }
        return buffer[bufpos++] & 0xff;
    } // read()
        
    /** Reads at most b.length bytes from the input stream.
     * @param b the place to put the data.
     * @return the number of bytes read.  Returns -1 if the stream is at
     * end-of-message.  Never returns 0.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    } // read(byte[])

    /** Reads at most len bytes from the input stream into buffer b,
     * starting at offset off.
     * @param b the place to put the data.
     * @param off starting offset in b.
     * @param len maximum number of bytes to read.
     * @return the number of bytes read.  Returns -1 if the stream is at
     * end-of-message.  Never returns 0.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public int read(byte b[], int off, int len) throws IOException {
        while (bufpos >= buflen) {
            if (!fillBuf()) {
                return -1;
            }
        }
        if (len > buffer.length - bufpos) {
            len = buffer.length - bufpos;
        }
        System.arraycopy(buffer, bufpos, b, off, len);
        bufpos += len;
        return len;
    } // read(byte[], int, int)

    /** Skips up n bytes or to the end of the current message, whichever
     * comes first.
     * @param n the maximum number of bytes to skip.
     * @return the number of bytes skipped.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public long skip(long n) throws IOException {
        // We actually read all the data and throw it away.  This could
        // be made more efficient by calling in.skip(), but nobody uses
        // this method, so what's the point?
        long toSkip = n;
        while (toSkip > 0) {
            if (buflen - bufpos <= toSkip) {
                bufpos += toSkip;
                return n;
            }
            toSkip -= buflen - bufpos;
            bufpos = buflen;
            if (!fillBuf()) {
                return n - toSkip;
            }
        }
        return n;
    } // skip(long)

    /** Returns the number of bytes buffered.  If no data is currently
     * buffered, first read until there is some data buffered, or
     * end-of-message is encountered, whichever comes first.
     * @return the number of bytes buffered, perhaps after first refilling the
     * buffer.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public int available() throws IOException {
        while (bufpos >= buflen) {
            if (!fillBuf()) {
                return 0;
            }
        }
        return buflen - bufpos;
    } // available()

    /** Closes the underlying stream and discards all buffered data.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public void close() throws IOException {
        bufpos = 0;
        buflen = 0;
        in.close();
    } // close()

    /** Does nothing (mark not supported). */
    public void mark(int n) {
    } // mark(int)

    /** Always throws IOException (mark not supported).
     * @throws IOException always.
     */
    public void reset() throws IOException {
        throw new IOException("mark not supported");
    } // reset()

    /** Always returns false (mark not supported). */
    public boolean markSupported() {
        return false;
    } // markSupported()

    //============ Methods specified by DataInput ====================

    /** Equivalent to readFully(b, 0, b.length).
     * @param b a place to put the data.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public void readFully(byte b[]) throws IOException {
        readFully(b, 0, b.length);
    } // readFully(byte[])

    /** Reads a specified number of bytes, or throws an exception if there
     * are not that many more bytes before the next end-of-message indication.
     * @param b place to put the data.
     * @param off offset within b where the data starts.
     * @param len number of bytes to read.
     * @throws EOFException if end-of-message is encountered before the
     * specified number of bytes are read.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public void readFully(byte b[], int off, int len) throws IOException {
        int bytesRead = 0;
        while (bytesRead < len) {
            int count = read(b, off + bytesRead, len - bytesRead);
            if (count < 0)
                throw new EOFException();
            bytesRead += count;
        }
    } // readFully(byte[], int, int)

    /** Equivalent to (int) skip(n).
     * @param n the maximum number of bytes to skip.
     * @return the number of bytes skipped.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public int skipBytes(int n) throws IOException {
        return (int) skip(n);
    } // skipBytes(int)

    /** Reads one boolean from the input stream.
     * @return the value read.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public boolean readBoolean() throws IOException {
        return readLong() != 0;
    } // readBoolean()

    /** Reads a byte from the input stream.
     * @return the value read.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public byte readByte() throws IOException {
        int b = read();
        if (b < 0) {
            throw new EOFException();
        }
        return (byte) b;
    } // readByte()

    /** Reads an unsigned byte from the input stream.
     * @return the value read.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public int readUnsignedByte() throws IOException {
        int b = read();
        if (b < 0) {
            throw new EOFException();
        }
        return b;
    } // readUnsignedByte()

    /** Reads a short from the input stream.
     * @return the value read.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public short readShort() throws IOException {
        return (short) readLong();
    } // readShort()

    /** Reads an unsigned short from the input stream.
     * @return the value read.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public int readUnsignedShort() throws IOException {
        return 0xffff & (int) readLong();
    } // readUnsignedShort()

    /** Reads one character from the input stream.
     * @return the value read.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public char readChar() throws IOException {
        return (char) readLong();
    } // readChar()

    /** Reads an integer from the input stream.
     * @return the value read.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public int readInt() throws IOException {
        return (int) readLong();
    } // readInt()

    /** Reads a long (64-bit) integer from the input stream.
     * @return the value read.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public long readLong() throws IOException {
        readFully(tmpbuf);
        return
            ((long)(tmpbuf[0] & 0xff) << 56) |
            ((long)(tmpbuf[1] & 0xff) << 48) |
            ((long)(tmpbuf[2] & 0xff) << 40) |
            ((long)(tmpbuf[3] & 0xff) << 32) |
            ((long)(tmpbuf[4] & 0xff) << 24) |
            ((long)(tmpbuf[5] & 0xff) << 16) |
            ((long)(tmpbuf[6] & 0xff) <<  8) |
            ((long)(tmpbuf[7] & 0xff));
    } // readLong()

    /** Reads a float from the input stream.
     * @return the value read.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public float readFloat() throws IOException {
        readFully(tmpbuf, 0, 4);
        int tmp =
                    ((tmpbuf[0] & 0xff) << 24) |
                    ((tmpbuf[1] & 0xff) << 16) |
                    ((tmpbuf[2] & 0xff) <<  8) |
                     (tmpbuf[3] & 0xff);
        return Float.intBitsToFloat(tmp);
    } // readFloat()

    /** Reads a double from the input stream.
     * @return the value read.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    } // readDouble()

    /** Reads bytes, converting them to characters, until end-of-line or
     * end-of-message is seen.  See DataInput.readLine() for details.
     * It is not clear to me why this ridiculous thing is not deprecated.
     * @return a string composed of the characters read.
     * @throws IOException if there is an I/O error on the underlying stream.
     * @deprecated Use {@link #readUTF()} instead.
     */
    public String readLine() throws IOException {
        sb.setLength(0);
        for (;;) {
            int c = read();
            if (c < 0 || c == '\n') {
                break;
            }
            if (c == '\r') {
                c = read();
                if (c != '\n') {
                    // NB:  If read() returned a non-zero result, it must
                    // done bufpos++ as its last operations, so the following
                    // will not cause an exception.
                    buffer[--bufpos] = (byte) c;
                }
                break;
            }
            sb.append((char) c);
        }
        if (sb.length() == 0) {
            return null;
        }
        return sb.toString();
    } // readLine()

    /** Reads a null-terminated string in modified UTF-8 encoding from the
     * input and throw away the terminating null.
     * @return the value read.
     * @throws UTFDataFormatException if the input data contains a bad
     * escape sequece.
     * @throws EOFException if end-of-message is encountered before a null.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public String readUTF() throws IOException {
        sb.setLength(0);
        for (;;) {
            int a = readUTFchar();
            if (a < 0) {
                break;
            }
            sb.append((char ) a);
        }
        return sb.toString();
    } // readUTF()

    /** Reads one character in UTF format from the input.
     * @return the character read, or -1 if a null is encountered in the
     * input stream (note that a null in the input stream cannot be part of the
     * UTF encoding of any character).
     * @throws UTFDataFormatException if the input data contains a bad
     * escape sequece.
     * @throws EOFException if end-of-message is encountered.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public final int readUTFchar() throws IOException {
        int a = read();
        if (a < 0) {
            throw new EOFException();
        }
        if (a == 0) {
            return -1;
        }
        if ((a & 0x80) != 0) {
            int b = read();
            if (b < 0) {
                throw new UTFDataFormatException();
            }
            switch (b & 0xf0) {
            case 0xc0:
            case 0xd0:
                a = ((a & 0x1f) << 6) | (b & 0x3f);
                break;
            case 0xe0:
                int c = read();
                if (c < 0) {
                    throw new UTFDataFormatException();
                }
                a = ((a & 0xf) << 12) | ((b & 0x3f) << 6) | (c & 0x3f);
                break;
            default:
                throw new UTFDataFormatException();
            }
        }
        return a;
    } // readUTFchar()

    /** Reads a packet of data into buffer, setting eomFlag, bufpos, and
     * buflen.  Any existing data in the buffer is discarded.  If end-of-file
     * or end-of-message is encountered, leave the buffer empty (zero length).  
     * @return false if end-of-file is encountered.
     * @throws EOFException if end-of-file is encountered on the underlying
     * stream after reading at least one byte of the next packet header
     * (indicates a truncated packet).
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    private boolean fillBuf() throws IOException {
        buflen = 0;
        bufpos = 0;
        if (eomFlag) {
            return false; // end-of-message encountered
        }
        int c = in.read();
        if (c < 0) {
            return false; // end-of-file encountered
        }
        byteCount++;
        eomFlag = c != 0;
        for (int i = 0; i < 4; i++) {
            c = in.read();
            if (c < 0) {
                // Protocol error:  Truncated packet header.
                // Just treat it like normal EOF.
                return false;
            }
            byteCount++;
            buflen = (buflen << 8) + c;
        }
        // Make sure buffer is big enough.
        if (buffer.length < buflen) {
            buffer = new byte[buflen];
        }
        // Fill the buffer with data.
        // Too bad there's no InputStream.readFully()!
        int pos = 0;
        while (pos < buflen) {
            int n = in.read(buffer, pos, buflen - pos);
            if (n < 0) {
                // Protocol error:  Truncated packet body.
                // Just treat it like normal EOF.
                return false;
            }
            pos += n;
            byteCount += n;
        }
        return true;
    } // fillBuf()

    /** Returns the number of "raw" bytes consumed from the original input
     * stream, including Cedar overhead, since this stream was opened.
     * @return the number of bytes read.
     */
    public int getByteCount() {
        return byteCount;
    } // getByteCount()
} // CedarInputStream
