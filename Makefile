# Makefile to assemble .jar files for for various condor.* packages

# WARNING:  This Makefile requires GNU make

# $Header: /p/condor/repository/CONDOR_SRC/src_java/Makefile,v 1.28 2003/06/09 13:35:43 solomon Exp $

# Note, the ordering of the following list is imporatant because it indicates
# the order in which the packages are compiled.

PACKAGES = \
	condor.cedar \
	condor.classad \
	condor.classad.tests \

SUBDIRS = $(subst .,/,$(PACKAGES))

# top-level Makefile version
.PHONY: default $(SUBDIRS)

default: $(SUBDIRS)

TOP = .
include config.make

$(SUBDIRS):
	$(MAKE) -C $@

condor/classad: condor/cedar
condor/classad/tests: condor/classad

all clean distclean jar test::
	for x in $(SUBDIRS); do \
		$(MAKE) -C $$x $@; \
		done

clean distclean::
	$(MAKE) -C byacc clobber

JAVADOC_FLAGS = \
	-d doc \
	-doctitle 'Condor Java Library' \
	-sourcepath $(TOP) \
	-package \
	-use \
	-version \
	-author \
	-splitindex \
	-link http://java.sun.com/j2se/1.4/docs/api \
	-breakiterator \
	-noqualifier 'java.*:javax.*'

javadoc:: doc jar
	$(JAVADOC) \
		$(JAVADOC_FLAGS) \
		$(PACKAGES)

doc:
	mkdir -p $@

clean::
	$(RM) -r doc
	$(RM) classad_java_src.tgz
