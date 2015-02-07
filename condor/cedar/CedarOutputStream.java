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

/** A Java output stream that more-or-less corresponds to the C++ class
 * ReliSock.
 * Except for floats, this class attempts to be compatible with the existing
 * Condor conventions for sending data over a connection.
 * See {@link <a href="package-summary.html#package_description">The package description</a>} for details.
 * @author Marvin solomon
 * @version $Revision: 1.11 $
 */
public class CedarOutputStream
        extends FilterOutputStream implements DataOutput
{
    private static String VERSION = "$Id: CedarOutputStream.java,v 1.11 2003/08/04 01:13:04 solomon Exp $";

    /** The default maximum size of a packet (8192 bytes). */
    public static final int DEFAULT_PACKET_SIZE = 8192;

    /** A buffer for outgoing packets. */
    private byte[] buffer;

    /** A buffer of nulls, for convenience. */
    private byte[] nulls = new byte[8];

    /** A buffer of ones, for sign-extending outputs */
    private byte[] ones = {
        (byte) -1, (byte) -1, (byte) -1, (byte) -1,
        (byte) -1, (byte) -1, (byte) -1, (byte) -1};

    /** The index of the first unused position in buffer. */
    private int bufpos;

    /** End of message flag for the buffer previously sent. */
    private boolean prevEom;

    //========== Constructors ================================================

    /** Creates a new CedarOutputStream wrapping an exising stream.
     * @param out the destination for raw bytes.
     */
    public CedarOutputStream(OutputStream out) {
        super(out);
        buffer = new byte[DEFAULT_PACKET_SIZE];
    } // CedarOutputStream(OutputStream)

    /** Creates a new CedarOutputStream wrapping an exising stream with a given
     * maximum packet size.
     * @param out the destination for raw bytes.
     * @param packetSize the maximum size of packet to send to stream "out".
     */
    public CedarOutputStream(OutputStream out, int packetSize) {
        super(out);
        buffer = new byte[packetSize];
    } // CedarOutputStream(OutputStream, int)

    //========== Methods overridden from FilterOututStream ====================

    /** Writes one byte to the output.
     * @param b the byte to write.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public void write(int b) throws IOException {
        if (bufpos >= buffer.length) {
            flush();
        }
        buffer[bufpos++] = (byte)b;
    } // write(int)

    /** Writes some data to the output.  Equivalent to write(b, 0, b.length).
     * @param b the data to write.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    } // write(byte[])

    /** Writes some data to the output. 
     * @param b the data to write.
     * @param off the starting location in the buffer.
     * @param len the number of bytes to write.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public void write(byte[] b, int off, int len) throws IOException {
        int bytesLeft = len;
        while (bytesLeft > 0) {
            int spaceLeft = buffer.length - bufpos;
            if (spaceLeft == 0) {
                flush();
                spaceLeft = buffer.length;
            }
            int n = bytesLeft;
            if (n > spaceLeft) {
                n = spaceLeft;
            }
            System.arraycopy(b, off, buffer, bufpos, n);
            bufpos += n;
            bytesLeft -= n;
            off += n;
        }
    } // write(byte[], int, int)

    /** Sends the contents of the current output buffer as a packet.
     * If the current output buffer is empty, no packet is sent.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public void flush() throws IOException {
        flush(false);
    } // flush()

    /** Flushs buffered data as a packet.  If eomFlag is true, marks the packet
     * as end of message and sends it, even if it is empty.  If eomFlag is
     * false, only sends a packet if there is something to send.
     * @param eomFlag the end of message flag to include in the packet.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    private void flush(boolean eomFlag) throws IOException {
        if (bufpos == 0) {
            if (eomFlag) {
                out.write(1);
                out.write(nulls, 0, 4);
                prevEom = true;
            }
            return;
        }
        out.write(eomFlag ? 1 : 0);
        for (int i = 3; i >= 0; i--) {
            out.write(bufpos >> (8*i));
        }
        out.write(buffer, 0, bufpos);
        bufpos = 0;
        prevEom = eomFlag;
    } // flush(boolean)

    /** Flushs any buffered data, indicate end of message, and closes the
     * underlying output stream.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public void close() throws IOException {
        if (!prevEom || bufpos > 0) {
            flush(true);
        }
        out.close();
    } // close()

    /** Sends an end of message indication, as a separate empty packet, if
     * necessary.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public void endOfMessage() throws IOException {
        flush(true);
    } // endOfMessage()

    /** Sends a boolean as a long integer (1 for true, 0 for false).
     * @param v the data to send.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public void writeBoolean(boolean v) throws IOException {
        write(nulls, 0, 7);
        write(v ? 1 : 0);
    } // writeBoolean(boolean)

    /** Sends a byte as is.
     * @param v the data to send.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public void writeByte(int v) throws IOException {
        write(v);
    } // writeByte(int)

    /** Sends a short as a long integer, sign-extended.
     * @param v the data to send.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public void writeShort(int v) throws IOException {
        write((v < 0 ? ones : nulls), 0, 6);
        write(v >> 8);
        write(v);
    } // writeShort(int)

    /** Sends a char as a long integer, zero-extended.
     * @param v the data to send.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public void writeChar(int v) throws IOException {
        write(nulls, 0, 6);
        write(v >> 8);
        write(v);
    } // writeChar(int)


    /** Sends an integer as a long integer, sign-extended.
     * @param v the data to send.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public void writeInt(int v) throws IOException {
        write((v < 0 ? ones : nulls), 0, 4);
        write(v >> 24);
        write(v >> 16);
        write(v >> 8);
        write(v);
    } // writeInt(int)

    /** Sends a long integer, big endian (MSB first).
     * @param v the data to send.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public void writeLong(long v) throws IOException {
        for (int i = 7; i >= 0; i--) {
            write((int) (v >> (8 * i)));
        }
    } // writeLong(long)

    /** Sends a float, big endian (MSB first).
     * @param v the data to send.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public void writeFloat(float v) throws IOException {
        int tmp = Float.floatToIntBits(v);
        for (int i = 3; i >= 0; i--) {
            write(tmp >> (8 * i));
        }
    } // writeFloat(float)

    /** Sends a double, big endian (MSB first).
     * @param v the data to send.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    } // writeDouble(double)

    /** Sends the low order byte of each character in the string.
     * Why isn't this deprecated?
     * @param v the data to send.
     * @deprecated Use {@link #writeUTF(java.lang.String)} instead.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public void writeBytes(String v) throws IOException {
        int len = v.length();
        for (int i = 0; i < len; i++) {
            write(v.charAt(i));
        }
    } // writeBytes(String)

    /** Sends each character as two bytes, high byte first.
     * @param v the data to send.
     * @deprecated Use {@link #writeUTF(java.lang.String)} instead.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public void writeChars(String v) throws IOException {
        int len = v.length();
        for (int i = 0; i < len; i++) {
            int c = v.charAt(i);
            write(c >> 8);
            write(c);
        }
    } // writeChars(String)

    /** Sends the string encoded using the Java version of UTF-8, followed by
     * a null byte.  Note that the Java version of UTF-8 never produces a
     * null byte.
     * @param v the data to send.
     * @throws IOException if there is an I/O error on the underlying stream.
     */
    public void writeUTF(String v) throws IOException {
        int len = v.length();
        for (int i = 0; i < len; i++) {
            int c = v.charAt(i);
            if (c > 0 && c < 128) { // 7 or fewer bits
                write(c);                           // all 7 bits
            } else if (c < 0x800) { // 11 or fewer bits
                write(0xc0 | ((c >> 6) & 0x1f));    // high 5 bits
                write(0x80 | (c & 0x3f));           // low 6 bits
            } else {
                write(0xe0 | ((c >> 12) & 0x0f));   // high 4 bits
                write(0x80 | ((c >> 6) & 0x3f));    // next 6 bits
                write(0x80 | (c & 0x3f));           // next 6 bits
            }
        }
        write(0);
    } // writeUTF(String)
} // CedarOutputStream
