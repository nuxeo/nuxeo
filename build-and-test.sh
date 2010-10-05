#!/bin/bash -x

# Build Nuxeo Case Management
mvn clean install || exit 1
mvn -Pserver -f nuxeo-platform-document-routing-distribution/pom.xml clean install || exit 1

# start JBoss
(cd nuxeo-platform-document-routing-distribution/target && unzip nuxeo-platform-document-routing-distribution-*.zip && rm *.zip) || exit 1
chmod +x nuxeo-platform-document-routing-distribution/target/nuxeo-dm-server/bin/nuxeoctl || exit 1
nuxeo-platform-document-routing-distribution/target/nuxeo-dm-server/bin/nuxeoctl start || exit 1

# Run selenium tests
HIDE_FF=true nuxeo-platform-document-routing-distribution/ftest/selenium/run.sh
ret1=$?

# Strop JBoss
nuxeo-platform-document-routing-distribution/target/nuxeo-dm-server/bin/nuxeoctl stop || exit 1

# Exit if some tests failed
[ $ret1 -eq 0 ] || exit 9
