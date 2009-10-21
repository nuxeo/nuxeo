#!/bin/bash

if [ ! -f nuxeo-build.jar ]; then
  echo "Downloading nuxeo distribution tools jar"
  mvn nuxeo:download -Dartifact=org.nuxeo.build:nuxeo-distribution-tools:0.1:jar:all -Dfile=target/nuxeo-build.jar
fi

java -Xrunjdwp:transport=dt_socket,address=8001,server=y,suspend=n -jar target/nuxeo-build.jar $@

 