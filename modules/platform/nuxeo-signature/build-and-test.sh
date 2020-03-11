#!/bin/bash

#install library
svn export --force https://svn.nuxeo.org/nuxeo/tools/qa/nuxeo-integration-release/trunk/integration-lib.sh
svn export --force https://svn.nuxeo.org/nuxeo/tools/qa/nuxeo-integration-release/trunk/integration-dblib.sh


HERE=$(cd $(dirname $0); pwd -P)
. $HERE/integration-lib.sh

# check no one is running on this server
check_ports_and_kill_ghost_process || exit 1

# Build Nuxeo Platform Signature distribution
mvn -f nuxeo-platform-signature-distribution/pom.xml clean package || exit 1

# start the nuxeo server
chmod +x nuxeo-platform-signature-distribution/target/stage/nuxeo-dm-server/bin/nuxeoctl || exit 1
nuxeo-platform-signature-distribution/target/stage/nuxeo-dm-server/bin/nuxeoctl start || exit 1

# Run selenium tests
HIDE_FF=true nuxeo-platform-signature-distribution/ftest/selenium/run.sh
ret1=$?

# Stop the application server
nuxeo-platform-signature-distribution/target/stage/nuxeo-dm-server/bin/nuxeoctl stop || exit 1


# Exit if some tests failed
[ $ret1 -eq 0 ] || exit 9
