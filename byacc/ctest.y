%{
/* Trivial test grammar, C version */
#include <stdio.h>
#include <string.h>
#include <malloc.h>
#include <ctype.h>
%}

%union {
    char *s;
}

%left <s> '+' '-'
%left <s> '*' '/'

%token <s> ID

%type <s> Expr

%start Prog

%%
Prog
    : Expr { printf("result \"%s\"\n", $1); }
    ;
Expr
    : Expr '+' Expr { $$ = cat($1, $2, $3); }
    | Expr '-' Expr { $$ = cat($1, $2, $3); }
    | Expr '*' Expr { $$ = cat($1, $2, $3); }
    | Expr '/' Expr { $$ = cat($1, $2, $3); }
    | '(' Expr ')' { $$ = $2; }
    | ID
    ;
%%

int yyparse(void);

void yyerror(const char *msg) {
    fprintf(stderr,"Error on input '%s': %s\n", yylval.s, msg);
} /* yyerror */

int main() {
    return yyparse();
} /* main */

int yylex() {
    for (;;) {
        int c = getchar();
        char val[2];
        val[1] = 0;
        if (c <= 0) {
            return 0;
        }
        if (isspace(c)) {
            continue;
        }
        val[0] = c;
        yylval.s = strdup(val);
        return isalnum(c) ? ID : c;
    }
} /* lex */

char *cat(char *a, char *b, char *c) {
    int len = strlen(a) + strlen(b) + strlen(c) + 3;
    char *res = malloc(len);
    sprintf(res, "(%s%s%s)", a, b, c);
    free(a);
    free(b);
    free(c);
    return res;
} /* cat */
