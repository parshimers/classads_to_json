#!/bin/sh

# Quick hack to try to find JAVA_HOME (the root of your java installation).
# Assumes that "java" is in your path, and the executable is in
# $JAVA_HOME/bin/java.  In a valid installation, $JAVA_HOME/jre/lib/rt.jar
# should exist.

# On success, prints the name of the directory to stdout.
# On failure, prints a string starting with ERROR
# Intended for use in a GNU Makefile

JAVA=`which java 2>&1`
if [ $? -ne 0 ]
then
    echo ERROR: java not found in your path
    exit 1
fi
if [ -h $JAVA ]
then
    JAVA=`ls -l $JAVA | sed -e 's/.*-> *//'`
fi
JAVA_HOME=`dirname $JAVA`
JAVA_HOME=`dirname $JAVA_HOME`
if [ -r $JAVA_HOME/jre/lib/rt.jar ]
then
    echo $JAVA_HOME
else
    echo ERROR $JAVA_HOME/jre/lib/rt.jar : no such file
fi
