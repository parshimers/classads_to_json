# Makefile for the package condor.classad

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

# $Header: /p/condor/repository/CONDOR_SRC/src_java/condor/classad/Makefile,v 1.21 2005/05/06 20:54:07 solomon Exp $

############################

# Standard header.  See $(TOP)/config.make for more information.

PACKAGE = condor.classad
EXTRA_DOC = condor.cedar
TOP = ../..

include $(TOP)/config.make

############################

all:: jar

TOOLDIR = $(TOP)/byacc
BYACC = $(TOOLDIR)/yacc.exe
GENJAVA = Parser.java

classes: $(GENJAVA)

# See also target refman: below.

############################# Classes

Parser.java: classad.y
	$(MAKE) -C $(TOOLDIR)
	$(RM) Parser.java
	$(BYACC) -v -j classad.y
	chmod -w Parser.java

$(CLASS_FILES): $(GENJAVA)

clean::
	$(RM) Parser.output
	$(RM) y.output

distclean::
	$(RM) Parser.java

############################# Reference manual

.PHONY: refman html
.PRECIOUS: refman.bbl

# Various ways of looking at the reference manual.  Choose the one that
# matches a previewer you have installed.

# Postscript previewers (see http://www.cs.wisc.edu/~ghost)
# Raw ghostscript previewer
PS_PREVIEWER = ghostscript
# Gui front-end to ghostscript
#PS_PREVIEWER = gsview

# Dvi ("raw" TeX output) previewers
# Yap previewer, part of MiKTeX (see http://www.miktex.org/)
#DVI_PREVIEWER = yap
# xdvi or xdvik for X windows (see http://xdvi.sourceforge.net/)
DVI_PREVIEWER = xdvi
PDF_PREVIEWER = xpdf

#PROOF = ps_proof
#PROOF = dvi_proof
PROOF = pdf_proof

L2HFLAGS = -math -no_footnode -local_icons -split +1

refman: refman.ps
proof: $(PROOF)
html: refman.dvi
	latex2html $(L2HFLAGS) refman.tex

ps_proof: refman.ps
	$(PS_PREVIEWER) refman.ps

dvi_proof: refman.dvi
	$(DVI_PREVIEWER) refman

pdf_proof: refman.pdf
	$(PDF_PREVIEWER) refman.pdf

grammar.tex: classad.y y2tex.pl
	$(PERL) y2tex.pl classad.y >$@

dtd.tex: classads.dtd
	(echo '\begin{verbatim}'; cat $< ; echo '\end{verbatim}') > $@

schema.tex: classads.xsd
	(echo '\begin{verbatim}'; cat $< ; echo '\end{verbatim}') > $@

refman.ps: refman.dvi
	dvips -o $@ $<

refman.pdf: refman.dvi
	dvipdf $< $@

refman.dvi: refman.tex grammar.tex dtd.tex schema.tex example.eps refman.bbl
	latex refman
	latex refman

refman.bbl: refman.bib refman.tex dtd.tex schema.tex grammar.tex
	latex refman
	bibtex refman

example.eps: example.fig
	fig2dev -Leps $< $@

clean::
	$(RM) refman.log refman.aux refman.dvi refman.bbl refman.blg
	$(RM) dtd.tex schema.tex grammar.tex
	$(RM) example.eps
	$(RM) -r refman

distclean::
	$(RM) refman.ps refman.pdf
