/** $Header: /p/condor/repository/CONDOR_SRC/src_java/condor/classad/tests/ExprTest.java,v 1.8 2005/05/06 20:53:31 solomon Exp $ */

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
import java.io.*;
import java.util.*;
import org.json.*;
import java.sql.Timestamp;

/** Main program to test evaluation of expressions.
 * Input consists of a sequence of expressions, each followed by a semicolon.
 * Each expression is one of the following:
 * <ul>
 * <li>A list of two classads (RECORD expressions); in this case ad1.value is
 * evaluated in the same environment used for matching classads.
 * <li>A single classad (RECORD) expression; in this case, ad.value is
 * evaluated.
 * <li>Anything else; the expression is simply evaluated.
 * </ul>
 * If the -n command-line option is specified, the expression is imply printed.
 */
public class ExprTest {
    /** Place to display output. */
    ClassAdWriter out;

    /** Abbreviation for system.out.println.
     * @param s something to be printed (generally a String)
     */
    private static void pl(Object s) {
        System.out.println(s);
    }

    /** Flags for controling printing */
    private int printFlags = ClassAdWriter.MULTI_LINE_ADS
                                  | ClassAdWriter.MULTI_LINE_LISTS
                                  | ClassAdWriter.MINIMAL_PARENTHESES
                                  | ClassAdWriter.SHOW_ERROR_DETAIL
                                  | ClassAdWriter.JAVA_REALS;

    /** Main program.
     * Command-line options are
     * <dl>
     * <dt>-v<dd>Request more verbose debugging output (may be repeated).
     * <dt>-v<em>nn</em><dd>Set the debugging level to <em>nn</em>.
     * <dt>-t<dd>Trace the actions of the parser.
     * <dt>-x<dd>Parse XML input.
     * <dt>-p flags<dd>Set print flags (try -px for a list).
     * </dl>
     * An optional file name may be supplied (default is stdin).
     * @param args the command-line arguments.
     * @throws Exception if something goes wrong.
     */
    public static void main(String[] args) throws Exception {
        new ExprTest().run(args);
    }

    /** Print a usage message and exit. */
    private void usage() {
        System.err.println(
            "usage: java "
            + getClass().getName()
            + " [-v[verbosity]] [-t] [-n] [-x] [-p print_options ...]\n"
            + "     [-l library_ class] [input_file]");
        System.exit(1);
    }

    /** The "main program".
     * @param args the command-line arguments.
     * @throws Exception if something goes wrong.
     */
    public void run(String[] args) throws Exception {
        // Get command-line arguments
        int verbosity = 0;              // amount of debugging output
        boolean tracing = false;        // -t flag:  tell the parser to trace
        boolean eval = true;            // -n flag: inhibit evaluation
        int rep = ClassAdWriter.NATIVE; // representation
        int format = ClassAdParser.TEXT; // input format
        GetOpt opts = new GetOpt(getClass().getName(), args, "v::tnxp:l:");
    opt_loop:
        for (;;) switch(opts.nextOpt()) {
        case 'v': // More verbose
            if (opts.optarg == null) {
                verbosity++;
            } else {
                verbosity = Integer.parseInt(opts.optarg);
            }
            break;
        case 't': // parser trace
            tracing = true;
            break;
        case 'n': // inhibit evaluation
            eval = false;
            break;
        case 'x': // XML input
            format = ClassAdParser.XML;
            break;
        case 'p': // print options
            for (int i = 0; i < opts.optarg.length(); i++) {
                switch (opts.optarg.charAt(i)) {
                default:
                    pl("print flags are:");
                    pl("  -   clear all flags");
                    pl("  s   NO_ESCAPE_STRINGS");
                    pl("  e   SHOW_ERROR_DETAIL");
                    pl("  p   MINIMAL_PARENTHESES");
                    pl("  a   MULTI_LINE_ADS");
                    pl("  l   MULTI_LINE_LISTS");
                    pl("  b   BRIEF");
                    pl("  c   COMPACT");
                    pl("  r   READABLE");
                    pl("  x   XML");
                    pl("default is alp");
                    return;
                case '-': printFlags = 0; break;
                case 's': printFlags |= ClassAdWriter.NO_ESCAPE_STRINGS; break;
                case 'e': printFlags |= ClassAdWriter.SHOW_ERROR_DETAIL; break;
                case 'p': printFlags |=
                                    ClassAdWriter.MINIMAL_PARENTHESES; break;
                case 'a': printFlags |= ClassAdWriter.MULTI_LINE_ADS; break;
                case 'l': printFlags |= ClassAdWriter.MULTI_LINE_LISTS; break;
                case 'b': printFlags = ClassAdWriter.BRIEF; break;
                case 'c': printFlags = ClassAdWriter.COMPACT; break;
                case 'r': printFlags = ClassAdWriter.READABLE; break;
                case 'x': rep = ClassAdWriter.XML; break;
                }
            }
            break;
        case 'l':
            if (!ClassAd.loadJavaLibrary(opts.optarg)) {
                pl("Library " + opts.optarg + " cannot be loaded");
            }
            break;
        case -1:
            break opt_loop;
        default: usage();
        }

        Reader input = null;
        if (opts.optind < args.length) {
            try {
                input = new FileReader(args[opts.optind]);
            } catch (Exception e) {
                System.err.println(args[opts.optind] + ": " + e);
                usage();
            }
        } else {
            input = new InputStreamReader(System.in);
        }

        ClassAdParser parser = new ClassAdParser(input, format);
        parser.enableTracing(tracing);
        out = new ClassAdWriter(System.out, rep, true);
        out.setFormatFlags(printFlags);

        // main loop
        if (format == ClassAdParser.XML) {
            ListExpr list = (ListExpr) parser.parse();
            for (Iterator i = list.iterator(); i.hasNext(); ) {
                eval((Expr) i.next());
            }
        } else {
            for(;;) {
                Expr expr = parser.parse();
                if (expr==null) {
                    break;
                }
                if (eval) {
                    eval(expr);
                } else {
                    out.println(expr);
                }
                int lookahead = parser.getNextToken();
                if (lookahead != ';') {
                    out.println("Expecting semicolon, found " + lookahead);
                    return;
                }
            }
        }
    } // run(String[]);

    private void eval(Expr expr) {
        RecordExpr rexpr = (RecordExpr) expr;
        JSONObject o = new JSONObject();

        for(Iterator<AttrName> i = rexpr.attributes(); i.hasNext();){
            AttrName attr = i.next();
            //out.println(attr.toString() + rexpr.lookup(attr).toString());
            Expr val = rexpr.lookup(attr);
            if(!val.isConstant()){
            //cast non-constant expressions to strings. dont worry about nested records.
                o.put(attr.toString(), val.toString().replace("\"",""));
            }
            //assume lists are only of simple types
            else if (val instanceof ListExpr){
                ListExpr lsVal = (ListExpr) val;
                for(Iterator<Constant> j = lsVal.iterator(); j.hasNext();){
                    accumulatetIntoJSON(o,attr,j.next());
                }
            }
            else{
                insertIntoJSON(o,attr,((Constant) val));
            }

        }
        out.print(o);
        out.println();
    } // eval(Expr)
    private void insertIntoJSON(JSONObject o, AttrName attr, Constant c){
        //i really don't like this...
        if(c.value instanceof Integer){
            o.put(attr.toString(),(Integer)c.value);
        }
        else if(c.value instanceof Double){
            o.put(attr.toString(), (Double)c.value);
        }
        else if(c.value instanceof String){
            o.put(attr.toString(), (String)c.value);
        }
        else if(c.value instanceof Long){
            //time
            o.put(attr.toString(), (Long)c.value);
        }
        else if(c.value instanceof Timestamp){
        //timestamp
            o.put(attr.toString(), ((Timestamp)c.value).getTime());
        }
        else {
            //boolean
            o.put(attr.toString(), c.isTrue());
        }
    }
    private void accumulatetIntoJSON(JSONObject o, AttrName attr, Constant c){
        //i really don't like this...
        if(c.value instanceof Integer){
            o.accumulate(attr.toString(),(Integer)c.value);
        }
        else if(c.value instanceof Double){
            o.accumulate(attr.toString(), (Double)c.value);
        }
        else if(c.value instanceof String){
            o.accumulate(attr.toString(), (String)c.value);
        }
        else if(c.value instanceof Long){
            //time
            o.accumulate(attr.toString(), (Long)c.value);
        }
        else if(c.value instanceof Timestamp){
        //timestamp
            o.accumulate(attr.toString(), ((Timestamp)c.value).getTime());
        }
        else {
            //boolean
            o.accumulate(attr.toString(), c.isTrue());
        }
    }
} // class ExprTest
