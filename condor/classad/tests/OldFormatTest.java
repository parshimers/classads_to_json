// $Header: /p/condor/repository/CONDOR_SRC/src_java/condor/classad/tests/OldFormatTest.java,v 1.6 2005/05/06 20:53:31 solomon Exp $

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

package condor.classad.tests;

import condor.classad.*;
import condor.cedar.*;
import java.io.*;

/** Ad hoc test for {@link condor.classad.RecordExpr#transmit(DataOutput)}
 * and {@link condor.classad.ClassAdReader}.
 * @author <a href="mailto:solomon@cs.wisc.edu">Marvin Solomon</a>
 */
public class OldFormatTest {
    private static void pl(Object o) { System.err.println(o); }
    /** Main program.
     * @param args ignored.
     */
    public static void main(String[] args) {
        try {
            ClassAdParser parser
                = new ClassAdParser(new FileInputStream("testads"));
            CedarOutputStream out
                = new CedarOutputStream(new FileOutputStream("tmp1"));
            RecordExpr expr;
            for (;;) {
                expr = (RecordExpr) parser.parse();
                if (expr == null)
                     break;
                pl("dumping " + expr);
                out.writeInt(1);
                try {
                    expr.transmit(out);
                } catch (InvalidObjectException e) {
                    pl(e);
                }
                int lookahead = parser.getNextToken();
                if (lookahead != ';') {
                    System.out.println("Expecting semicolon, found "
                        + lookahead);
                    break;
                }
            }
            out.writeInt(0);
            out.close();
            pl("=============");

            CedarInputStream cin
                = new CedarInputStream(new FileInputStream("tmp1"));
            ClassAdReader rdr = new ClassAdReader(cin);
            for (;;) {
                int c = rdr.read();
                if (c == -1)
                    break;
                System.err.print((char) c);
            }
            System.err.println();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
