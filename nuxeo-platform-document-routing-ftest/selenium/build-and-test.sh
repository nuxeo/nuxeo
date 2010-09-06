#!/bin/bash -x

HERE=$(cd $(dirname $0); pwd -P)

# Retrieve Nuxeo CM Distribution, nuxeo-platform-document-routing jars and selenium-server.jar
(cd .. && mvn clean dependency:copy -P$MAVEN_PROFILES) || exit 1

# Start JBoss
cd ../target
unzip nuxeo-case-management-distribution-*.zip || exit 1
mv nuxeo-cm-server* jboss || exit 1
mv nuxeo-platform-document-routing* jboss/server/default/deploy/nuxeo.ear/plugins/ || exit 1
chmod +x jboss/bin/nuxeoctl || exit 1
jboss/bin/nuxeoctl start || exit 1

# Run selenium tests
cd $HERE
./run.sh
ret1=$?

# Stop JBoss
(cd ../target && jboss/bin/nuxeoctl stop) || exit 1

# Exit if some tests failed
[ $ret1 -eq 0 ] || exit 9
