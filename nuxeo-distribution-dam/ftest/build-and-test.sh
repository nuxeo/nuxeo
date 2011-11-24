#!/bin/bash -x

# Build Nuxeo DAM
ant -f ft-build.xml make-distribution -Dmvn.profiles=$MAVEN_PROFILES,http-importer || exit 1

# Start JBoss
(cd ../target && unzip nuxeo-dam-distribution-*-jboss-importer.zip && mv nuxeo-dam-*-jboss jboss && sed -i.bak "s/-Xmx1024m/-Xmx2g/" jboss/bin/nuxeo.conf) || exit 1
ant -f ft-build.xml start-jboss || exit 1

# Unzip assets to import
if [ -d "selenium/data/toImport" ]; then
    rm -r selenium/data/toImport
fi
(cd selenium/data && unzip toImport.zip) || exit 1
cp selenium/data/metadata.properties selenium/data/toImport/

# Run selenium tests
HIDE_FF=true .//selenium/run.sh
ret1=$?

# Stop JBoss
ant -f ft-build.xml stop-jboss || exit

# Exit if some tests failed
[ $ret1 -eq 0 ] || exit 9
