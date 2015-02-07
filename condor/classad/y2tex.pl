:
# The following nonsense is like using a first line of
#        #!/path/to/perl -w
# but doesn't require you to wire in the pathname of perl.  vim: syntax=perl
eval 'exec perl -wS $0 ${1+"$@"}'
if 0;

# Hack to extract the grammer from a YACC file, stripping out semantic
# routines, etc. and escaping things to serve as input to latex.
# This is not a general-purpose tool.  It's just barely good enough for
# classad.y, which is written in a stylized manner.  In particular, we assume 
# that a semantic routine either
#   1.  starts with ' { ' and ends with '}' at the end of the same line, or
#   2.  starts with '{' and the eof of a line and ends wiht '}' at the end
#       of a later line.
# Moreover, any other left braces that appear outsize a semantic routine, or
# any right braces anywhere will be followed by a non-space character.

use strict 'subs';

$extra_space = 0;

print "\\begin{verbatim}\n";
while (<>) {
    last if /^\%\%/;
    if (/^\%left/ && !$extra_space) {
        print "\n";
        $extra_space = 1;
    }
    print if /^\%(token|left)/;
}
print "\n%%\n\n\\end{verbatim}\n\\begin{verbatim}\n";
while (<>) {
    last if /^\%\%/;
    #print ">> $_";
    if (/ {($| )/) {
        #print "start\n";
        if (/}$/) {
            s/ { .*}$//;
            #print "one-liner: $_";
            print;
        } else {
            s/ {$//;
            #print "strip head: $_";
            print;
            while (<>) {
                #print "skip body: $_";
                last if /}$/;
            }
        }
    } else {
        #print "no semantic routine\n";
        print;
    }
}
print "\\end{verbatim}\n";
