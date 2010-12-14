#!/bin/bash -x

# Build Nuxeo DAM
ant -f ft-build.xml make-tomcat-distribution -Dmvn.profiles=$MAVEN_PROFILES,http-importer || exit 1

# Start Tomcat
(cd ../target && unzip nuxeo-dam-distribution-*-tomcat-importer.zip && mv nuxeo-dam-*-tomcat tomcat) || exit 1
ant -f ft-build.xml start-tomcat || exit 1

# Unzip assets to import
if [ -d "nuxeo-dam-distribution/ftest/selenium/data/toImport" ]; then
    rm -r nuxeo-dam-distribution/ftest/selenium/data/toImport
fi
(cd selenium/data && unzip toImport.zip) || exit 1
cp selenium/data/metadata.properties selenium/data/toImport/

# Run selenium tests
HIDE_FF=true ./selenium/run.sh
ret1=$?

# Stop Tomcat
ant -f ft-build.xml stop-tomcat || exit 1

# Exit if some tests failed
[ $ret1 -eq 0 ] || exit 9
