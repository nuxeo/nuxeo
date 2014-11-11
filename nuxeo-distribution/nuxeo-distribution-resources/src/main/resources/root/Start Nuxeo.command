#!/bin/sh

# Convenience script for Mac OS X
BIN_DIR=`dirname "$0"`/bin
chmod +x "$BIN_DIR"/*.sh "$BIN_DIR"/*ctl
exec "$BIN_DIR"/nuxeoctl gui start
