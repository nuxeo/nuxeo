#!/bin/bash

GEN_DIR=tools/nuxeo-project-generator
JAR=${GEN_DIR}/target/nuxeo-project-gen-1.0.jar

if [ ! -f $JAR ]; then
    echo "Building project generator";
    pushd $GEN_DIR;
    mvn install;
    popd;
fi

echo "Generating projects ..."

java -cp ${JAR} org.nuxeo.build.osgi.gen.ProjectGenerator "-clean" "../../" "projects/pom.xml" "projects"

echo "Done."
