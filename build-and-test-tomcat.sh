#!/bin/bash -x

# Build Nuxeo DAM
ant -f ft-build.xml make-tomcat-distribution -Dmvn.profiles=$MAVEN_PROFILES

# Start Tomcat
ant -f ft-build.xml start-tomcat

# Run selenium tests
HIDE_FF=true ./nuxeo-dam-distribution/nuxeo-dam-ear/ftest/selenium/run.sh
ret1=$?

# Stop Tomcat
ant -f ft-build.xml stop-tomcat

# Exit if some tests failed
[ $ret1 -eq 0 ] || exit 9