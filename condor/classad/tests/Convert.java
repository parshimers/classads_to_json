// $Header: /p/condor/repository/CONDOR_SRC/src_java/condor/classad/tests/Convert.java,v 1.7 2005/05/06 20:53:31 solomon Exp $

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

/** Program to demonstrate how to convert classads from one format to another.
 * <p>
 * usage:
 * <pre>
 *      java condor.classad.tests.Convert [ options ] file
 * </pre>
 * Results go to standard output.
 * <p>Options are
 * <dl>
 * <dt>-vlevel  <dd>Set verbosity of debugging output to <em>level</em>
 * <dt>-v       <dd>Add one to the verbosity level
 * <dt>-i F     <dd>Input format is F.
 * <dt>-o F     <dd>Output format is F.
 * <dt>-l       <dd>Make output more readable by adding line-breaks and
 *                  indentation.
 * </dl>
 * Format F is currently one of
 * <dl>
 * <dt>t        <dd>"text" format (e.g. [ foo = 10; ]) (default).
 * <dt>x        <dd> XML format (e.g. <ca> <a name="foo"><n>10</n></a></ca>).
 * <dt>n        <dd> none (output only, for timing tests).
 * </dl>
 *
 * @author <a href="mailto:solomon@cs.wisc.edu">Marvin Solomon</a>
 */
public class Convert {
    /** The main program.
     * @param args command-line arguments.
     */
    public static void main(String[] args) {
        new Convert().run(args);
    } // main

    /** Stream for printing test output.  */
    ClassAdWriter out;

    /** Print a usage message and exit. */
    private void usage() {
        System.err.println("usage: java " + getClass().getName()
            + " [-v [level]] [-i format] [-o format] [-l] file");
        System.exit(1);
    }

    /** The main program as a non-static method.
     * @param args command-line arguments.
     */
    private void run(String args[]) {
        // Command-line flags
        int verbosity = 0;
        boolean
            xmlInput = false,
            lineBreaks = false,
            dumpOutput = false;
        int outputFormat = ClassAdWriter.NATIVE;
        int outputFlags = ClassAdWriter.MINIMAL_PARENTHESES;

        // Parse the command line

        GetOpt opts = new GetOpt("fetch", args, "v::i:o:l");
        int c;
    opt_loop:
        for (;;) switch(opts.nextOpt()) {
            case 'v': // More verbose
                if (opts.optarg == null) {
                    verbosity++;
                } else {
                    verbosity = Integer.parseInt(opts.optarg);
                }
                break;
            case 'i':
                switch (opts.optarg.charAt(0)) {
                case 'x': xmlInput = true; break;
                case 't': xmlInput = false; break;
                default: usage();
                }
                break;
            case 'o':
                switch (opts.optarg.charAt(0)) {
                case 'x': outputFormat = ClassAdWriter.XML; break;
                case 't': outputFormat = ClassAdWriter.NATIVE; break;
                case 'n': dumpOutput = true; break;
                default: usage();
                }
                break;
            case 'l':
                outputFlags |= ClassAdWriter.MULTI_LINE_ADS
                             | ClassAdWriter.MULTI_LINE_LISTS;
                break;
            case -1:
                break opt_loop;
            default: usage();
        }
        if (verbosity > 0) {
            outputFlags |= ClassAdWriter.SHOW_ERROR_DETAIL;
        }

        if (opts.optind != args.length - 1)
            usage();

        String fname = args[opts.optind];
        ClassAdWriter out = new ClassAdWriter(System.out, outputFormat, true);
        out.setFormatFlags(outputFlags);

        try {
            if (false &&outputFormat == ClassAdWriter.XML) {
                // some header boiler plate for a file of classads
                out.println("<?xml version=\"1.0\"?>");
                out.println("<!DOCTYPE classads SYSTEM"
                    + " \"http://www.cs.wisc.edu/~roy/classads.dtd\">");
                out.println("<classads>");
            }

            Reader in
                = new InputStreamReader(
                    new BufferedInputStream(
                        new FileInputStream(
                            fname)));
            if (xmlInput) {
                ClassAdParser parser =
                    new ClassAdParser(in, ClassAdParser.XML);
                if (!dumpOutput) {
                    out.println(parser.parse());
                }
            } else {
                ClassAdParser parser =
                    new ClassAdParser(in, ClassAdParser.TEXT);
                for (;;) {
                    Expr next = parser.parse();
                    if (next == null) {
                        break;
                    }
                    if (!dumpOutput) {
                        out.println(next);
                    }

                    int lookahead = parser.getNextToken();
                    if (lookahead != ';') {
                        System.err.println(
                            "Expecting semicolon, found " + lookahead);
                        break;
                    }
                }
            }

            if (outputFormat == ClassAdWriter.XML) {
                // out.println("</classads>");
                out.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    } // run
} // Convert
