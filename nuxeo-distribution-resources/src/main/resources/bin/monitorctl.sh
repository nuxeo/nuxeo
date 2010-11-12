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

The script generate the following reports and logs:

  log/misc-*.txt     Miscellaneous information
  log/pgstat-*.txt   VCS statistics and PostgreSQL configuration
  log/nuxeo-conf.txt A copy of the nuxeo.conf file
  log/thread-*.html  The list of JBoss thread CPU utilization and 
                     memory pool usage
  log/systat*.log    System activity during monitoring (sar)
  log/pgsql.log      PostgreSQL log during monitoring
  log/vacuum.log     PostgreSQL vacuum log


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

vacuumdb
    Vacuum PostgreSQL db and gets the vacuum report.
    Perform a reindex after to keep indexes consistant.

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
	echo "No logtail package, won't monitor PostgreSQL log."
    fi
fi
if [ ! -z $LOGTAIL ]; then
    if [ ! -z $PG_LOG ]; then
	if [ -r $PG_LOG ]; then
	    pglog=true
	    PG_LOG_OFFSET="$PID_DIR"/pgsql.offset
	    PG_MON_LOG="$LOG_DIR"/pgsql.log
	fi 
    fi
fi


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
    echo "## os" >> $file
    lsb_release -a >> $file 2> /dev/null 
    echo "## CPUs list" >> $file
    cat /proc/cpuinfo  | grep "model name" >> $file
    echo "## CPU speed" >> $file
    dmesg | grep -i bogomips >> $file 
    echo "## uptime" >> $file
    uptime >> $file
    echo "## free -m" >> $file
    free -m >> $file
    echo "## mount" >> $file
    mount >> $file
    echo "## df" >> $file
    df -h >> $file
    echo "## du -shx $DATA_DIR" >> $file
    du -shx $(readlink -e $DATA_DIR) >> $file
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
	if [ -e $HERE/twiddle.sh ]; then
	    echo "## twiddle.sh get 'jboss.system:type=ServerInfo'" >> $file
	    $HERE/twiddle.sh get "jboss.system:type=ServerInfo" >> $file
	fi
    fi
}


get_pgconf() {
    CONF=$LOG_DIR/nuxeo-conf.txt
    TEMPLATE=`cat $CONF | grep nuxeo.templates | grep postgresql | cut -d= -f2`
    if [ -z $TEMPLATE ]; then
	return
    fi
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
    PGDB="true"
}

log_pgstat() {
    get_pgconf
    [ -z $PGDB ] && return
    file=$1
    PGPASSWORD=$DBPWD psql $DBNAME -U $DBUSER -h $DBHOST -p $DBPORT <<EOF &> /dev/null
    \o $file
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

vacuum() {
    get_pgconf
    rm -f "$LOG_DIR"/vacuum.log
    if [ -z $PGDB ]; then
	die "No PostgreSQL configuration found."
    fi
    echo "Vacuuming $DBNAME `date --rfc-3339=second` ..."
    PGPASSWORD=$DBPWD vacuumdb -fzv $DBNAME -U $DBUSER -h $DBHOST -p $DBPORT &> "$LOG_DIR"/vacuum.log
    echo "Reindexing $DBNAME `date --rfc-3339=second` ..."
    PGPASSWORD=$DBPWD reindexdb $DBNAME -U $DBUSER -h $DBHOST -p $DBPORT &> "$LOG_DIR"/reindexdb.log
    echo "Done `date --rfc-3339=second`"
}

start() {
    # start sar
    if moncheckalive; then
	die "Monitoring is already running with pid `cat $SAR_PID`"
    fi
    echo "Starting monitoring `date --rfc-3339=second` ..."
    [ -r "$SAR_DATA" ] && rm -f $SAR_DATA
    [ -r "$SAR_LOG" ] && rm -f $SAR_LOG
    $SAR -d -o $SAR_DATA $SAR_INTERVAL $SAR_COUNT >/dev/null 2>&1 &
    echo $! > $SAR_PID

    # logtail on pg log
    if [ "$pglog" = "true" ]; then
	[ -r $PG_LOG_OFFSET ] && rm -f $PG_LOG_OFFSET
	[ -r $PG_MON_LOG ] && rm -f $PG_MON_LOG
	$LOGTAIL -f $PG_LOG -o $PG_LOG_OFFSET > /dev/null
    fi

    # misc
    rm -f $LOG_DIR/misc-*.txt
    log_misc $LOG_DIR/misc-start.txt
    
    # get a copy of nuxeo.conf
    grep -Ev '^$|^#' $HERE/nuxeo.conf > $LOG_DIR/nuxeo-conf.txt

    # pg stats
    rm -f $LOG_DIR/pgstat-*.txt
    log_pgstat $LOG_DIR/pgstat-start.txt

    # cpu by thread
    rm -f $LOG_DIR/thread-usage-*.html
    if checkalive; then
	if [ -e $HERE/twiddle.sh ]; then
	    $HERE/twiddle.sh invoke 'jboss.system:type=ServerInfo' listThreadCpuUtilization > $LOG_DIR/thread-usage-start.html
            # mem pool info
	    $HERE/twiddle.sh invoke "jboss.system:type=ServerInfo" listMemoryPools true >> $LOG_DIR/thread-usage-start.html
	fi
    fi
    echo "[`cat $SAR_PID`] Monitoring started."
}

stop() {
    if moncheckalive; then
	echo "Stopping monitoring `date --rfc-3339=second` ..."
	kill -9 `cat "$SAR_PID"`
	sleep 1
	rm -f $SAR_PID
	if [ "$pglog" = "true" ]; then
	    $LOGTAIL -f $PG_LOG -o $PG_LOG_OFFSET > $PG_MON_LOG
	    rm -f $PG_LOG_OFFSET
	fi
        # Convert sar log into text
	LC_ALL=C sar -Ap -f $SAR_DATA > $SAR_LOG
	[ $? ] && rm -f $SAR_DATA
	log_misc $LOG_DIR/misc-end.txt
	log_pgstat $LOG_DIR/pgstat-end.txt

	if [ -e $HERE/twiddle.sh ]; then
            # get cpu and pool info    
	    $HERE/twiddle.sh invoke 'jboss.system:type=ServerInfo' listThreadCpuUtilization > $LOG_DIR/thread-usage-end.html
	    $HERE/twiddle.sh invoke "jboss.system:type=ServerInfo" listMemoryPools true >> $LOG_DIR/thread-usage-end.html
	fi
	echo "Monitoring stopped."
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
    logdir=`basename $LOG_DIR`
    (cd `dirname $LOG_DIR`; tar czf $ARCH_FILE $logdir/*.txt $logdir/*.log $logdir/*.html)
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
    vacuumdb)
	vacuum
	;;
    help)
	help
	;;
    *)
	echo "Usage: monitorctl.sh (start|stop|status|archive [TAG]|heapdump [TAG]|info|vacuumdb|help)"
	;;
esac
