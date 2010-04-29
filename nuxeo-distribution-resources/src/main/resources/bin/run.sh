#!/bin/sh
#####
#
# Nuxeo backward compliance startup script
#
#####

DIRNAME=`dirname "$0"`

echo WARNING: run.sh is DEPRECATED, please use 'nuxeoctl console' instead.

"$DIRNAME"/nuxeoctl console $*