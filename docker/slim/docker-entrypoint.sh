#!/bin/bash
set -e

# Allow supporting arbitrary user IDs
if ! whoami &> /dev/null; then
  if [ -w /etc/passwd ]; then
    sed /^nuxeo/d /etc/passwd > /tmp/passwd && cp /tmp/passwd /etc/passwd
    echo "${NUXEO_USER:-nuxeo}:x:$(id -u):0:${NUXEO_USER:-nuxeo} user:${NUXEO_HOME}:/sbin/nologin" >> /etc/passwd
  fi
fi

function configure () {
  cat << EOF
nuxeo.data.dir=/var/lib/nuxeo
nuxeo.log.dir=/var/log/nuxeo
nuxeo.tmp.dir=/tmp
nuxeo.pid.dir=/var/pid/nuxeo
# Set java.io.tmpdir = \${nuxeo.tmp.dir}
launcher.override.java.tmpdir=true
EOF
}

# Handle nuxeo.conf
if [[ ! -f $NUXEO_HOME/configured && ! -f $NUXEO_CONF ]]; then
  echo "ENTRYPOINT: Initialize server configuration without $NUXEO_CONF"

  echo "ENTRYPOINT: Move $NUXEO_HOME/bin/nuxeo.conf to $NUXEO_CONF"
  mv $NUXEO_HOME/bin/nuxeo.conf $NUXEO_CONF

  echo "ENTRYPOINT: Append required properties to $NUXEO_CONF:"
  echo -e "\n## ENTRYPOINT: Append required properties" >> $NUXEO_CONF
  configure | tee -a $NUXEO_CONF

  find /etc/nuxeo/conf.d/ -type f | sort | while read i; do
    echo "ENTRYPOINT: Append properties from $i to $NUXEO_CONF"
    echo -e "\n## ENTRYPOINT: Append properties from $i" >> $NUXEO_CONF
    cat $i >> $NUXEO_CONF
  done

  if [ -n "$JAVA_OPTS" ]; then
    echo "ENTRYPOINT: Append JAVA_OPTS environment variable to the JVM options set in $NUXEO_CONF:"
    echo -e "\n## ENTRYPOINT: Append JAVA_OPTS environment variable" >> $NUXEO_CONF
    echo "JAVA_OPTS=\$JAVA_OPTS $JAVA_OPTS" | tee -a $NUXEO_CONF
  fi

  touch $NUXEO_HOME/configured
fi

# Handle instance.clid
if [ -n "$NUXEO_CLID" ]; then
  echo "ENTRYPOINT: Write NUXEO_CLID environment variable to /var/lib/nuxeo/instance.clid"
  # Replace -- by a carriage return
  NUXEO_CLID="${NUXEO_CLID/--/\\n}"
  echo "$NUXEO_CLID" > /var/lib/nuxeo/instance.clid
fi

# Handle NUXEO_PACKAGES
if [ -n "$NUXEO_PACKAGES" ]; then
  echo "ENTRYPOINT: Install Nuxeo packages: $NUXEO_PACKAGES"
  nuxeoctl mp-install $NUXEO_PACKAGES --accept=true
fi

exec "$@"
