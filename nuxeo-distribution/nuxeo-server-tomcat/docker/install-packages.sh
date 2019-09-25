#!/bin/bash
set -e

echo '===================='
echo '- Install packages -'
echo '===================='

if [ "$(ls -A /packages)" ]; then
  packages=$(ls -d /packages/*)
  echo 'Packages to install:'
  cat << EOF
$packages
EOF
  echo
  $NUXEO_HOME/bin/nuxeoctl mp-install --accept yes --nodeps $packages
else
  echo 'Found no packages to install.'
fi
echo
