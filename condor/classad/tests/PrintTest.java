// $Header: /p/condor/repository/CONDOR_SRC/src_java/condor/classad/tests/PrintTest.java,v 1.4 2005/05/06 20:53:31 solomon Exp $

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
import java.util.*;
import java.io.*;

/** Simple test program to test parsing and printing classads.
 * Input consists of a sequence of classads separated by semicolons.
 */
class PrintTest {
    /** Print a usage message and exit. */
    private void usage() {
        System.err.println("usage: java "
                        + getClass().getName() + " [-v [debug_level]] file");
        System.exit(1);
    } // usage()

    /** The main program.
     * @param args the command-line arguments:
     *  <code>[-v [debug_level]] file</code>
     */
    public static void main(String[] args) {
        int verbosity = 0; // amount of debugging output
        GetOpt opts = new GetOpt(getClass().getName(), args, "v::");
    opt_loop:
        for (;;) {
            switch (opts.nextOpt()) {
            default: usage();
            case 'v': // More verbose
                if (opts.optarg == null) {
                    verbosity++;
                } else {
                    verbosity = Integer.parseInt(opts.optarg);
                }
                break;
            }
        }

        if (opts.optind !=  args.length - 1) {
            usage();
        }

        Reader input = null;
        if (args.length > 0) {
            try {
                input = new FileReader(args[0]);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        } else {
            input = new InputStreamReader(System.in);
        }
        ClassAdParser parser = new ClassAdParser(input);
        //parser.enableTracing(true);
        ClassAdWriter out = new ClassAdWriter(System.out);

        // main loop
        for(;;) {
            Expr expr = parser.parse();
            if (expr == null) {
                break;
            }
            out.println(expr.toString());
            out.println(expr);
            out.flush();
            int lookahead = parser.getNextToken();
            if (lookahead != ';') {
                out.println("Expecting semicolon, found " + lookahead);
                return;
            }
        } // for (;;)
    } // main(String[])
} // PrintTest
