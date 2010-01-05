#!/bin/sh

JAVA_OPTS="$JAVA_OPTS -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n"

java ${JAVA_OPTS} -jar target/nuxeo-chemistry-shell-1.6.1-SNAPSHOT.jar $@
