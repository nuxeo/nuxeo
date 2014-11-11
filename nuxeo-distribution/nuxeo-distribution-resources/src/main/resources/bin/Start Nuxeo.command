#!/bin/sh

# Convenience script for Mac OS X
BIN_DIR=`dirname "$0"`
chmod +x $BIN_DIR/*.sh $BIN_DIR/*ctl 
$BIN_DIR/nuxeoctl start
