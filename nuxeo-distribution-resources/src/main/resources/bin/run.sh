#!/bin/sh
#####
#
# Nuxeo backward compliance startup script
#
#####

DIRNAME=`dirname "$0"`

echo
echo WARNING: run.sh is DEPRECATED, please use \"nuxeoctl console\" instead.
echo

sleep 2

echo Trying to start anyway using \"nuxeoctl console\"

"$DIRNAME"/nuxeoctl console $*
