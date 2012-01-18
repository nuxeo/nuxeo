#!/bin/bash

# install library
svn export --force https://svn.nuxeo.org/nuxeo/tools/qa/nuxeo-integration-release/trunk/integration-lib.sh
svn export --force https://svn.nuxeo.org/nuxeo/tools/qa/nuxeo-integration-release/trunk/integration-dblib.sh
HERE=$(cd $(dirname $0); pwd -P)
. $HERE/integration-lib.sh

# check no one is running on this server
check_ports_and_kill_ghost_process || exit 1

# Build Document Platform routing
mvn -f nuxeo-platform-document-routing-distribution/pom.xml clean package || exit 1

# start JBoss
chmod +x nuxeo-platform-document-routing-distribution/target/stage/nuxeo-cap-server/bin/nuxeoctl || exit 1
nuxeo-platform-document-routing-distribution/target/stage/nuxeo-cap-server/bin/nuxeoctl start || exit 1

# Run selenium tests
HIDE_FF=true nuxeo-platform-document-routing-distribution/ftest/selenium/run.sh
ret1=$?

# Strop JBoss
nuxeo-platform-document-routing-distribution/target/stage/nuxeo-cap-server/bin/nuxeoctl stop || exit 1

# Exit if some tests failed
[ $ret1 -eq 0 ] || exit 9
