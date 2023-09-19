#!/bin/bash
set -e

nuxeoctl start
if [ ! -f /var/log/nuxeo/server.log ]; then
  echo "File /var/log/nuxeo/server.log not found, make sure 'docker' or 'docker-json' template is not enabled."
  exit 1
fi

tail -fn+1 /var/log/nuxeo/server.log
