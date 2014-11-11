#!/bin/sh
##
## (C) Copyright 2010-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
##
## All rights reserved. This program and the accompanying materials
## are made available under the terms of the GNU Lesser General Public License
## (LGPL) version 2.1 which accompanies this distribution, and is available at
## http://www.gnu.org/licenses/lgpl.html
##
## This library is distributed in the hope that it will be useful,
## but WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
## Lesser General Public License for more details.
##
## Contributors:
##     Julien Carsique
##
MAX_FD_LIMIT_HELP_URL="http://doc.nuxeo.com/display/KB/java.net.SocketException+Too+many+open+files"

NUXEO_HOME=${NUXEO_HOME:-$(cd $(dirname $0)/../nuxeo-distribution-tomcat/target/nuxeo-dm-5.4.1-SNAPSHOT-tomcat; pwd -P)}
NUXEO_CONF=${NUXEO_CONF:-$NUXEO_HOME/bin/nuxeo.conf}

## OS detection
cygwin=false
darwin=false
linux=false
case "`uname`" in
  CYGWIN*) cygwin=true;;
  Darwin*) darwin=true;;
  Linux) linux=true;;
esac

## Setup the JVM (find JAVA_HOME if undefined; use JAVA if defined)
while read line; do
  if [[ "$line" != \#* ]]; then
    $line
  fi
done <<EOF
`grep JAVA_HOME $NUXEO_CONF`
EOF
if [ -z "$JAVA" ]; then
  JAVA="java"
fi
if [ -z "$JAVA_HOME" ]; then
  JAVA=`which $JAVA`

  # follow symlinks
  while [ -h "$JAVA" ]; do
    ls=`ls -ld "$JAVA"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
      JAVA="$link"
    else
      JAVA=`dirname "$JAVA"`/"$link"
    fi
  done
  JAVA_HOME=`dirname "$JAVA"`
  JAVA_HOME=`dirname "$JAVA_HOME"`
fi
PATH="$JAVA_HOME/bin:$PATH"

while read line; do
  if [[ "$line" != \#* ]]; then
    JAVA_OPTS="$(echo $line|cut -d= -f2-)"
  fi
done <<EOF
`grep JAVA_OPTS $NUXEO_CONF`
EOF
[ -z "$JAVA_OPTS" ] && JAVA_OPTS="$JAVA_OPTS -Xms512m -Xmx1024m -XX:MaxPermSize=512m -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000 -Dfile.encoding=UTF-8"
# Force IPv4
JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Stack=true"
# Set AWT headless
JAVA_OPTS="$JAVA_OPTS -Djava.awt.headless=true"

# If -server not set in JAVA_OPTS, set it, if supported
SERVER_SET=`echo $JAVA_OPTS | grep "\-server"`
if [ "x$SERVER_SET" = "x" ]; then
  # Check for SUN(tm) JVM w/ HotSpot support
  if [ "x$HAS_HOTSPOT" = "x" ]; then
    HAS_HOTSPOT=`"$JAVA" -version 2>&1 | grep -i HotSpot`
  fi
  # Enable -server if we have Hotspot, unless we can't
  if [ "x$HAS_HOTSPOT" != "x" ]; then
    # MacOS does not support -server flag
    if [ "$darwin" != "true" ]; then
      JAVA_OPTS="-server $JAVA_OPTS"
    fi
  fi
fi

## OS specific checks
# Check file descriptor limit is not too low
if [ "$cygwin" = "false" ]; then
  MAX_FD_LIMIT=`ulimit -H -n`
  if [ $? -eq 0 ]; then
    if [ "$darwin" = "true" ] && [ "$MAX_FD_LIMIT" = "unlimited" ]; then
      MAX_FD_LIMIT=`sysctl -n kern.maxfilesperproc`
      ulimit -n $MAX_FD_LIMIT
    fi
    if [ $MAX_FD_LIMIT -lt 2048 ]; then
      warn "Maximum file descriptor limit is too low: $MAX_FD_LIMIT"
      warn "See: $MAX_FD_LIMIT_HELP_URL"
    fi
  else
    warn "Could not get system maximum file descriptor limit (got $MAX_FD_LIMIT)"
  fi
fi

launcher() {
    echo JAVA_HOME=$JAVA_HOME
    echo JAVA_OPTS=$JAVA_OPTS
    echo Launcher command: $JAVA -Dlauncher.java.opts="$JAVA_OPTS" -Dnuxeo.home="$NUXEO_HOME" -Dnuxeo.conf="$NUXEO_CONF" -jar $NUXEO_HOME/bin/nuxeo-launcher.jar $@
    echo
    $JAVA -Dlauncher.java.opts="$JAVA_OPTS" -Dnuxeo.home="$NUXEO_HOME" -Dnuxeo.conf="$NUXEO_CONF" -jar $NUXEO_HOME/bin/nuxeo-launcher.jar $@
}

cp target/nuxeo-launcher-5.4.1-SNAPSHOT-jar-with-dependencies.jar $NUXEO_HOME/bin/nuxeo-launcher.jar

launcher $@
