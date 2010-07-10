#!/bin/bash -x

# Build Nuxeo DAM
ant -f ft-build.xml make-tomcat-distribution -Dmvn.profiles=$MAVEN_PROFILES,http-importer || exit 1

# Start Tomcat
(cd nuxeo-dam-distribution/target && unzip nuxeo-dam-distribution-*-tomcat-importer.zip && mv nuxeo-dam-*-tomcat tomcat) || exit 1
ant -f ft-build.xml start-tomcat || exit 1

# Unzip assets to import
if [ -d "nuxeo-dam-distribution/ftest/selenium/data/toImport" ]; then
    rm -r nuxeo-dam-distribution/ftest/selenium/data/toImport
fi
(cd nuxeo-dam-distribution/ftest/selenium/data && unzip toImport.zip) || exit 1
cp nuxeo-dam-distribution/ftest/selenium/data/metadata.properties nuxeo-dam-distribution/ftest/selenium/data/toImport/

# Import assets
PWD=`pwd`
curl -uAdministrator:Administrator "http://localhost:8080/nuxeo/site/damImporter/run?inputPath=$PWD/nuxeo-dam-distribution/ftest/selenium/data/toImport&interactive=true&nbThreads=1&importSetTitle=Test%20import%20set%20title"

# Run selenium tests
HIDE_FF=true ./nuxeo-dam-distribution/ftest/selenium/run.sh
ret1=$?

# Stop Tomcat
ant -f ft-build.xml stop-tomcat || exit 1

# Exit if some tests failed
[ $ret1 -eq 0 ] || exit 9
