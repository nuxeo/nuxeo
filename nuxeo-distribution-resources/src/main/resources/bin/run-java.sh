#!/bin/sh
#####
#
# Shell script calling a multi-OS Nuxeo startup script
# Will replace run.sh when Java Nuxeo Launcher will be finished 
#
#####

NUXEO_HOME=${NUXEO_HOME:-$(cd $(dirname $0)/..; pwd -P)}
PARAM_NUXEO_HOME="-Dnuxeo.home=$NUXEO_HOME/"
NUXEO_CONF=${NUXEO_CONF:-$NUXEO_HOME/bin/nuxeo.conf}
PARAM_NUXEO_CONF="-Dnuxeo.conf=$NUXEO_CONF"

NUXEO_LAUNCHER=$NUXEO_HOME/bin/nuxeo-launcher.jar
if [ ! -e "$NUXEO_LAUNCHER" ]; then
    echo Could not locate $NUXEO_LAUNCHER. 
    # echo Please check that you are in the bin directory when running this script.
    exit 1
fi

echo java "$PARAM_NUXEO_HOME" "$PARAM_NUXEO_CONF" -jar $NUXEO_LAUNCHER $@
java "$PARAM_NUXEO_HOME" "$PARAM_NUXEO_CONF" -jar $NUXEO_LAUNCHER $@
