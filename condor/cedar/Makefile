# Makefile for the package condor.cedar
# WARNING:  This Makefile requires GNU make

# $Header: /p/condor/repository/CONDOR_SRC/src_java/condor/cedar/Makefile,v 1.9 2003/01/27 22:13:36 solomon Exp $

############################

# Standard header.  See $(TOP)/config.make for more information.

PACKAGE = condor.cedar
TOP = ../..

include $(TOP)/config.make

############################

all:: jar

test1: cedar.jar Test1.class
	$(JAVA) -cp .:cedar.jar Test1

test2: cedar.jar Test2.class
	$(JAVA) -cp .:cedar.jar Test2

Test1.java: Test1.test
	cp Test1.test Test1.java; chmod -w Test1.java

Test2.java: Test2.test
	cp Test2.test Test2.java; chmod -w Test2.java

clean distclean::
	$(RM) Test1.java Test2.java data
