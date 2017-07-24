#!/usr/bin/make

# @(#) $Id: makefile 470 2010-06-08 19:26:56Z gfis $
# 2017-07-22: Georg Fischer: copied from xtrans

DIFF=diff -y --suppress-common-lines --width=160
DIFF=diff -w -rs -C0
SRC=src/main/java/org/teherba/putrans
TOMC=/var/lib/tomcat/webapps/putrans
TOMC=c:/var/lib/tomcat/webapps/putrans
TESTDIR=test
# the following can be overriden outside for single or subset tests,
# for example make regression TEST=U%
TEST="%"
# for Windows, SUDO should be empty
SUDO=
JAVA=java -cp dist/putrans.jar
JAR=java -jar dist/putrans.jar

all: regression
#-------------------------------------------------------------------
# Perform a regression test
regression: 
	java -classpath "dist/putrans.jar" \
			org.teherba.common.RegressionTester $(TESTDIR)/all.tests $(TEST) 2>&1 \
	| tee $(TESTDIR)/regression.log
	grep FAILED $(TESTDIR)/regression.log

#	java -Dlog4j.debug 
#----
# Recreate all testcases which failed (i.e. remove xxx.prev.tst)
# Handle with care!
# Failing testcases are turned into "passed" and are manifested by this target!
recreate: recr1 regr2
recr0:
	grep -E '> FAILED' $(TESTDIR)/regression*.log | cut -f 3 -d ' ' | xargs -l -iﬂﬂ echo rm -v test/ﬂﬂ.prev.tst
recr1:
	grep -E '> FAILED' $(TESTDIR)/regression*.log | cut -f 3 -d ' ' | xargs -l -iﬂﬂ rm -v test/ﬂﬂ.prev.tst
regr2:
	make regression TEST=$(TEST) > x.tmp
#---------------------------------------------------
# test whether all defined tests in all.tests have *.prev.tst results and vice versa
check_tests:
	grep -E "^TEST" $(TESTDIR)/all.tests | cut -b 6-8 | sort | uniq -c > $(TESTDIR)/tests_formal.tmp
	ls -1 $(TESTDIR)/*.prev.tst          | cut -b 6-8 | sort | uniq -c > $(TESTDIR)/tests_actual.tmp
	diff -y --suppress-common-lines --width=32 $(TESTDIR)/tests_formal.tmp $(TESTDIR)/tests_actual.tmp
#---------------------------------------------------
jfind:
	find src -iname "*.java" | xargs -l grep -H $(JF)
rmbak:
	find src -iname "*.bak"  | xargs -l rm -v
#---------------------------------------------------
test1:
	$(JAR) -ibm6788 ../../pt/disk/test/6788.1/DOCUM002.TXT | tee x.tmp
traub1:
	$(JAR) -ibm6788 test/traub1/DOCUM001.TXT               | tee x.tmp
batch: b1 b2 b3 b4
b1:
	find test -iname "*.xml"         | xargs -l rm -v
b2:
	find test -type f -iname "*.txt" | xargs -t -l -iﬂﬂ java -jar dist/putrans.jar -ibm6788 ﬂﬂ ﬂﬂ.xml
b3:
	find test -iname "*.xml"         | xargs -l grep -aiH " head.6788"
b4:
	find test -iname "*.xml"         | xargs -l grep -aiH " title.6788"
