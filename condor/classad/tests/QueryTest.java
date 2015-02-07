// $Header: /p/condor/repository/CONDOR_SRC/src_java/condor/classad/tests/QueryTest.java,v 1.13 2005/05/06 20:53:31 solomon Exp $

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
import java.util.*;
import java.net.*;

/** Main program to exercise the matching feature of classads.
 * It reads ads from a file in the same format as the output of Fetch, runs a
 * query in an SQL-like langauge on them, and prints the results.  The query
 * comes either from the command line or from standard input.  It looks like
 * this:
 * <pre>
 *    select Name, Arch, Memory, Disk, Idle = timeInterval(ConsoleIdle)
 *    from ads_bin
 *    where ConsoleIdle &gt; 24*60*60
 * </pre>
 * This class is meant to be an example of programming with classads, to be
 * studied and adapted.  It should not be used as a black box.
 * @author <a href="mailto:solomon@cs.wisc.edu">Marvin Solomon</a>
 */
public class QueryTest {

    /** Collector port. */
    public static final int COLLECTOR_COMM_PORT = 9618;

    /** Output stream for printing results. */
    private ClassAdWriter out = new ClassAdWriter(System.out, true);

    /** Main program.
     * Command-line options are
     * <dl>
     * <dt>-v<dd>Request more verbose debugging output (may be repeated).
     * <dt>-v<em>nn</em><dd>Set the debugging level to <em>nn</em>.
     * <dt>-m<em>nn</em><dd>Limit the number of items printed by "select *"
     *           to <em>nn</em>.
     * <dt>-c<dd>Get the ads directly from the Condor collector rather than
     *           readting them from a file (and ignore the "from" clause of the
     *           query).
     * <dt>-s<dd>Let the server do the filtering:  Send the "where" clause as
     *           part of the query to the collector, so that only matching ads
     *           are returned.  (Implies -c).
     * </dl>
     * Any remaining words on the command line are assumed to be query.
     * If there are no non-flag arguments, the query is read from stdin.
     * @param args the command-line arguments.
     */
    public static void main(String[] args) {
        // ClassAdParser.disable_tracing();
        new QueryTest().run(args);
    } // main(String[])

    /** Print a usage message and exit. */
    private void usage() {
        System.err.println("usage: java "+getClass().getName()
            +" [-v [debug_level]] [-m count] [-c] [-s] [query]");
        System.exit(1);
    } // usage()

    /** The main body of the program.
     * @param args the command-line arguments.
     */
    private void run(String[] args) {
        // Get command-line arguments
        int maxads = -1; // number of ads to print with select * (-1 = unlimted)
        int verbosity = 0; // amount of debugging output
        boolean cflag = false; // get ads from collector rather than file
        boolean sflag = false; // do filtering at server (the collector)
        String server = "condor.cs.wisc.edu";

        out.setFormatFlags(ClassAdWriter.READABLE);

        GetOpt opts = new GetOpt(getClass().getName(), args, "v::m:cs");
    opt_loop:
        for (;;) {
            switch(opts.nextOpt()) {
            case 'v': // More verbose
                if (opts.optarg == null) {
                    verbosity++;
                } else {
                    verbosity = Integer.parseInt(opts.optarg);
                }
                break;
            case 'm': // max ads
                maxads = Integer.parseInt(opts.optarg);
                break;
            case 'c': // use the collector
                cflag = true;
                break;
            case 's': // do the "where" filtering on the server
                sflag = true;
                cflag = true;
                break;
            case -1:
                break opt_loop;
            default: usage();
            }
        }

        // Parse the query
        Query q = null;
        if (opts.optind >= args.length) {
            q = Query.parse(System.in);
        } else {
            String s = args[opts.optind];
            for (opts.optind++;opts.optind<args.length;opts.optind++) {
                s += " " + args[opts.optind];
            }
            q = Query.parse(s);
        }
        if (q == null) {
            out.println("Cannot parse query");
            return;
        }

        // "Run" the query:  If the query is
        //    select .... from fname where expr
        // the ads are read from file "fname" (or directly from the Condor
        // collector) and put into a Vector if they satisfy the given expr.
        // All ads are kept if "where expr" is omitted.
        Vector results = new Vector();
        String adsFile = q.fromClause.toString();
        Reader adsReader;
        if (verbosity > 0 && q.whereClause != null) {
            out.print("Filter by ");
            out.println(q.whereClause);
        }
        int totalAds = 0;
        try {
            if (cflag) {
                // Get ads from the collector over the net
                if (verbosity > 0) {
                    out.println("Connecting to host '" + server
                    + "', port " + COLLECTOR_COMM_PORT);
                }
                Socket sock = new Socket(server, COLLECTOR_COMM_PORT);
                if (verbosity > 0) {
                    out.println("Connected: " + sock);
                }
                CedarOutputStream cmdStream
                    = new CedarOutputStream(sock.getOutputStream());

                // Construct a "filter" ad to send to the collector.
                // Currently, the collector demands an "old format" classad
                // Equivalent to
                //    [ Rank = 0;
                //      MyType = "Query";
                //      TargetType = "Machine";
                //    ]
                // If the -s flag was specified, we also ask the collector to
                // filter the ads according to the "where" clause of the query
                // by adding the attribute
                //      Requirements = WHERE_CLAUSE;
                RecordExpr ad = new RecordExpr();
                ad.insertAttribute("Rank", ClassAd.constant(0));
                ad.insertAttribute("MyType", ClassAd.constant("Query"));
                ad.insertAttribute("TargetType", ClassAd.constant("Machine"));
                if (sflag) {
                    ad.insertAttribute("Requirements", q.whereClause);
                }
                cmdStream.writeInt(5); // QUERY_STARTD_ADS
                ad.transmit(cmdStream);
                cmdStream.endOfMessage();
                adsReader
                    = new ClassAdReader(
                        new CedarInputStream(
                            new BufferedInputStream(
                                sock.getInputStream())));
            } else {
                // No -c flag:  Get the ads from the file specified in the
                // "from" clause of the query.
                adsReader
                    = new InputStreamReader(
                        new BufferedInputStream(
                            new FileInputStream(
                                adsFile)));
            }
            ClassAdParser parser = new ClassAdParser(adsReader);

            // Parse the returned ads and build a list
            // Ads are expected to be separated by semicolons in the input
            // stream.
            if (verbosity > 0) {
                out.println("Parsing ads");
            }
            long start = System.currentTimeMillis();
            for (;;) { // for each ad
                RecordExpr ad = (RecordExpr) parser.parse();
                if (ad == null) {
                    break;
                }
                totalAds++;
                int lookahead = parser.getNextToken();
                if (lookahead != ';') {
                    out.println("Expecting semicolon, found " + lookahead);
                    break;
                }
                if (q.whereClause != null) {
                    // Check whether this ad satisfies the "where" clause.
                    // Note that this test is a redundant verification of the
                    // collector's filtering if -s was specified.
                    if (verbosity == 1) {
                        out.print(".");
                    }
                    if (ClassAd.eval("Requirements", q.whereClause, ad)
                            .isTrue())
                    {
                        if (verbosity > 1) {
                            Expr.db("select " + ad.toString());
                        }
                        results.addElement(ad);
                    } else {
                        if (verbosity > 1) {
                            Expr.db("reject " + ad.toString());
                        }
                    }
                } else {
                    if (verbosity > 0) {
                        out.print("*");
                    }
                    results.addElement(ad);
                }
            } // for each ad
            long elapsed = System.currentTimeMillis() - start;
            int bytesRead;
            if (cflag) {
                bytesRead = ((ClassAdReader) adsReader).getByteCount();
            } else {
                bytesRead = (int) (new File(adsFile)).length();
            }
            float rate = (float) ((double) (1000*bytesRead)/elapsed);
            if (verbosity > 0) {
                out.println("");
                out.println(bytesRead + " bytes read in "
                            + elapsed + " ms ("
                            + rate + " bps)");
            }
        } catch (FileNotFoundException e) {
            out.println(e.getMessage() + ": no such file");
            return;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return;
        }
        if (verbosity > 0) {
            out.println("Total ads received = " + totalAds);
        }

        // Now pretty-print the result.  If the select clause is "select *",
        // then simply dump the resulting ads.  Otherwise, arrange them in
        // nice rows and columns.

        if (q.selectClause == null) {
            // 'select * from ...'
            // Just dump all the results
            int count = 0;
            for (Iterator res = results.iterator(); res.hasNext();) {
                Expr ad = (RecordExpr)res.next();
                out.println(ad);
                count++;
                if (maxads >= 0 && count >= maxads) {
                    break;
                }
            }
            return;
        }

        // We make two passes over the result tuples.
        // On the first pass, we determine the type of each selected field
        // (for now, fields are either integer or "other"),
        // find the max length of each field, and accumulate totals.
        // On the second pass, we print the results.

        if (verbosity > 0) {
            out.println("Analyzing " + results.size() + " rows...");
        }

        // Initialize tables with stuff that is the same for all tuples in the
        // result.
        Expr     fieldExpr[] = q.selectClause;
        String   fieldLabel[] = (String[]) q.selectNames.clone();
        int      fieldCount = fieldExpr.length;
        int      maxLen[] = new int[fieldCount];
        int      type[] = new int[fieldCount];
                        // -1 means undefined
                        //  0 means type not known yet
                        //  1 means int
                        //  2 means other
        int      sum[] = new int[fieldCount];
        int      lineLen = 0;

        // Initialize column info
        for (int i = 0; i < fieldCount; i++) {
            if (fieldLabel[i] == null) {
                fieldLabel[i] = fieldExpr[i].toString();
            }
            maxLen[i] = fieldLabel[i].length();
            type[i] = 0;
            sum[i] = 0;
        }

        // Pass 1: check types and lengths and accumulate totals
        for (Iterator res = results.iterator(); res.hasNext();) {
            RecordExpr ad = (RecordExpr) res.next();
            for (int i = 0; i < fieldCount; i++) {
                Expr value = ClassAd.eval("$value$", fieldExpr[i], ad);
                int ty = 2;
                switch (value.type) {
                case Expr.INTEGER:
                    sum[i] += ((Constant)value).intValue();
                    ty = 1;
                    break;
                case Expr.UNDEFINED:
                    ty = -1;
                    break;
                }
                    
                if (ty >= 0) {
                    if (type[i] == 0) {
                        type[i] = ty;
                    } else if (type[i] != ty) {
                        out.println("Error: mixed types for attribute "
                                    + fieldLabel[i]);
                        return;
                    }
                }
                int len = value.toString().length();
                if (len > maxLen[i]) {
                    maxLen[i] = len;
                }
            }
        } // End of Pass 1

        // Calculate line length
        for (int i = 0; i < fieldCount; i++) {
            if (type[i] == 1) {
                int len = (""+sum[i]).length();
                if (len > maxLen[i]) {
                    maxLen[i] = len;
                }
            }
            lineLen += maxLen[i];
        }
        lineLen += 3 * (fieldCount - 1); // for field separators

        // Print headers
        out.setFormatFlags(ClassAdWriter.COMPACT);
        for (int i = 0; i < fieldCount; i++) {
            if (i > 0) {
                out.print(" | ");
            }
            field(fieldLabel[i], maxLen[i], type[i]==1);
        }
        out.println("");
        for (int i = 0; i < lineLen; i++) {
            out.print("-");
        }
        out.println("");

        // Pass 2: print results
        out.setFormatFlags(ClassAdWriter.BRIEF);
        for (Iterator res = results.iterator(); res.hasNext();) {
            RecordExpr ad = (RecordExpr) res.next();
            for (int i = 0; i < fieldCount; i++) {
                if (i > 0) {
                    out.print(" | ");
                }
                Expr value = ClassAd.eval("$value$", fieldExpr[i], ad);
                String s = value.toString();
                field(s, maxLen[i], type[i]==1);
            }
            out.println("");
        }
        for (int i = 0; i < lineLen; i++) {
            out.print("=");
        }
        out.println("");

        // Print totals
        for (int i = 0; i < fieldCount; i++) {
            if (i>0) {
                out.print(" | ");
            }
            int len = maxLen[i];
            if (type[i] == 1) {
                field(""+sum[i], maxLen[i], true);
            } else {
                field("", maxLen[i], false);
            }
        }
        out.println("");
        int size = results.size();
        out.println(size + " row" + (size==1 ? "" : "s") + " found");
    } // run

    /** Pad str to field of length n (if n &gt; str.length()).
     ** Padding goes before if isInt, otherwise after.
     **/
    private void field(String str, int n, boolean isInt) {
        n -= str.length();
        if (n <= 0) {
            n = 0;
        }
        if (isInt) {
            while (n-- > 0) {
                out.print(" ");
            }
        }
        out.print(str);
        if (!isInt) {
            while (n-- > 0) {
                out.print(" ");
            }
        }
    }
} // QueryTest
