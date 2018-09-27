#!/bin/bash
ROOT=$(cd $(dirname $0)/..; pwd -P)
LOG4J_CONF=tools-log4j2.xml
CLASSPATH="$ROOT/lib/*:$ROOT/nxserver/lib/*:$ROOT/nxserver/bundles/*"
cd $ROOT
java -cp $CLASSPATH -Dlog4j.configurationFile="$LOG4J_CONF" org.nuxeo.lib.stream.tools.Main "$@"
