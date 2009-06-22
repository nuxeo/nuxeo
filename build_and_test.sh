#!/bin/bash -x

# Build Nuxeo DAM
ant -f ft-build.xml make-distribution

# Start JBoss
ant -f ft-build.xml start-jboss

# Run selenium tests
HIDE_FF=true ./nuxeo-dam-distribution/nuxeo-dam-ear/ftest/selenium/run.sh
ret1=$?

# Stop JBoss
ant -f ft-build.xml stop-jboss

# Exit if some tests failed
[ $ret1 -eq 0 ] || exit 9