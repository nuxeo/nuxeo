#!/bin/sh

HERE=`pwd`

# Start server
nuxeo-distribution/nuxeo-distribution-tomcat/target/nuxeo-dm-*-SNAPSHOT-tomcat/bin/nuxeoctl start

# Run tests
cd nuxeo-distribution/nuxeo-distribution-dm/ftest/selenium 
if [ -f /usr/bin/xvfb-run ]
then
    HIDE_FF=true xvfb-run ./run.sh
else
    HIDE_FF=true ./run.sh
fi

# Stop server

cd $HERE
nuxeo-distribution/nuxeo-distribution-tomcat/target/nuxeo-dm-*-SNAPSHOT-tomcat/bin/nuxeoctl stop

