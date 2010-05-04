#!/bin/bash -x

# Build Nuxeo DAM
ant -f ft-build.xml make-distribution -Dmvn.profiles=$MAVEN_PROFILES || exit 1

# Start JBoss
(cd nuxeo-dam-distribution/target && unzip nuxeo-dam-distribution-*-jboss.zip && mv nuxeo-dam-*-jboss jboss) || exit 1
chmod +x nuxeo-dam-distribution/target/jboss/bin/nuxeoctl || exit 1
nuxeo-dam-distribution/target/jboss/bin/nuxeoctl start || exit 1

# Run selenium tests
HIDE_FF=true ./nuxeo-dam-distribution/ftest/selenium/run.sh
ret1=$?

# Stop JBoss
nuxeo-dam-distribution/target/jboss/bin/nuxeoctl stop || exit 1

# Exit if some tests failed
[ $ret1 -eq 0 ] || exit 9
