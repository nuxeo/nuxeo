#!/bin/bash
set -e

# expand filename patterns which match no files to a null string, rather than themselves
shopt -s nullglob

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

function configure_nuxeo_dev () {
  cat << EOF
org.nuxeo.dev=true
org.nuxeo.rest.stack.enable=true
# for hot reload
nuxeo.server.sdk=true
nuxeo.server.sdkInstallReloadTimer=true
EOF
}

# Handle nuxeo.conf
if [ ! -f $NUXEO_HOME/configured ]; then
  if [ ! -f $NUXEO_CONF ]; then
    echo "ENTRYPOINT: Initialize server configuration without $NUXEO_CONF"

    echo "ENTRYPOINT: Move $NUXEO_HOME/bin/nuxeo.conf to $NUXEO_CONF"
    mv $NUXEO_HOME/bin/nuxeo.conf $NUXEO_CONF

    echo "ENTRYPOINT: Append required properties to $NUXEO_CONF"
    echo -e "\n## ENTRYPOINT: Append required properties" >> $NUXEO_CONF
    configure | tee -a $NUXEO_CONF

    find /etc/nuxeo/conf.d/ -type f | sort | while read i; do
      echo "ENTRYPOINT: Append properties from $i to $NUXEO_CONF"
      echo -e "\n## ENTRYPOINT: Append properties from $i" >> $NUXEO_CONF
      if [ "$NUXEO_DEV" = true ]; then
        cat $i | tee -a $NUXEO_CONF
      else
        cat $i >> $NUXEO_CONF
      fi
    done

    if [ -n "$JAVA_OPTS" ]; then
      echo "ENTRYPOINT: Append JAVA_OPTS environment variable to the JVM options set in $NUXEO_CONF"
      echo -e "\n## ENTRYPOINT: Append JAVA_OPTS environment variable" >> $NUXEO_CONF
      echo "JAVA_OPTS=\$JAVA_OPTS $JAVA_OPTS" | tee -a $NUXEO_CONF
    fi

    # Handle NUXEO_CONNECT_URL
    if [ -n "$NUXEO_CONNECT_URL" ]; then
      echo "ENTRYPOINT: Configure Connect URL with NUXEO_CONNECT_URL environment variable"
      echo -e "\n## ENTRYPOINT: Configure Connect URL with NUXEO_CONNECT_URL environment variable" >> $NUXEO_CONF
      echo "org.nuxeo.connect.url=$NUXEO_CONNECT_URL" | tee -a $NUXEO_CONF
    fi

    # Handle NUXEO_DEV
    if [ "$NUXEO_DEV" = true ]; then
      echo "ENTRYPOINT: Append dev mode properties to $NUXEO_CONF"
      echo -e "\n## ENTRYPOINT: Append dev mode properties" >> $NUXEO_CONF
      configure_nuxeo_dev | tee -a $NUXEO_CONF
    fi
  fi

  # Handle instance.clid
  if [ -n "$NUXEO_CLID" ]; then
    echo "ENTRYPOINT: Write NUXEO_CLID environment variable to /var/lib/nuxeo/instance.clid"
    # Replace -- by a carriage return
    NUXEO_CLID="${NUXEO_CLID//--/\\n}"
    printf "%b\n" "$NUXEO_CLID" > /var/lib/nuxeo/instance.clid
  fi

  # Handle NUXEO_PACKAGES
  if [ -n "$NUXEO_PACKAGES" ]; then
    echo "ENTRYPOINT: Install Nuxeo packages: $NUXEO_PACKAGES"
    installCommand="nuxeoctl mp-install $NUXEO_PACKAGES --accept=true --relax no"
    if [ "$NUXEO_DEV" = true ]; then
      installCommand+=" --debug"
    fi
    $installCommand
  fi

  touch $NUXEO_HOME/configured
fi

# Handle shell scripts
echo "ENTRYPOINT: Looking for shell scripts in /docker-entrypoint-initnuxeo.d"
for f in /docker-entrypoint-initnuxeo.d/*; do
  case "$f" in
    *.sh)  echo "Running $f"; /bin/bash "$f" ;;
    *)     echo "Ignoring $f" ;;
  esac
done

if [ "$NUXEO_DEV" = true ]; then
  echo
  echo "####################################################################################"
  echo "# CAUTION: YOU ARE RUNNING IN DEV MODE, WHICH IS INSECURE AND NOT PRODUCTION-READY #"
  echo "####################################################################################"
  echo
fi

# override the command in dev mode only if the default command was not changed
if [ "$NUXEO_DEV" = true  -a  "$*" = "nuxeoctl console" ]; then
  exec /nuxeo-run-dev.sh
else
  exec "$@"
fi
