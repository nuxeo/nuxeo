#!/bin/bash
set -e

usage() {
  exec >&2
  echo "Usage:"
  echo "  install-packages.sh [--clid <clid>] [--connect-url <connecturl>] <packagelist>"
  exit 2
}

echo '===================='
echo '- Install packages -'
echo '===================='

if [[ -f $NUXEO_HOME/configured ]]; then
  echo "Nuxeo is already configured."
  echo "This script must be used in a Dockerfile to install packages at build time."
  exit 2
fi

while [ $# -ne 0 ]; do
  case $1 in
    --clid) clid=$2; shift 2 ;;
    --connect-url) connect_url=$2; shift 2 ;;
    -*) echo "Unknown option: $1" >&2; usage ;;
    *) packages=$@; break ;;
  esac
done
if [ -z "$packages" ]; then echo "No packages to install" >&2; usage; fi

echo
if [ -n "$clid" ]; then
  echo "Setting CLID"
  # Replace -- by a carriage return
  clid="${clid//--/\\n}"
  mkdir -p $NUXEO_HOME/nxserver/data/
  printf "%b\n" "$clid" >> $NUXEO_HOME/nxserver/data/instance.clid
fi

if [ -n "$connect_url" ]; then
  echo "Setting Connect URL: $connect_url"
  printf "org.nuxeo.connect.url=%b\n" "$connect_url" > $NUXEO_HOME/bin/nuxeo.conf
fi

echo
NUXEO_CONF=$NUXEO_HOME/bin/nuxeo.conf $NUXEO_HOME/bin/nuxeoctl mp-install --accept yes --relax no $packages

echo
if [ -n "$clid" ]; then
  echo "Unsetting CLID"
  rm -rf $NUXEO_HOME/nxserver/data/
fi

if [ -n "$connect_url" ]; then
  echo "Unsetting Connect URL"
  sed -i "/org.nuxeo.connect.url=/d" $NUXEO_HOME/bin/nuxeo.conf
fi

# Clean up package installation directories
backupDir=$NUXEO_HOME/packages/backup
tmpDir=$NUXEO_HOME/packages/tmp
echo "Clean up package installation directories: $backupDir, $tmpDir"
rm -rf $backupDir
rm -rf $tmpDir
echo

# Set appropriate permissions on distribution directory
chmod -R g+rwX $NUXEO_HOME
