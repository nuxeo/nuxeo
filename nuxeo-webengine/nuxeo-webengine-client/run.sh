#!/bin/sh

JAVA_OPTS="$JAVA_OPTS -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n"

java ${JAVA_OPTS} -jar target/nuxeo-webengine-client-5.2-SNAPSHOT.jar $@
