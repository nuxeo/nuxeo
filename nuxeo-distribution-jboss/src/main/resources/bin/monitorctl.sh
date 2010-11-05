#/bin/bash
#####
# Nuxeo monitoring script
#
# If PostgreSQL is running on the same host you can add a PG_LOG variable
# in nuxeo.conf to archive pg logs.
#
# Requires: logtail, sysstat
#####

# Load nuxeo conf
HERE=$(cd $(dirname $0); pwd -P)
. $HERE/nuxeoctl status > /dev/null || exit 1

if [ "$linux" != "true" ]; then
  die "Sorry, $0 works only on Linux."
fi

# Check for sar command
SAR=`which sar`
[ -z $SAR ] && die "You need to install sysstat sar package."
SAR_DATA="$LOG_DIR"/sysstat-sar-bin.log
SAR_LOG="$LOG_DIR"/sysstat-sar.log
SAR_PID=$PID_DIR/sysstat-sar.pid
# default is 2h monitoring (5sx1440)
SAR_INTERVAL=5
SAR_COUNT=1440

# Check for logtail
LOGTAIL=`which logtail`
[ -z $LOGTAIL ] && die "You need to install logtail package."
if [ ! -z $PG_LOG ]; then
  if [ -r $PG_LOG ]; then
    pglog=true
  fi 
fi
PG_LOG_OFFSET="$PID_DIR"/pgsql.offset
PG_MON_LOG="$LOG_DIR"/pgsql.log

checkalive() {
  if [ ! -r "$SAR_PID" ]; then
    # we don't mind if sar has terminated
    return 1
  else
    return 0
  fi
}


start() {
  # start sar
  if checkalive; then
    die "Monitoring is already running with pid `cat $SAR_PID`"
  fi
  [ -r "$SAR_DATA" ] && rm -f $SAR_DATA
  [ -r "$SAR_LOG" ] && rm -f $SAR_LOG
  $SAR -d -o $SAR_DATA $SAR_INTERVAL $SAR_COUNT >/dev/null 2>&1 &
  echo $! > $SAR_PID
  # logtail on pg log
  if [ "$pglog" = "true" ]; then
    [ -r $PG_LOG_OFFSET ] && rm -f $PG_LOG_OFFSET
    [ -r $PG_MON_LOG ] && rm -f $PG_MON_LOG
    $LOGTAIL -f $PG_LOG -o $PG_LOG_OFFSET > /dev/null
  else
    echo "No PostgreSQL log file found."
  fi
  echo "[`cat $SAR_PID`] Monitoring started."
}

stop() {
  if checkalive; then
    kill -9 `cat "$SAR_PID"`
    sleep 1
    rm -f $SAR_PID
    echo "Monitoring stopped."
    if [ "$pglog" = "true" ]; then
       $LOGTAIL -f $PG_LOG -o $PG_LOG_OFFSET > $PG_MON_LOG
       rm -f $PG_LOG_OFFSET
    fi
    # Convert sar log into text
    LC_ALL=C sar -Ap -f $SAR_DATA > $SAR_LOG
    [ $? ] && rm -f $SAR_DATA
    archive
    return 0
  else
    echo "Monitoring is not running."
  fi
}

status() {
    if checkalive; then
      echo "Monitoring is running with pid `cat $SAR_PID`"
    else
      echo "Monitoring is not running."
    fi
}

archive() {
   echo "Archiving log ..."
   if [ ! -z "$1" ]; then
     TAG=$1 
   else
     TAG=`date -u '+%Y%m%d-%H%M%S'`
   fi
   ARCH_FILE=$LOG_DIR/log-$TAG.tgz
   (cd `dirname $LOG_DIR`; tar czf $ARCH_FILE `basename $LOG_DIR`/*.log)
   echo "Done: $ARCH_FILE"
}

case "$1" in
  status)
    status
    ;;
  start)
    start
    ;;
  stop)
    stop
    ;;
  restart)
    stop
    start
    ;;
  status)
    status
    ;;
  archive)
    shift
    archive $@
    ;;
  *)
    echo "Usage: monitorctl.sh (start|stop|status|archive [TAG])"
    ;;
esac

