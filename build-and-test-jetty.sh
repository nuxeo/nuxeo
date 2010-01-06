#!/bin/bash -x

# Build Nuxeo DAM
ant -f ft-build.xml make-jetty-distribution -Dmvn.profiles=$MAVEN_PROFILES || exit 1

# Start Jetty
(cd nuxeo-dam-distribution/nuxeo-dam-distribution-jetty/target && unzip nuxeo-dam-distribution-jetty-*.zip && rm *.zip && mv nuxeo-dam-distribution-jetty-* nuxeo-dam-jetty) || exit 1
chmod +x nuxeo-dam-distribution/nuxeo-dam-distribution-jetty/target/nuxeo-dam-jetty/*.sh || exit 1
(cd ./nuxeo-dam-distribution/nuxeo-dam-distribution-jetty/target/nuxeo-dam-jetty/ && nxserverctl.sh start) || exit 1

# Run selenium tests
HIDE_FF=true ./nuxeo-dam-distribution/nuxeo-dam-ear/ftest/selenium/run.sh
ret1=$?

# Stop Jetty
(cd ./nuxeo-dam-distribution/nuxeo-dam-distribution-jetty/target/nuxeo-dam-jetty/ && nxserverctl.sh stop) || exit 1

# Exit if some tests failed
[ $ret1 -eq 0 ] || exit 9
