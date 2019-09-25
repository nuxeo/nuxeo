#!/bin/bash
set -e

# Allow supporting arbitrary user id
if ! whoami &> /dev/null; then
  if [ -w /etc/passwd ]; then
    sed /^nuxeo/d /etc/passwd > /tmp/passwd && cp /tmp/passwd /etc/passwd
    echo "${NUXEO_USER:-nuxeo}:x:$(id -u):0:${NUXEO_USER:-nuxeo} user:${NUXEO_HOME}:/sbin/nologin" >> /etc/passwd
  fi
fi

# Set the required nuxeo.conf properties
if [ "$1" = 'nuxeoctl' ]; then
  if [[ ( ! -f $NUXEO_HOME/configured ) ]]; then
    # Can't do that at Java level since it's needed in nuxeoctl scripting
    cat << EOF >> $NUXEO_CONF
nuxeo.data.dir=/var/lib/nuxeo/data
nuxeo.log.dir=/var/log/nuxeo
nuxeo.tmp.dir=/tmp
nuxeo.pid.dir=/var/pid/nuxeo
# Set java.io.tmpdir = \${nuxeo.tmp.dir}
launcher.override.java.tmpdir=true
nuxeo.wizard.done=true
EOF
    touch $NUXEO_HOME/configured
  fi
fi

exec "$@"
