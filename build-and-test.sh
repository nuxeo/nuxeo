#!/bin/bash -x

# Build Nuxeo DAM
ant -f ft-build.xml make-distribution -Dmvn.profiles=$MAVEN_PROFILES || exit 1

# Start JBoss
chmod +x nuxeo-dam-distribution/nuxeo-dam-distribution-jboss/target/nuxeo-dam-jboss/bin/jbossctl || exit 1
nuxeo-dam-distribution/nuxeo-dam-distribution-jboss/target/nuxeo-dam-jboss/bin/jbossctl start || exit 1

# Run selenium tests
HIDE_FF=true ./nuxeo-dam-distribution/nuxeo-dam-ear/ftest/selenium/run.sh
ret1=$?

# Stop JBoss
nuxeo-dam-distribution/nuxeo-dam-distribution-jboss/target/nuxeo-dam-jboss/bin/jbossctl stop || exit 1

# Exit if some tests failed
[ $ret1 -eq 0 ] || exit 9
