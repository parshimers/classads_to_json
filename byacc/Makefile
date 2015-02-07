DEST = .
HDRS = defs.h
CFLAGS = -g -Wall
CC = gcc
JAVAC = jikes
JAVA = java
LDFLAGS =
LIBS =
LINKER = gcc
MAKEFILE = Makefile
OBJS = closure.o \
	error.o \
	lalr.o \
	lr0.o \
	main.o \
	mkpar.o \
	output.o \
	reader.o \
	skeleton.o \
	symtab.o \
	verbose.o \
	warshall.o

PRINT = pr -f -l88

# Windows requires that the name end in .exe but Unix doesn't care.
PROGRAM = yacc.exe

SRCS = closure.c \
	error.c \
	lalr.c \
	lr0.c \
	main.c \
	mkpar.c \
	output.c \
	reader.c \
	skeleton.c \
	symtab.c \
	verbose.c \
	warshall.c

ALL = 1.README.FIRST README Makefile \
	ACKNOWLEDGEMENTS NEW_FEATURES NOTES NO_WARRANTY \
	$(HDRS) $(SRCS) \
	ctest.y jtest.y tf.y yacc.1

all: $(PROGRAM)

$(PROGRAM): $(OBJS) $(LIBS)
	$(LINKER) $(LDFLAGS) -o $(PROGRAM) $(OBJS) $(LIBS)

clean::
	rm -f $(OBJS)

clobber: clean
	rm -f $(PROGRAM) byacc.tar.gz

depend:
	mkmf -f $(MAKEFILE) PROGRAM=$(PROGRAM) DEST=$(DEST)

index:
	ctags -wx $(HDRS) $(SRCS)

install: $(PROGRAM)
	install -s $(PROGRAM) $(DEST)

listing:
	$(PRINT) Makefile $(HDRS) $(SRCS) | lpr

program: $(PROGRAM)

tags: $(HDRS) $(SRCS)
	ctags $(HDRS) $(SRCS)

export: byacc.tar.gz
byacc.tar.gz: $(ALL)
	tar cvzf byacc.tar.gz $(ALL)

# Java parser Test

INPUT='a+b c*d'

java_test: Parser.class
	@echo ======================== prefix allowed
	echo $(INPUT) | java Parser -p
	@echo ======================== only parse whole input
	echo $(INPUT) | java Parser 

%.class: %.java
	$(JAVAC) $<

Parser.java: jtest.y $(PROGRAM)
	./$(PROGRAM) -v -j jtest.y

clean::
	rm -f *.class Parser.java Parser.output

# C parser test

c_test: parser
	echo 'a+b+c' | parser

parser: y.tab.o
	$(CC) -o $@ $(CFLAGS) y.tab.o

y.tab.c: ctest.y $(PROGRAM)
	./$(PROGRAM) -v ctest.y

clean::
	rm -f *.o parser y.tab.c y.output

###
closure.o: defs.h
error.o: defs.h
lalr.o: defs.h
lr0.o: defs.h
main.o: defs.h
mkpar.o: defs.h
output.o: defs.h
reader.o: defs.h
skeleton.o: defs.h
symtab.o: defs.h
verbose.o: defs.h
warshall.o: defs.h
