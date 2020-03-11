#!/bin/bash
###
# Nuxeo monitoring script
warn() {
  echo "WARNING: $*"
}

die() {
  echo "ERROR: $*"
  exit 1
}

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

    monitorctl.sh  OPTIONS

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

heap-histo
    Java heap histo

clear-vcs-cache
    Clear vcs row cache

invoke-fgc
    Perform a full GC

vacuumdb
    Vacuum PostgreSQL db and gets the vacuum report.
    Perform a reindex after to keep indexes consistant.

pgstat-get-reset
    Get postgresql stats and reset



Requirement
============

packages:
  sysstat atop logtail postgresql-client jmxstat bc
EOF
}


cygwin=false
darwin=false
linux=false

case "`uname`" in
  CYGWIN*) cygwin=true;;
  Darwin*) darwin=true;;
  Linux) linux=true;;
esac

if [ "$linux" != "true" ]; then
    die "Sorry, $0 works only on Linux."
fi

HERE=$(cd $(dirname $0); pwd -P)
if [ "x$NUXEO_CONF" = "x" ]; then
  NUXEO_CONF="$HERE/nuxeo.conf"
fi

# Load nuxeo conf
if [ -r "$NUXEO_CONF" ]; then
  while read confline
  do
    if [ "x$confline" = "x" ]; then
      continue
    fi
    if [[ "$confline" == \#* ]]; then
      continue
    fi
    key="$(echo $confline | cut -d= -f1)"
    value="$(echo $confline | cut -d= -f2-)"
    if [[ "$key" == "nuxeo.log.dir" ]]; then
      read -r LOG_DIR <<< $value
    elif [[ "$key" == "nuxeo.pid.dir" ]]; then
      read -r PID_DIR <<< $value
    elif [[ "$key" == "nuxeo.data.dir" ]]; then
      read -r DATA_DIR <<< $value
    elif [[ "$key" == "nuxeo.tmp.dir" ]]; then
      read -r TMP_DIR <<< $value
    elif [[ "$key" == "nuxeo.bind.address" ]]; then
      read -r BIND_ADDRESS <<< $value
    elif [[ "$key" == *.* ]]; then
      continue
    else
      value=${value/nuxeo.log.dir/LOG_DIR}
      value=${value/nuxeo.pid.dir/PID_DIR}
      value=${value/nuxeo.data.dir/DATA_DIR}
      value=${value/nuxeo.tmp.dir/TMP_DIR}
      value=${value/nuxeo.bind.address/BIND_ADDRESS}
      eval "$key=\"$value\""
    fi
  done < $NUXEO_CONF
fi
# Setup NUXEO_HOME
if [ -z "$NUXEO_HOME" ]; then
  if [ -n "$JBOSS_HOME" ]; then NUXEO_HOME="$JBOSS_HOME"
  elif [ -n "$CATALINA_HOME" ]; then NUXEO_HOME="$CATALINA_HOME"
  elif [ -n "$JETTY_HOME" ]; then NUXEO_HOME="$JETTY_HOME"
  else NUXEO_HOME=`cd "$HERE"/..; pwd`
  fi
fi


# Layout setup
# LOG_DIR

if [ -z "$LOG_DIR" ]; then
  LOG_DIR="$NUXEO_HOME/log"
elif [[ "$LOG_DIR" != /* ]]; then
  LOG_DIR="$NUXEO_HOME/$LOG_DIR"
fi
LOG="$LOG_DIR/console.log"

DATA_DIR=${DATA_DIR:-server/default/data/NXRuntime/data}

# PID_DIR

if [ -z "$PID_DIR" ]; then
  PID_DIR="$LOG_DIR"
elif [[ "$PID_DIR" != /* ]]; then
  PID_DIR="$NUXEO_HOME/$PID_DIR"
fi
PID="$PID_DIR/nuxeo.pid"



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
# atop
ATOP_DATA=$LOG_DIR/atop.raw
ATOP_PID=$LOG_DIR/atop.pid
# default is 2h monitoring (5sx1440)
ATOP_INTERVAL=5
ATOP_COUNT=1440

###
# jmxstat
JMXHOST=localhost:1089
JMXSTAT=`which jmxstat`
JMXSTAT_LOG="$LOG_DIR"/jmxstat.log
JMXSTAT_PID="$PID_DIR"/jmxstat.pid
JMXSTAT_INTERVAL=$SAR_INTERVAL
JMXSTAT_COUNT=$SAR_COUNT
JMXSTAT_OPTS="$JMXHOST --contention Catalina:type=DataSource,class=javax.sql.DataSource,name=\"jdbc/nuxeo\"[numActive,numIdle]"
JMXSTAT_VCS_ROW_RESET="--quiet org.nuxeo:name=ecm.core.storage.sql.row.cache.access,type=Counter,management=metric[!reset] org.nuxeo:name=ecm.core.storage.sql.row.cache.hits,type=Counter,management=metric[!reset] org.nuxeo:name=ecm.core.storage.sql.row.cache.get,type=Stopwatch,management=metric[!reset] org.nuxeo:name=ecm.core.storage.sql.sor.gets,type=Stopwatch,management=metric[!reset]"
JMXSTAT_VCS_ROW="--quiet org.nuxeo:name=ecm.core.storage.sql.row.cache.access,type=Counter,management=metric[!sampleAsMap] org.nuxeo:name=ecm.core.storage.sql.row.cache.hits,type=Counter,management=metric[!sampleAsMap] org.nuxeo:name=ecm.core.storage.sql.row.cache.size,type=Counter,management=metric[!sampleAsMap] org.nuxeo:name=ecm.core.storage.sql.row.cache.get,type=Stopwatch,management=metric[!sampleAsMap] org.nuxeo:name=ecm.core.storage.sql.sor.gets,type=Stopwatch,management=metric[!sampleAsMap]"
JMXSTAT_VCS_SEL_RESET=`echo $JMXSTAT_VCS_ROW_RESET | sed 's/row/selection/g'`
JMXSTAT_VCS_SEL=`echo $JMXSTAT_VCS_ROW | sed 's/row/selection/g'`
JMXSTAT_LISTENER_MON='org.nuxeo:name=EventMonitoring,type=service[!setSyncHandlersTrackingEnabled/true,!setAsyncHandlersTrackingEnabled/true,!resetHandlersExecTime]'
JMXSTAT_LISTENER_STAT='org.nuxeo:name=EventMonitoring,type=service[!getListenersConfig,!getAsyncHandlersExecTime,!getSyncHandlersExecTime]'

[ -z $JMXSTAT ] && echo "You can install jmxstat from https://github.com/bdelbosc/jmxstat"

JMX_LISTENING=`netstat -ltn | grep 1089`
[ $? != 0 ] && unset JMXSTAT

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




# Layout setup


# JAR PATH of JMXSH http://code.google.com/p/jmxsh/
# you need to enable JMX on the server on port 1089
JMXSH=${JMXSH:-/usr/local/jmxsh/jmxsh-R5.jar}
if [ ! -r $JMXSH ]; then
  unset JMXSH
fi

checkalive() {
  if [ ! -r "$PID" ]; then
    return 1
  fi
  # NXP-6636
  #MYPID=`cat "$PID"`
  PID=`jps -v | grep "nuxeo.home=$NUXEO_HOME" | cut -f1 -d" "`
  #PS=`ps aux | grep java | grep "nuxeo.home=$NUXEO_HOME" | grep $MYPID`
  if [ "x$PID" = "x" ]; then
    return 1
  else
    return 0
  fi
}

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
    echo "## lscpu" >> $file
    lscpu >> $file
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
    echo "## java -XX:+PrintCommandLineFlags -XX:+PrintGCDetails -version" >> $file
    $JAVA_HOME/bin/java -XX:+PrintCommandLineFlags -XX:+PrintGCDetails -version >> $file 2>&1
    echo "## jps -v" >> $file
    $JAVA_HOME/bin/jps -v >> $file
    echo "## ps aux | grep java" >> $file
    ps aux | grep java >> $file
    echo "## netstat -s" >> $file
    netstat -s >> $file
    if checkalive; then
        # NXP-6636
        #NXPID=`cat "$PID"`
        NXPID=`jps -v | grep "nuxeo.home=$NUXEO_HOME" | cut -f1 -d" "`
        echo "## jmap -heap" >> $file
        $JAVA_HOME/bin/jmap -heap $NXPID >> $file 2> /dev/null
        echo "## jstat -gc" >> $file
        $JAVA_HOME/bin/jstat -gc $NXPID >> $file
        echo "## jstat counter" >> $file
        $JAVA_HOME/bin/jstat -J-Djstat.showUnsupported=true -snap $NXPID >> $file
        if [ -e $HERE/twiddle.sh ]; then
            echo "## twiddle.sh get 'jboss.system:type=ServerInfo'" >> $file
            $HERE/twiddle.sh get "jboss.system:type=ServerInfo" >> $file
        fi
        if [ ! -z $JMXSTAT ]; then
            echo "## VCS cache row stats: access, hits, size (,cache_get, db_gets)" >> $file
            out=`$JMXSTAT $JMXHOST $JMXSTAT_VCS_ROW 1 1 2> /dev/null`
            echo $out >> $file
	    echo "## VCS cache row stats" >> $file
            s_access=`echo $out |  cut -d{ -f2 | sed 's/^.*counter=\([^,]*\),.*$/\1/g'`
            s_hits=`echo $out |  cut -d{ -f3 | sed 's/^.*counter=\([^,]*\),.*$/\1/g'`
            s_size=`echo $out |  cut -d{ -f4 | sed 's/^.*counter=\([^,]*\),.*$/\1/g'`
            s_get=`echo $out |cut -d{ -f5 | sed 's/^.*total=\([^,]*\),.*$/\1/g'`
            s_db=`echo $out |cut -d{ -f6 | sed 's/^.*total=\([^,]*\),.*$/\1/g'`
            echo -e "Access: $s_access\nHits: $s_hits\nSize: $s_size" >> $file
            s_miss=`echo "$s_access - $s_hits" | bc 2> /dev/null`
            s_ratio=`echo "scale=3;$s_hits/$s_access" | bc 2> /dev/null`
            s_ratio_pc=`echo "scale=2;$s_hits*100/$s_access" | bc 2> /dev/null`
            s_getdb=`echo "scale=3;$s_db / $s_miss" | bc 2> /dev/null`
            s_getcache=`echo "scale=3;$s_get / $s_access" | bc 2> /dev/null`
            s_speedup=`echo "scale=2;$s_getdb/$s_getcache" | bc 2> /dev/null`
            s_sys_speedup=`echo "scale=2; 1 / ((1 - $s_ratio) + $s_ratio/$s_speedup)" | bc 2> /dev/null`
            echo -e "HitRatio: $s_ratio_pc%\nSpeedup: $s_speedup\nSystemSpeedup: $s_sys_speedup" >> $file
            echo "## VCS cache selection stats: access, hits, size" >> $file
            out=`$JMXSTAT $JMXHOST $JMXSTAT_VCS_SEL 1 1  2> /dev/null`
            echo $out >> $file
	    echo "## VCS cache selection stats" >> $file
            s_access=`echo $out |  cut -d{ -f2 | sed 's/^.*counter=\([^,]*\),.*$/\1/g'`
            s_hits=`echo $out |  cut -d{ -f3 | sed 's/^.*counter=\([^,]*\),.*$/\1/g'`
            s_size=`echo $out |  cut -d{ -f4 | sed 's/^.*counter=\([^,]*\),.*$/\1/g'`
            s_ratio=`echo "scale=3;$s_hits/$s_access" | bc 2> /dev/null`
            s_ratio_pc=`echo "scale=2;$s_hits*100/$s_access" | bc 2> /dev/null`
            echo -e "Access: $s_access\nHits: $s_hits\nSize: $s_size\nHitRatio: $s_ratio_pc%" >> $file
            echo "## Listener config, stats for async followed by sync" >> $file
            $JMXSTAT $JMXHOST $JMXSTAT_LISTENER_STAT 1 1 2> /dev/null >> $file
        fi
    fi
}


get_pgconf() {
    TEMPLATE=`grep "^nuxeo.templates" $NUXEO_CONF | grep postgresql | cut -d= -f2`
    if [ -z $TEMPLATE ]; then
        return
    fi
    DBPORT=`grep "^nuxeo.db.port" $NUXEO_CONF| cut -d= -f2`
    DBHOST=`grep "^nuxeo.db.host" $NUXEO_CONF | cut -d= -f2`
    DBNAME=`grep "^nuxeo.db.name" $NUXEO_CONF | cut -d= -f2`
    DBUSER=`grep "^nuxeo.db.user" $NUXEO_CONF | cut -d= -f2`
    DBPWD=`grep "^nuxeo.db.password" $NUXEO_CONF | cut -d= -f2`
    if [ -z $DBHOST ]; then
        DBHOST=localhost
    fi
    if [ -z $DBPORT ]; then
        DBPORT=5432
    fi
    PGDB="true"
}


log_pgstat_reset() {
    get_pgconf
    [ -z $PGDB ] && return
    PGPASSWORD=$DBPWD psql $DBNAME -U $DBUSER -h $DBHOST -p $DBPORT <<EOF &> /dev/null
SELECT pg_stat_statements_reset();
\q
EOF
}


log_pgstat_collect() {
    get_pgconf
    [ -z $PGDB ] && return
    file=$1
    PGPASSWORD=$DBPWD psql $DBNAME -U $DBUSER -h $DBHOST -p $DBPORT <<EOF &> /dev/null
    \o $file
SELECT count(1) AS uniq_queries, round(sum(total_time)*1000)/1000 AS sum_total_time, sum(calls) AS sum_calls, round(sum(total_time)/sum(calls)*1000)/1000 AS avg FROM pg_stat_statements;
SELECT round(total_time*1000)/1000 AS total_time, calls, round(total_time/calls*1000)/1000 AS avg, query FROM pg_stat_statements ORDER BY total_time DESC LIMIT 50;
SELECT round(total_time*1000)/1000 AS total_time, calls, round(total_time/calls*1000)/1000 AS avg, query FROM pg_stat_statements ORDER BY calls DESC LIMIT 50;
\o
\q
EOF
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
SELECT (SELECT COUNT(*) FROM users) AS users, (SELECT COUNT(*) FROM user2group) AS user2groups,
    (SELECT COUNT(*) FROM groups) AS group,  (SELECT COUNT(*) FROM group2group) AS group2group;
SELECT stat.relname AS "Table",
    pg_size_pretty(pg_total_relation_size(stat.relid)) AS "Total size",
    pg_size_pretty(pg_relation_size(stat.relid)) AS "Table size",
    CASE WHEN cl.reltoastrelid = 0 THEN 'None' ELSE
        pg_size_pretty(pg_relation_size(cl.reltoastrelid)+
        COALESCE((SELECT SUM(pg_relation_size(indexrelid)) FROM pg_index WHERE indrelid=cl.reltoastrelid)::int8, 0)) END AS "TOAST table size",
    pg_size_pretty(COALESCE((SELECT SUM(pg_relation_size(indexrelid)) FROM pg_index WHERE indrelid=stat.relid)::int8, 0)) AS "Index size",
    CASE WHEN pg_relation_size(stat.relid) = 0 THEN 0.0 ELSE
    round(100 * COALESCE((SELECT SUM(pg_relation_size(indexrelid)) FROM pg_index WHERE indrelid=stat.relid)::int8, 0) /  pg_relation_size(stat.relid)) / 100 END AS "Index ratio"
FROM pg_stat_all_tables stat
  JOIN pg_statio_all_tables statio ON stat.relid = statio.relid
  JOIN pg_class cl ON cl.oid=stat.relid AND stat.schemaname='public'
ORDER BY pg_total_relation_size(stat.relid) DESC
LIMIT 20;
SELECT nspname,relname,
    round(100 * pg_relation_size(indexrelid) / pg_relation_size(indrelid)) / 100 AS index_ratio, pg_size_pretty(pg_relation_size(indexrelid)) AS index_size, pg_size_pretty(pg_relation_size(indrelid)) AS table_size
FROM pg_index I
LEFT JOIN pg_class C ON (C.oid = I.indexrelid)
LEFT JOIN pg_namespace N ON (N.oid = C.relnamespace)
WHERE
  nspname NOT IN ('pg_catalog', 'information_schema', 'pg_toast') AND C.relkind='i' AND pg_relation_size(indrelid) > 0
ORDER BY pg_relation_size(indexrelid) DESC LIMIT 15;
SELECT relname, idx_tup_fetch + seq_tup_read AS total_reads
FROM pg_stat_all_tables WHERE idx_tup_fetch + seq_tup_read != 0
ORDER BY total_reads desc LIMIT 15;
SELECT now() - query_start AS duration, current_query FROM pg_stat_activity
  WHERE current_query != '<IDLE>' ORDER BY duration DESC;
SELECT database, gid FROM pg_prepared_xacts;
SELECT pg_size_pretty(COUNT(*) * 8192) as buffered FROM  pg_buffercache;
SELECT c.relname, pg_size_pretty(count(*) * 8192) as buffered, round(100.0 * count(*) /
(SELECT setting FROM pg_settings
WHERE name='shared_buffers')::integer,1)
AS buffers_percent,
round(100.0 * count(*) * 8192 /
coalesce(pg_relation_size(c.oid),1), 1)
AS percent_of_relation
FROM pg_class c
INNER JOIN pg_buffercache b
ON b.relfilenode = c.relfilenode
INNER JOIN pg_database d
ON (b.reldatabase = d.oid AND d.datname = current_database())
WHERE pg_relation_size(c.oid) != 0
GROUP BY c.oid,c.relname
ORDER BY 3 DESC
LIMIT 20;
SELECT relname, seq_scan, n_live_tup AS rows, idx_scan FROM pg_stat_user_tables ORDER BY seq_scan * n_live_tup DESC LIMIT 20;
SELECT
 schemaname as nspname,
 relname,
 indexrelname AS useless_indexrelname,
 idx_scan,
 pg_size_pretty(pg_relation_size(i.indexrelid)) AS index_size
FROM
 pg_stat_user_indexes i
 JOIN pg_index USING (indexrelid)
WHERE
 indisunique IS false
ORDER BY idx_scan,pg_relation_size(i.indexrelid) DESC LIMIT 10;
SELECT
  sum(heap_blks_read) as heap_read,
  sum(heap_blks_hit)  as heap_hit,
  (sum(heap_blks_hit) - sum(heap_blks_read)) / sum(heap_blks_hit) as ratio
FROM
  pg_statio_user_tables;
SELECT
  relname,
  100 * idx_scan / (seq_scan + idx_scan) percent_of_times_index_used,
  n_live_tup rows_in_table
FROM
  pg_stat_user_tables
ORDER BY
  n_live_tup DESC LIMIT 50;
SELECT
  sum(idx_blks_read) as idx_read,
  sum(idx_blks_hit)  as idx_hit,
  (sum(idx_blks_hit) - sum(idx_blks_read)) / sum(idx_blks_hit) as ratio
FROM
  pg_statio_user_indexes;
\di+
SELECT sum(generate_series) AS "speedTest" FROM generate_series(1,1000000);
EXPLAIN ANALYZE SELECT sum(generate_series) AS "speedTest" FROM generate_series(1,1000000);
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
    PGPASSWORD=$DBPWD LC_ALL=C vacuumdb -fzv $DBNAME -U $DBUSER -h $DBHOST -p $DBPORT &> "$LOG_DIR"/vacuum.log
    echo "Reindexing $DBNAME `date --rfc-3339=second` ..."
    PGPASSWORD=$DBPWD reindexdb $DBNAME -U $DBUSER -h $DBHOST -p $DBPORT &> "$LOG_DIR"/reindexdb.log
    echo "Done `date --rfc-3339=second`"
}

start() {
    # full GC
    invoke_fgc

    # log heap histo
    NXPID=`jps -v | grep "nuxeo.home=$NUXEO_HOME" | cut -f1 -d" "`
    $JAVA_HOME/bin/jmap -histo $NXPID > $LOG_DIR/heap-histo-start.txt

    # start sar
    if moncheckalive; then
        die "Monitoring is already running with pid `cat $SAR_PID`"
    fi
    echo "Starting monitoring `date --rfc-3339=second` ..."
    [ -r "$SAR_DATA" ] && rm -f $SAR_DATA
    [ -r "$SAR_LOG" ] && rm -f $SAR_LOG
    $SAR -d -o $SAR_DATA $SAR_INTERVAL $SAR_COUNT >/dev/null 2>&1 &
    echo $! > $SAR_PID

    [ -r "$JMXSTAT_LOG" ] && rm -f $JMXSTAT_LOG
    if [ ! -z $JMXSTAT ]; then
        $JMXSTAT $JMXSTAT_OPTS $JMXSTAT_INTERVAL $JMXSTAT_COUNT > $JMXSTAT_LOG 2>&1 &
        echo $! > $JMXSTAT_PID
    fi

    # logtail on pg log
    if [ "$pglog" = "true" ]; then
        [ -r $PG_LOG_OFFSET ] && rm -f $PG_LOG_OFFSET
        [ -r $PG_MON_LOG ] && rm -f $PG_MON_LOG
        $LOGTAIL -f $PG_LOG -o $PG_LOG_OFFSET > /dev/null
    fi

    [ -r "$ATOP_DATA" ] && rm -f $ATOP_DATA
    atop -w $ATOP_DATA $ATOP_INTERVAL $ATOP_COUNT >/dev/null 2>&1 &
    echo $! > $ATOP_PID

    # misc
    rm -f $LOG_DIR/misc-*.txt
    log_misc $LOG_DIR/misc-start.txt

    # Reset VCS stats
    if [ ! -z $JMXSTAT ]; then
        $JMXSTAT $JMXHOST $JMXSTAT_VCS_ROW_RESET 1 1 >/dev/null 2>&1 &
        $JMXSTAT $JMXHOST $JMXSTAT_VCS_SEL_RESET 1 1 >/dev/null 2>&1 &
	$JMXSTAT $JMXHOST $JMXSTAT_LISTENER_MON 1 1 >/dev/null 2>&1 &
    fi

    # get a copy of nuxeo.conf
    grep -Ev '^$|^#' $NUXEO_CONF | sed "s/\(password\=\).*$/\1******/g" > $LOG_DIR/nuxeo-conf.txt

    # pg stats
    rm -f $LOG_DIR/pgstat-*.txt
    log_pgstat $LOG_DIR/pgstat-start.txt
    log_pgstat_reset
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
        kill -9 `cat "$JMXSTAT_PID"`
        sleep 1
        rm -f $JMXSTAT_PID
        kill -9 `cat "$ATOP_PID"`
        sleep 1
        rm -f $ATOP_PID

        if [ "$pglog" = "true" ]; then
            $LOGTAIL -f $PG_LOG -o $PG_LOG_OFFSET > $PG_MON_LOG
            rm -f $PG_LOG_OFFSET
        fi
        # Convert sar log into text
        LC_ALL=C sar -Ap -f $SAR_DATA > $SAR_LOG
        [ $? ] && rm -f $SAR_DATA
        log_misc $LOG_DIR/misc-end.txt
        log_pgstat_collect $LOG_DIR/pgstat-statements.txt
        log_pgstat $LOG_DIR/pgstat-end.txt

        if [ -e $HERE/twiddle.sh ]; then
            # get cpu and pool info
            $HERE/twiddle.sh invoke 'jboss.system:type=ServerInfo' listThreadCpuUtilization > $LOG_DIR/thread-usage-end.html
            $HERE/twiddle.sh invoke "jboss.system:type=ServerInfo" listMemoryPools true >> $LOG_DIR/thread-usage-end.html
        fi
        # full GC
        invoke_fgc

        # log heap histo
        NXPID=`jps -v | grep "nuxeo.home=$NUXEO_HOME" | cut -f1 -d" "`
        $JAVA_HOME/bin/jmap -histo $NXPID > $LOG_DIR/heap-histo-end.txt

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
    (cd `dirname $LOG_DIR`; tar czf $ARCH_FILE --exclude='*.tgz' $logdir/*)
    echo "Done: $ARCH_FILE"
}


invoke_fgc() {
    if [ ! -z $JMXSTAT ]; then
       NXPID=`jps -v | grep "nuxeo.home=$NUXEO_HOME" | cut -f1 -d" "`
       echo "Invoking Full GC"
       $JMXSTAT localhost:1089 --performGC $NXPID
    else
       warn "FGC requires jmxstat and a running instance with JMX enable"
    fi
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
        # NXP-6636
        #NXPID=`cat "$PID"`
        NXPID=`jps -v | grep "nuxeo.home=$NUXEO_HOME" | cut -f1 -d" "`
        $JAVA_HOME/bin/jmap -dump:format=b,file=$LOG_DIR/heap-$TAG.bin $NXPID
        echo "You can use the following command to browse the dump:"
        echo " $JAVA_HOME/bin/jhat -J-mx3g -J-ms3g $LOG_DIR/heap-$TAG.bin"
        ;;
    info)
        if ! checkalive; then
            die "No Nuxeo DM running."
        fi
        # NXP-6636
        #NXPID=`cat "$PID"`
        NXPID=`jps -v | grep "nuxeo.home=$NUXEO_HOME" | cut -f1 -d" "`
        $JAVA_HOME/bin/jps -v | grep $NXPID
        $JAVA_HOME/bin/jmap -heap $NXPID
        $JAVA_HOME/bin/jstat -gc $NXPID
        ;;
    vacuumdb)
        vacuum
        ;;
    heap-histo)
        # NXP-6636
        #NXPID=`cat "$PID"`
        NXPID=`jps -v | grep "nuxeo.home=$NUXEO_HOME" | cut -f1 -d" "`
        $JAVA_HOME/bin/jmap -histo $NXPID
        ;;
    pgstat-get-reset)
        file=$2
        log_pgstat_collect $file
        log_pgstat_reset
        ;;
    clear-vcs-cache)
        if [ ! -z $JMXSTAT ]; then
            $JMXSTAT $JMXHOST org.nuxeo:name=SQLStorage,type=service[!clearCaches] 1 1
            #$JMXSTAT $JMXHOST org.nuxeo.ecm.core.storage.sql.management.RepositoryStatusMBean[!clearCaches] 1 1
        fi
        ;;
    invoke-fgc)
        invoke_fgc
        ;;
    disable-cm|disable-contention-monitoring)
        [ -z $JMXSH ] && die "You need to enable JMX and install JMXSH"
        cat <<EOF |  $JAVA_HOME/bin/java -jar $JMXSH
jmx_connect -h localhost -p 1089
set MBEAN java.lang:type=Threading
set ATTROP ThreadContentionMonitoringEnabled
jmx_set false
jmx_close
EOF
        ;;
    enable-cm|enable-contention-monitoring)
        [ -z $JMXSH ] && die "You need to enable JMX and install JMXSH"
        cat <<EOF |  $JAVA_HOME/bin/java -jar $JMXSH
jmx_connect -h localhost -p 1089
set MBEAN java.lang:type=Threading
set ATTROP ThreadContentionMonitoringEnabled
jmx_set true
jmx_close
EOF
        ;;
    help)
        help
        ;;
    *)
        echo "Usage: monitorctl.sh (start|stop|status|archive [TAG]|heapdump [TAG]|info|vacuumdb|heap-histo|pgstat-get-reset OUTPUT_FILE|clear-vcs-cache|invoke-fgc|help)"
        ;;
esac
