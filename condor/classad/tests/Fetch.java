// $Header: /p/condor/repository/CONDOR_SRC/src_java/condor/classad/tests/Fetch.java,v 1.14 2005/05/06 20:53:31 solomon Exp $

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
import java.net.*;
import java.util.Date;

/** Main program to fetch classads from the Condor collector and save them to a
 * file.  This program
 * <ol>
 * <li> Uses ClassAdParser to parse a trivial classad.
 * <li> Connects the the Condor collector and sets up Cedar streams to/from
 *      collector.
 * <li> Transmits the ad from step 1 to the collector to request a full dump
 *      of all STARTD ads.  The ad is sent in the "old" classad format,
 *      using RecordExpr.transmit().
 * <li> Creates a ClassAdReader to convert the response (a sequence of ads
 *      in "old" format) to ASCII format.
 * <li> Dumps the result to stdout.
 * </ol>
 * Its main purpose is to serve as an example how to do these things with
 * the packages condor.cedar and condor.classad and to measure the performance.
 * <p>
 * usage:
 * <pre>
 *      java condor.classad.tests.Fetch [ options ] command output-file
 * </pre>
 * Options are
 * <dl>
 * <dt>-vlevel<dd>Set verbosity of debugging output to <em>level</em>
 * <dt>-v<dd>Add one to the verbosity level
 * <dt>-h host<dd>Name of host running the collector
 * <dt>-m count<dd>Only save the first <em>count</em> adds
 * <dt>-r<dd>Store the results in "raw" (not ASCII) format
 * <dt>-d<dd>Discard the output (for timing purposes)
 * </dl>
 * @author <a href="mailto:solomon@cs.wisc.edu">Marvin Solomon</a>
 */
public class Fetch {
    /** Default host for Condor collector. */
    public static String server = "condor.cs.wisc.edu";

    /** Default port for Condor collector. */
    public static final int COLLECTOR_COMM_PORT    = 9618;

    private static class Op {
        public String mnemonic;
        public int code;
        public String type;
        private Op(String mnemonic, int code, String type) {
            this.mnemonic = mnemonic;
            this.code = code;
            this.type = type;
        } // Op constructor
        public static Op[] table = {
            // This information is gathered by hand from 
            //   src/condor_includes/condor_adtypes.h
            // and
            //   src/condor_includes/condor_commands.h
            new Op("QUERY_STARTD_ADS",      5, "Machine"),
            new Op("QUERY_SCHEDD_ADS",      6, "Scheduler"),
            new Op("QUERY_MASTER_ADS",      7, "DaemonMaster"),
            new Op("QUERY_GATEWAY_ADS",     8, "??"),
            new Op("QUERY_CKPT_SRVR_ADS",   9, "CkptServer"),
            new Op("QUERY_STARTD_PVT_ADS",  10, "??"),
            new Op("QUERY_SUBMITTOR_ADS",   12, "Submitter"),
            new Op("QUERY_COLLECTOR_ADS",   20, "Collector"),
            new Op("QUERY_LICENSE_ADS",     43, "License"),
            new Op("QUERY_STORAGE_ADS",     46, "Storage"),
            new Op("QUERY_ANY_ADS",         48, "Any"),
            // The following types in condor_adtypes do not seem to correspond
            // to any command:
            //   "Job"
            //   "Query"
            //   "CkptFile"
            //   "Authentication"
            // The following commands from condor_commands do not seem to
            // correspond to any ad type.
            //   QUERY_GATEWAY_ADS     = 8;
            //   QUERY_STARTD_PVT_ADS  = 10;
            //   QUERY_HIST_STARTD = 22;
            //   QUERY_HIST_STARTD_LIST = 23;
            //   QUERY_HIST_SUBMITTOR = 24;
            //   QUERY_HIST_SUBMITTOR_LIST = 25;
            //   QUERY_HIST_GROUPS = 26;
            //   QUERY_HIST_GROUPS_LIST = 27;
            //   QUERY_HIST_SUBMITTORGROUPS = 28;
            //   QUERY_HIST_SUBMITTORGROUPS_LIST = 29;
            //   QUERY_HIST_CKPTSRVR = 30;
            //   QUERY_HIST_CKPTSRVR_LIST = 31;
        }; // Op.table
        static public Op find(String name) {
            for (int i = 0; i < table.length; i++) {
                if (name.equalsIgnoreCase(table[i].type)) {
                    return table[i];
                }
            }
            return null;
        } // Op.find
    } // class Op

    /** The main program.
     * @param args command-line arguments.
     */
    public static void main(String[] args) {
        // ClassAdParser.disable_tracing();
        new Fetch().run(args);
    } // main

    /** Convenience procedure for debugging output.
     * @param s a message to print to System.err.
     */
    private static void pl(Object s) {
        System.err.println(s); System.err.flush();
    } // pl

    /** Convenience procedure for debugging output.
     * @param s a message to print to System.err.
     */
    private static void p(Object s) {
        System.err.print(s); System.err.flush();
    } // p

    /** Print a usage message and exit. */
    private void usage() {
        System.err.println("usage: java "+getClass().getName()
            + " [-v [level]] [-h host] [-m count] [-r] [-d] op fname");
        System.err.println("where op is one of");
        for (int i = 0; i < Op.table.length; i++) {
            System.err.println("   " + Op.table[i].type);
        }
        System.exit(1);
    } // usage

    /** The main program as a non-static method.
     * @param args command-line arguments.
     */
    private void run(String args[]) {
        // Command-line flags
        int verbosity = 0;
        int maxAds = -1; // number of ads to fetch (-1 means unlimited)
        boolean rawFlag = false;
        boolean discard = false;

        // Parse the command line

        GetOpt opts = new GetOpt("fetch", args, "v::h:m:rd");
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
            case 'm': // Maxinum number of ads to fetch
                maxAds = Integer.parseInt(opts.optarg);
                break;
            case 'h': // server host
                server = opts.optarg;
                break;
            case 'r': // raw
                rawFlag = true;
                break;
            case 'd': // discard output
                discard = true;
                break;
            case -1:
                break opt_loop;
            default: usage();
        }
        if (opts.optind != args.length - 2)
            usage();

        OutputStream out = null;
        if (!discard) try {
            out = new BufferedOutputStream(
                    new FileOutputStream(args[opts.optind + 1]));
        } catch (Exception e) {
            pl(e.getMessage());
            System.exit(1);
        }

        String arg = args[opts.optind];
        Op op = Op.find(arg);
        if (op == null) {
            pl("unknown resource class " + arg);
            System.exit(1);
        }

        // Generate an ad to send to the collector
        RecordExpr ad;
        ad = (RecordExpr)new ClassAdParser(
            "[Rank = 0; MyType = \"Query\"; TargetType = \""
            + op.type + "\"; Requirements = 1; ]").parse();

        if (verbosity > 0)
            pl("Sending command " + op.mnemonic + "(" + op.code
            + "), ad " + ad);

        // Connect to the server
        Socket sock = null;
        CedarOutputStream toHost = null;
        try {
            if (verbosity > 0)
                p("Connecting to host '" + server
                + "', port " + COLLECTOR_COMM_PORT + "...");
            sock = new Socket(server, COLLECTOR_COMM_PORT);
            if (verbosity > 0)
                pl(verbosity > 1 ? ("Connected: " + sock) : "");
            toHost = new CedarOutputStream(sock.getOutputStream());
        }
        catch (Exception e) {
            pl("Connection failed: " + e);
            System.exit(1);
        }

        // Send the command and the ad
        try {
            toHost.writeInt(op.code);
            ad.transmit(toHost);
            toHost.endOfMessage();
        }
        catch (Exception e) {
            pl("failure sending query:");
            e.printStackTrace(System.err);
            System.exit(1);
        }

        // Process the returned data
        Date start = new Date();
        int bytesRead = 0;
        try {
            if (rawFlag) {
                InputStream in
                    = new BufferedInputStream(sock.getInputStream());
                byte[] buffer = new byte[8192];
                for (;;) {
                    if (verbosity > 0)
                        p(".");
                    int n = in.read(buffer);
                    if (n < 0)
                        break;
                    if (!discard)
                        out.write(buffer, 0, n);
                    bytesRead += n;
                }
            } else {
                // Clean up the response
                ClassAdReader rdr
                    = new ClassAdReader(
                        new CedarInputStream(sock.getInputStream()));
                int adCount = 0;
                for (;;) {
                    int n = rdr.read();
                    if (n == -1)
                        break;
                    if (!discard)
                        out.write(n);
                    bytesRead++;
                    if (verbosity > 0 && (bytesRead % 1024) == 0)
                        p(".");
                    if (maxAds >= 0) {
                        // Count the ads output thus far.
                        // We simply count the number of close brackets,
                        // assuing that they appear only at the ends of
                        // top-level ads (not in nested ads or quoted strings).
                        if (n == ']') {
                            if (++adCount >= maxAds) {
                                out.write(';');
                                out.write('\n');
                                break;
                            }
                        }
                    }
                }
            } // not rawFlag
            out.close();
            if (verbosity > 0)
                pl("");
            long elapsed = (new Date()).getTime() - start.getTime();
            float rate = (float)((double)(1000*bytesRead)/elapsed);
            pl(bytesRead + " bytes read in " + elapsed + " ms ("
                + rate + " bps)");
        }
        catch (Exception e) {
            pl("failure retrieving result: " + e);
            e.printStackTrace(System.err);
        }
    } // run
} // Fetch
