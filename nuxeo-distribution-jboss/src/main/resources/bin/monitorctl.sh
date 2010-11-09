#/bin/bash
###
# Nuxeo monitoring script

help() {
    cat <<EOF
monitorctl.sh
==============

A GNU Linux/Nuxeo EP monitoring.

This scipts captures system activities and logs that can be later
processed by logchart
(https://svn.nuxeo.org/nuxeo/tools/qa/logchart/trunk).

The script generate the following new logs:
  log/misc-*.log     miscellaneous information
  log/systat*.log    system activity during monitoring
  log/pgstat-*.log   PostgreSQL VCS store statistics
  log/pgsql.log      PostgreSQL log during monitoring
  log/nuxeo-conf.log The nuxeo.conf file


Usage
======
   
    monitorctl.sh (start|stop|status|archive [TAG])

Options
--------

start
    Remove old monitoring logs and start monitoring.

stop
    Stop monitoring and create a tgz file with all the log files.

status
    Is the monitoring running or not.

heapdump
    Create a heap dump, that can be read by jhat.

info
    Display some jvm info.

Requirement
============

packages: 
  sysstat logtail postgresql-client
EOF
}


# Load nuxeo conf
HERE=$(cd $(dirname $0); pwd -P)
. $HERE/nuxeoctl status > /dev/null || exit 1

if [ "$linux" != "true" ]; then
    die "Sorry, $0 works only on Linux."
fi

### 
# Systat sar
SAR=`which sar`
[ -z $SAR ] && die "You need to install sysstat sar package."
SAR_DATA="$LOG_DIR"/sysstat-sar-bin.log
SAR_LOG="$LOG_DIR"/sysstat-sar.log
SAR_PID=$PID_DIR/sysstat-sar.pid
# default is 2h monitoring (5sx1440)
SAR_INTERVAL=5
SAR_COUNT=1440

###
# PostgreSQL and logtail
LOGTAIL=`which logtail`
if [ -z $LOGTAIL ]; then
    if [ -e /usr/sbin/logtail ]; then
	LOGTAIL='/usr/sbin/logtail'
    else
	die "You need to install logtail package."
    fi
fi
if [ ! -z $PG_LOG ]; then
    if [ -r $PG_LOG ]; then
	pglog=true
    fi 
fi
PG_LOG_OFFSET="$PID_DIR"/pgsql.offset
PG_MON_LOG="$LOG_DIR"/pgsql.log

###
# misc sys 
MISC_LOG="$LOG_DIR/misc-";

moncheckalive() {
    if [ ! -r "$SAR_PID" ]; then
    # we don't mind if sar has terminated
	return 1
    else
	return 0
    fi
}

log_misc() {
    file=$1
    echo "## Misc system info `date --rfc-3339=second`" > $file
    uname -a >> $file
    echo "## CPUs list" >> $file
    cat /proc/cpuinfo  | grep "model name" >> $file
    echo "## uptime" >> $file
    uptime >> $file
    echo "## free -m" >> $file
    free -m >> $file
    echo "## mount" >> $file
    mount >> $file
    echo "## df" >> $file
    df >> $file
    echo "## du JBoss data directory" >> $file
    du -shx $(readlink -e $JBOSS_DATA_DIR) >> $file
    echo "## java -version" >> $file
    $JAVA_HOME/bin/java -version >> $file 2>&1
    echo "## jps -v" >> $file
    $JAVA_HOME/bin/jps -v >> $file
    if checkalive; then
	NXPID=`cat "$PID"`
	echo "## jmap -v" >> $file
	$JAVA_HOME/bin/jmap -heap $NXPID >> $file 2> /dev/null
	echo "## jstat -gc" >> $file
	$JAVA_HOME/bin/jstat -gc $NXPID >> $file
    fi
}

get_nuxeo_conf_value() {
    key=$1
    value=$2
    value=`grep -Ev '^$|^#' $HERE/nuxeo.conf | grep $key | cut -d= -f2`
}

log_pgstat() {
    CONF=$LOG_DIR/nuxeo-conf.log
    TEMPLATE=`cat $CONF | grep nuxeo.templates | grep postgresql | cut -d= -f2`
    [ -z $TEMPLATE ] && return
    DBPORT=`cat $CONF | grep nuxeo.db.port | cut -d= -f2`
    DBHOST=`cat $CONF | grep nuxeo.db.host | cut -d= -f2`
    DBNAME=`cat $CONF | grep nuxeo.db.name | cut -d= -f2`
    DBUSER=`cat $CONF | grep nuxeo.db.user | cut -d= -f2`
    DBPWD=`cat $CONF | grep nuxeo.db.password | cut -d= -f2`
    if [ -z $DBHOST ]; then 
	DBHOST=localhost
    fi
    if [ -z $DBPORT ]; then 
	DBPORT=5432
    fi
    file=$1
    PGPASSWORD=$DBPWD psql $DBNAME -U $DBUSER -h $DBHOST -p $DBPORT <<EOF
    \o $file
\timing
SELECT now(), Version();
SELECT current_database() AS db_name,  pg_size_pretty(pg_database_size(current_database())) AS db_size, pg_size_pretty(SUM(pg_relation_size(indexrelid))::int8) AS index_size FROM pg_index;
SELECT COUNT(*) AS documents_count FROM hierarchy WHERE NOT isproperty;
SELECT primarytype, COUNT(*) AS count FROM hierarchy WHERE NOT isproperty GROUP BY primarytype ORDER BY count DESC;
SELECT COUNT(*) AS hierarchy_count FROM hierarchy;
SELECT COUNT(*) AS aces_count FROM acls;
SELECT COUNT(DISTINCT(id)) AS acls_count FROM acls;
SELECT COUNT(*) AS read_acls_count FROM read_acls;
SELECT  stat.relname AS "Table",
    pg_size_pretty(pg_total_relation_size(stat.relid)) AS "Total size",
    pg_size_pretty(pg_relation_size(stat.relid)) AS "Table size",
    CASE WHEN cl.reltoastrelid = 0 THEN 'None' ELSE
        pg_size_pretty(pg_relation_size(cl.reltoastrelid)+
        COALESCE((SELECT SUM(pg_relation_size(indexrelid)) FROM pg_index WHERE indrelid=cl.reltoastrelid)::int8, 0)) END AS "TOAST table size",
    pg_size_pretty(COALESCE((SELECT SUM(pg_relation_size(indexrelid)) FROM pg_index WHERE indrelid=stat.relid)::int8, 0)) AS "Index size"
FROM pg_stat_all_tables stat
  JOIN pg_statio_all_tables statio ON stat.relid = statio.relid
  JOIN pg_class cl ON cl.oid=stat.relid AND stat.schemaname='public'
ORDER BY pg_total_relation_size(stat.relid) DESC
LIMIT 20;
SELECT name, unit, current_setting(name), source FROM pg_settings WHERE source!='default';
SHOW ALL;
\q
EOF
}

start() {
  # start sar
    if moncheckalive; then
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

  # misc
    rm -rf $LOG_DIR/misc-*.log
    log_misc $LOG_DIR/misc-start.log
    
  # get a copy of nuxeo.conf
    grep -Ev '^$|^#' $HERE/nuxeo.conf > $LOG_DIR/nuxeo-conf.log

  # pg stats
    rm -rf $LOG_DIR/pgstat-*.log
    log_pgstat $LOG_DIR/pgstat-start.log

    echo "[`cat $SAR_PID`] Monitoring started."
}

stop() {
    if moncheckalive; then
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
	log_misc $LOG_DIR/misc-end.log
	log_pgstat $LOG_DIR/pgstat-end.log
	archive
	return 0
    else
	echo "Monitoring is not running."
    fi
}

status() {
    if moncheckalive; then
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
    heapdump)
	if ! checkalive; then
	    die "No Nuxeo DM running."
	fi
	shift
	if [ ! -z "$1" ]; then
	    TAG=$1 
	else
	    TAG=`date -u '+%Y%m%d-%H%M%S'`
	fi   
	NXPID=`cat "$PID"`
	$JAVA_HOME/bin/jmap -dump:format=b,file=$LOG_DIR/heap-$TAG.bin $NXPID
	echo "You can use the following command to browse the dump:"
	echo " $JAVA_HOME/bin/jhat -J-mx3g -J-ms3g $LOG_DIR/heap-$TAG.bin"
	;;
    info)
	if ! checkalive; then
	    die "No Nuxeo DM running."
	fi
	NXPID=`cat "$PID"`
	set -x
	$JAVA_HOME/bin/jps -v | grep $NXPID
	$JAVA_HOME/bin/jmap -heap $NXPID
	$JAVA_HOME/bin/jstat -gc $NXPID
	;;
    help)
	help
	;;
    *)
	echo "Usage: monitorctl.sh (start|stop|status|archive [TAG]|heapdump [TAG]|info|help)"
	;;
esac
