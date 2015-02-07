%{
import java.lang.Math;
import java.io.*;
import java.util.StringTokenizer;
public class Parser {
%}
     
/* YACC Declarations */
%token NUM
%left '-' '+'
%left '*' '/'
%left NEG     /* negation--unary minus */
%right '^'    /* exponentiation        */
     
/* Grammar follows */
%%
input:    /* empty string */
    | input line
    ;
     
line:
    '\n'
    | exp '\n'  { System.out.println(" " + $1 + " "); }
    ;
     
exp:
    NUM                  { $$ = $1;         }
    | exp '+' exp        { $$ = $1.doubleValue() + $3.doubleValue();    }
    | exp '-' exp        { $$ = $1.doubleValue() - $3.doubleValue();    }
    | exp '*' exp        { $$ = $1.doubleValue() * $3.doubleValue();    }
    | exp '/' exp        { $$ = $1.doubleValue() / $3.doubleValue();    }
    | '-' exp  %prec NEG { $$ = -$2.doubleValue();        }
    | exp '^' exp        { $$ = Math.pow($1.doubleValue(), $3.doubleValue()); }
    | '(' exp ')'        { $$ = $2.doubleValue();         }
    ;
%%

String ins;
StringTokenizer st;

void error(String s)
{
    System.out.println("par:"+s);
}

boolean newline;
int lex()
{
    //System.out.print("lex ");
    if (!st.hasMoreTokens()) {
        if (newline)
            return 0;
        else {
            newline=true;
            return '\n';  //So we look like classic YACC example
        }
        else
    }
    String s = st.nextToken();
    //System.out.println("tok:"+s);
    try {
        lval = Double.valueOf(s);
        return NUM;
    }
    catch (NumberFormatException e) {
        return s.charAt(0);
    }
}

void dotest()
{
BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
  System.out.println("BYACC/Java Calculator Demo");
  System.out.println("Note: Since this example uses the StringTokenizer");
  System.out.println("for simplicity, you will need to separate the items");
  System.out.println("with spaces, i.e.:  '( 3 + 5 ) * 2'");
  while (true)
    {
    System.out.print("expression:");
    try
      {
      ins = in.readLine();
      if (ins == null)
        {
        System.out.println();
        return;
        }
      }
    catch (Exception e)
      {
        e.printStackTrace();
      }
    st = new StringTokenizer(ins);
    newline=false;
    parse();
    }
}

public static void main(String args[])
{
  parser par = new parser(false);
  par.dotest();
}
