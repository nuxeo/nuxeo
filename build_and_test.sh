#!/bin/bash -x

# Build Nuxeo DAM
ant -f ft-build.xml make-distribution -Dmvn.profiles=$MAVEN_PROFILES

# Start JBoss
chmod +x nuxeo-dam-distribution/nuxeo-dam-distribution-jboss/target/nuxeo-dam-jboss/bin/jbossctl
nuxeo-dam-distribution/nuxeo-dam-distribution-jboss/target/nuxeo-dam-jboss/bin/jbossctl start

# Run selenium tests
HIDE_FF=true ./nuxeo-dam-distribution/nuxeo-dam-ear/ftest/selenium/run.sh
ret1=$?

# Stop JBoss
nuxeo-dam-distribution/nuxeo-dam-distribution-jboss/target/nuxeo-dam-jboss/bin/jbossctl stop

# Exit if some tests failed
[ $ret1 -eq 0 ] || exit 9