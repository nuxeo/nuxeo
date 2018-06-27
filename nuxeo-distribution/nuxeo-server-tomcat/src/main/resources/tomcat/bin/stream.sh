#!/bin/bash
ROOT=$(cd $(dirname $0)/..; pwd -P)
LOG4J_CONF=tools-log4j.xml
CLASSPATH="$ROOT/lib/*:$ROOT/nxserver/lib/*:$ROOT/nxserver/bundles/*"
cd $ROOT
java -cp $CLASSPATH -Dlog4j.configuration="$LOG4J_CONF" org.nuxeo.lib.stream.tools.Main "$@"
