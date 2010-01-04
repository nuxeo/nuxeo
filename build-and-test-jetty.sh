#!/bin/bash -x

# Build Nuxeo DAM
ant -f ft-build.xml make-jetty-distribution -Dmvn.profiles=$MAVEN_PROFILES

# Start Jetty
(cd nuxeo-dam-distribution/nuxeo-dam-distribution-jetty/target && unzip nuxeo-dam-distribution-jetty-*.zip && rm *.zip && mv nuxeo-dam-distribution-jetty-* nuxeo-dam-jetty)
chmod +x nuxeo-dam-distribution/nuxeo-dam-distribution-jetty/target/nuxeo-dam-jetty/*.sh
(cd ./nuxeo-dam-distribution/nuxeo-dam-distribution-jetty/target/nuxeo-dam-jetty/ && nxserverctl.sh start)

# Run selenium tests
HIDE_FF=true ./nuxeo-dam-distribution/nuxeo-dam-ear/ftest/selenium/run.sh
ret1=$?

# Stop Jetty
(cd ./nuxeo-dam-distribution/nuxeo-dam-distribution-jetty/target/nuxeo-dam-jetty/ && nxserverctl.sh stop)

# Exit if some tests failed
[ $ret1 -eq 0 ] || exit 9