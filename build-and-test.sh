#!/bin/bash

HERE=$(cd $(dirname $0); pwd -P)
. $HERE/integration-lib.sh
TMP_DIR=/tmp
TEST_PDF_FILE_NAME=original.pdf

# check no one is running on this server
check_ports_and_kill_ghost_process || exit 1

# Build Nuxeo Platform Signature distribution 
mvn -f nuxeo-platform-signature-distribution/pom.xml clean package || exit 1
TEST_PDF_ORIG=nuxeo-platform-signature-distribution/ftest/selenium/samples/$TEST_PDF_FILE_NAME
[ -f $TEST_PDF_ORIG ] && echo "original pdf found"
cp $TEST_PDF_ORIG $TMP_DIR
echo "$TEST_PDF_ORIG copied to $TMP_DIR"

# start the nuxeo server
chmod +x nuxeo-platform-signature-distribution/target/stage/nuxeo-dm-server/bin/nuxeoctl || exit 1
nuxeo-platform-signature-distribution/target/stage/nuxeo-dm-server/bin/nuxeoctl start || exit 1

# Run selenium tests
HIDE_FF=true nuxeo-platform-signature-distribution/ftest/selenium/run.sh
ret1=$?

# Stop the application server
nuxeo-platform-signature-distribution/target/stage/nuxeo-dm-server/bin/nuxeoctl stop || exit 1

rm $TMP_DIR/$TEST_PDF_FILE_NAME

# Exit if some tests failed
[ $ret1 -eq 0 ] || exit 9
