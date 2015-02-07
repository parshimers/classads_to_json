# Makefile for the package condor.classad.tests

# WARNING:  This Makefile requires GNU make

# $Header: /p/condor/repository/CONDOR_SRC/src_java/condor/classad/tests/Makefile,v 1.28 2005/05/06 21:09:45 solomon Exp $

# **************************Copyright-DO-NOT-REMOVE-THIS-LINE**
# Condor Copyright Notice
#
# See LICENSE.TXT for additional notices and disclaimers.
#
# Copyright (c)1990-2005 Condor Team, Computer Sciences Department,
# University of Wisconsin-Madison, Madison, WI.  All Rights Reserved.
# Use of the CONDOR Software Program Source Code is authorized
# solely under the terms of the Condor Public License (see LICENSE.TXT).
# For more information contact:
# CONDOR Team, Attention: Professor Miron Livny,
# 7367 Computer Sciences, 1210 W. Dayton St., Madison, WI 53706-1685,
# (608) 262-0856 or miron@cs.wisc.edu.
# ***************************Copyright-DO-NOT-REMOVE-THIS-LINE**/

############################

# Standard header.  See $(TOP)/config.make for more information.

PACKAGE = condor.classad.tests
TOP = ../../..
JLIBS = ../classad.jar:../../cedar/cedar.jar
CONDOR_HOST = condor.cs.wisc.edu
MAIN_CLASS = CATest

include $(TOP)/config.make

############################

test:: expr_test query_test expr_test_xml print_test old_test

demo.jar: Makefile
	$(RM) demo.jar
	$(RM) -r demo.tmp
	echo "Main-Class: $(PACKAGE_DIR)/$(MAIN_CLASS)" > manifest
	mkdir demo.tmp
	cd demo.tmp ; jar xf ../tests.jar ; jar xf ../../classad.jar
	$(RM) -r demo.tmp/META-INF
	cd demo.tmp ; jar cmf ../manifest ../demo.jar *
	$(RM) manifest
	$(RM) -r demo.tmp

clean::
	$(RM) manifest
	$(RM) -r demo.tmp

interactive: CATest.class
	$(JAVA) $(JAVA_FLAGS) \
		$(PACKAGE).CATest -v

batch: CATest.class
	$(JAVA) $(JAVA_FLAGS) \
		$(PACKAGE).CATest -v -e batch_test

# Create the .jar files needed by these tests (should perhaps be a dependency
# on all other targets).
libs:
	$(MAKE) -C ../../cedar cedar.jar
	$(MAKE) -C .. classad.jar

# Fetch all "machine" ads from the Condor collector.
# Will only work if CONDOR_HOST is accessible from here.
ads_raw: Fetch.class
	$(JAVA) $(JAVA_FLAGS) $(PACKAGE).Fetch -h $(CONDOR_HOST) -v1 machine $@

expr_test: expr_test1 expr_test2 expr_test3 expr_test4 expr_test5
expr_test_xml: expr_test1_xml expr_test2_xml expr_test3_xml \
	expr_test4_xml expr_test5_xml

print_test: ExprTest.class
	echo ====== default > print.log
	$(JAVA) $(JAVA_FLAGS) \
		$(PACKAGE).ExprTest -n < scripts/print.test >> print.log
	echo ====== no flags >> print.log
	$(JAVA) $(JAVA_FLAGS) \
		$(PACKAGE).ExprTest -n -p- < scripts/print.test >> print.log
	echo ====== NO_ESCAPE_STRINGS >> print.log
	$(JAVA) $(JAVA_FLAGS) \
		$(PACKAGE).ExprTest -n -p-s < scripts/print.test >> print.log
	echo ====== BRIEF >> print.log
	$(JAVA) $(JAVA_FLAGS) \
		$(PACKAGE).ExprTest -n -pb < scripts/print.test >> print.log
	echo ====== COMPACT >> print.log
	$(JAVA) $(JAVA_FLAGS) \
		$(PACKAGE).ExprTest -n -pc < scripts/print.test >> print.log
	echo ====== READABLE >> print.log
	$(JAVA) $(JAVA_FLAGS) \
		$(PACKAGE).ExprTest -n -pr < scripts/print.test >> print.log
	diff scripts/print.data print.log

expr_test1: ExprTest.class
	$(JAVA) $(JAVA_FLAGS) \
		$(PACKAGE).ExprTest < scripts/expr.1.test >expr.1.log
	grep -av '^#' scripts/expr.1.data | diff -au - expr.1.log
expr_test2: ExprTest.class
	$(JAVA) $(JAVA_FLAGS) \
		$(PACKAGE).ExprTest < scripts/expr.2.test >expr.2.log
	grep -av '^#' scripts/expr.2.data | diff -au - expr.2.log
expr_test3: ExprTest.class
	$(JAVA) $(JAVA_FLAGS) \
		$(PACKAGE).ExprTest < scripts/expr.3.test >expr.3.log
	grep -av '^#' scripts/expr.3.data | diff -au - expr.3.log
expr_test4: ExprTest.class
	$(JAVA) $(JAVA_FLAGS) \
		$(PACKAGE).ExprTest < scripts/expr.4.test >expr.4.log
	grep -av '^#' scripts/expr.4.data \
		| diff -au -IVARIABLE - expr.4.log
expr_test5: ExprTest.class
	$(JAVA) $(JAVA_FLAGS) \
		$(PACKAGE).ExprTest < scripts/expr.5.test >expr.5.log
	grep -av '^#' scripts/expr.5.data | diff -au - expr.5.log

expr_test1_xml: ExprTest.class Convert.class
	$(JAVA) $(JAVA_FLAGS) $(PACKAGE).Convert -ox -l \
		scripts/expr.1.test > tmp.xml
	$(JAVA) $(JAVA_FLAGS) $(PACKAGE).ExprTest -x < tmp.xml >expr.1.log
	grep -av '^#' scripts/expr.1.data | diff -au - expr.1.log
expr_test2_xml: ExprTest.class Convert.class
	$(JAVA) $(JAVA_FLAGS) $(PACKAGE).Convert -ox -l \
		scripts/expr.2.test > tmp.xml
	$(JAVA) $(JAVA_FLAGS) $(PACKAGE).ExprTest -x < tmp.xml >expr.2.log
	grep -av '^#' scripts/expr.2.data | diff -au - expr.2.log
expr_test3_xml: ExprTest.class Convert.class
	$(JAVA) $(JAVA_FLAGS) $(PACKAGE).Convert -ox -l \
		scripts/expr.3.test > tmp.xml
	$(JAVA) $(JAVA_FLAGS) $(PACKAGE).ExprTest -x < tmp.xml >expr.3.log
	grep -av '^#' scripts/expr.3.data | diff -au - expr.3.log
expr_test4_xml: ExprTest.class Convert.class
	$(JAVA) $(JAVA_FLAGS) $(PACKAGE).Convert -ox -l \
		scripts/expr.4.test > tmp.xml
	$(JAVA) $(JAVA_FLAGS) $(PACKAGE).ExprTest -x < tmp.xml >expr.4.log
	grep -av '^#' scripts/expr.4.data \
		| diff -au -IVARIABLE - expr.4.log
expr_test5_xml: ExprTest.class Convert.class
	$(JAVA) $(JAVA_FLAGS) $(PACKAGE).Convert -ox -l \
		scripts/expr.5.test > tmp.xml
	$(JAVA) $(JAVA_FLAGS) $(PACKAGE).ExprTest -x < tmp.xml >expr.5.log
	grep -av '^#' scripts/expr.5.data | diff -au - expr.5.log

query_test: query_test0 query_test1 query_test2 query_test3

query_test0: QueryTest.class ads_raw
	echo 'select Name, Arch from ads_raw where name=="gjetost.cs.wisc.edu"' \
		| $(JAVA) $(JAVA_FLAGS) \
			$(PACKAGE).QueryTest -v1 -s
query_test1: QueryTest.class ads_raw
	cat scripts/query.1.test
	$(JAVA) $(JAVA_FLAGS) \
		$(PACKAGE).QueryTest -m2 <scripts/query.1.test
query_test2: QueryTest.class ads_raw
	cat scripts/query.2.test
	$(JAVA) $(JAVA_FLAGS) \
		$(PACKAGE).QueryTest <scripts/query.2.test
query_test3: QueryTest.class ads_raw
	cat scripts/query.3.test
	$(JAVA) $(JAVA_FLAGS) \
		$(PACKAGE).QueryTest -v1 <scripts/query.3.test
	
old_test: OldFormatTest.class testads
	$(JAVA) $(JAVA_FLAGS) $(PACKAGE).OldFormatTest

ads_raw.xml: ads_raw Convert.class
	$(JAVA) $(JAVA_FLAGS) $(PACKAGE).Convert -ox ads_raw >$@

clean::
	$(RM) ads_raw ads_raw.xml *.log
