#!/bin/sh -x
HERE=$(cd $(dirname $0); pwd -P)

JBOSS_HOME=$1
OUTPUT=$2

echo "start jboss"
$JBOSS_HOME/bin/jbossctl start || exit 1

echo "run selenium tests"
HIDE_FF=true "$HERE"/run.sh $OUTPUT
ret1=$?

echo "stop jboss"
$JBOSS_HOME/bin/jbossctl stop || exit 1

exit $ret1
