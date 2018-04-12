#!/bin/bash
ROOT=$(cd $(dirname $0)/..; pwd -P)
CLASSPATH=$ROOT/lib/jcl-over-slf4j-1.7.21.jar:\
$ROOT/lib/slf4j-api-1.7.21.jar:\
$ROOT/lib/slf4j-log4j12-1.7.21.jar:\
$ROOT/lib/log4j-1.2.17.jar:\
$ROOT/nxserver/lib/commons-cli-1.4.jar:\
$ROOT/nxserver/lib/chronicle-queue-4.6.44.jar:\
$ROOT/nxserver/lib/kafka-clients-1.0.0.jar:\
$ROOT/nxserver/lib/kafka_2.11-1.0.0.jar:\
$ROOT/nxserver/lib/scala-library-2.11.7.jar:\
$ROOT/nxserver/lib/zookeeper-3.4.8.jar:\
$ROOT/nxserver/lib/jgrapht-core-1.0.1.jar:\
$ROOT/nxserver/lib/jackson-databind-2.7.4.jar:\
$ROOT/nxserver/lib/jackson-core-2.7.4.jar:\
$ROOT/nxserver/lib/jackson-annotations-2.7.4.jar:\
$ROOT/nxserver/bundles/nuxeo-importer-stream-10.2-SNAPSHOT.jar:\
$ROOT/nxserver/bundles/nuxeo-core-api-10.2-SNAPSHOT.jar:\
$ROOT/nxserver/lib/nuxeo-stream-10.2-SNAPSHOT.jar
cd $ROOT
java -cp $CLASSPATH org.nuxeo.lib.stream.tools.Main "$@"
