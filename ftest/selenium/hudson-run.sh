#!/bin/sh -x
HERE=$(cd $(dirname $0); pwd -P)

JBOSS_HOME=$1
OUTPUT=$2

hostname=${hostname:-0.0.0.0}
port=${port:-8080}
RUNNING_PID=`lsof -i@$hostname:$port -sTCP:LISTEN -n -t`
if [ ! -z $RUNNING_PID ]; then
    echo [WARN] A process is already using port $port: $RUNNING_PID
    echo [WARN] Storing jstack in $PWD/$RUNNING_PID.jstack then killing process
    [ -e /usr/lib/jvm/java-6-sun/bin/jstack ] && /usr/lib/jvm/java-6-sun/bin/jstack $RUNNING_PID >$PWD/$RUNNING_PID.jstack
    kill $RUNNING_PID || kill -9 $RUNNING_PID
fi

echo "start jboss"
chmod +x $JBOSS_HOME/bin/*ctl || exit 1
$JBOSS_HOME/bin/nuxeoctl start || exit 1

echo "run selenium tests"
chmod +x "$HERE"/run.sh $OUTPUT || exit 1
HIDE_FF=true "$HERE"/run.sh $OUTPUT
ret1=$?

echo "stop jboss"
$JBOSS_HOME/bin/nuxeoctl stop || exit 1

exit $ret1
