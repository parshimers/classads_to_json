%{
// Trivial test grammar, Java version
import java.io.*;
public class Parser {
%}

%left <String> '+' '-'
%left <String> '*' '/'

%token <String> ID

%type <String> Expr

%start Expr

%%
Expr
    : Expr '+' Expr { $$ = "(" + $1 + $2 + $3 + ")"; }
    | Expr '-' Expr { $$ = "(" + $1 + $2 + $3 + ")"; }
    | Expr '*' Expr { $$ = "(" + $1 + $2 + $3 + ")"; }
    | Expr '/' Expr { $$ = "(" + $1 + $2 + $3 + ")"; }
    | '(' Expr ')' { $$ = $2; }
    | ID
    ;
%%

/** Print a usage message and exit. */
private static void usage() {
    System.err.println("usage: java Parse [-v][-p]");
    System.exit(1);
} // usage()

/** Main program.
 * @param args command-line options.
 */
public static void main(String[] args) {
    Parser p = new Parser();
    boolean acceptPrefix = false;
    for (int i = 0; i < args.length; i++) {
        if ("-p".equals(args[i])) {
            acceptPrefix = true;
        } else if ("-v".equals(args[i])) {
            p.enableTracing(true);
        } else {
            usage();
        }
    }
    Object result;
    int failures = 0;
    for (;;) {
        result = p.parse(acceptPrefix);
        System.out.println("*** result " + result);
        if (result == null) {
            if (p.lookahead == 0) {
                break;
            } else {
                System.out.println("skip lookahead " + p.lval);
                p.lookahead = p.lex();
            }
        }
    }
} // main(String[])

/** Trivial lexical analyzer.
 * Whitespace is skipped, a single letter or digit is classified as an ID,
 * and anything else is returned return as is.  In all cases lval is the
 * character found, as a String.
 * @return the token code for the next token.
 */
int lex() {
    try {
        for (;;) {
            int n = System.in.read();
            if (n <= 0) {
                return 0;
            }
            char c = (char) n;
            if (Character.isWhitespace(c)) {
                continue;
            }
            lval = Character.toString(c);
            return Character.isLetterOrDigit(c) ? ID : c;
        }
    } catch (IOException e) {
        e.printStackTrace();
        System.exit(1);
        return 0;
    }
} // lex()

/** Display an error message.
 * @param msg the message to display.
 */
void error(String msg) {
    System.err.println("ERROR on token " + lval + ": " + msg);
} // error(String)

} // Parser
