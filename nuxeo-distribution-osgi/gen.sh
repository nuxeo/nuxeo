#!/bin/bash

GEN_DIR=tools/nuxeo-project-generator
JAR=${GEN_DIR}/target/nuxeo-project-generator-*.jar

#JAVA_OPTS="$JAVA_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=y"

if [ ! -f $JAR ]; then
    echo "Building project generator";
    pushd $GEN_DIR;
    mvn install;
    popd;
fi

echo "Generating plugins ..."

java $JAVA_OPTS -cp ${JAR} org.nuxeo.build.osgi.gen.ProjectGenerator "-clean" "../../" "plugins/pom.xml" "plugins"

echo "Done."
