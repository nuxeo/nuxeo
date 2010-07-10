#!/bin/bash -x

# Build Nuxeo DAM
ant -f ft-build.xml make-distribution -Dmvn.profiles=$MAVEN_PROFILES,http-importer || exit 1

# Start JBoss
(cd nuxeo-dam-distribution/target && unzip nuxeo-dam-distribution-*-jboss-importer.zip && mv nuxeo-dam-*-jboss jboss) || exit 1
chmod +x nuxeo-dam-distribution/target/jboss/bin/nuxeoctl || exit 1
nuxeo-dam-distribution/target/jboss/bin/nuxeoctl start || exit 1

# Unzip assets to import
(cd nuxeo-dam-distribution/ftest/selenium/data && unzip toImport.zip) || exit 1
cp nuxeo-dam-distribution/ftest/selenium/data/metadata.properties nuxeo-dam-distribution/ftest/selenium/data/toImport/

# Import assets
PWD=`pwd`
curl -uAdministrator:Administrator "http://localhost:8080/nuxeo/site/damImporter/run?inputPath=$PWD/nuxeo-dam-distribution/ftest/selenium/data/toImport&interactive=true&nbThreads=1&importSetTitle=Test%20import%20set%20title"

# Run selenium tests
HIDE_FF=true ./nuxeo-dam-distribution/ftest/selenium/run.sh
ret1=$?

# Stop JBoss
nuxeo-dam-distribution/target/jboss/bin/nuxeoctl stop || exit 1

# Exit if some tests failed
[ $ret1 -eq 0 ] || exit 9
