#!/bin/bash
set -e

echo '===================='
echo '- Install packages -'
echo '===================='

# Prevent nuxeoctl from reaching Connect:
# - We rely on local packages.
# - The network might not be available.
# - The Connect server might not be responding.
noConnectProperty=org.nuxeo.connect.server.reachable=false
echo $noConnectProperty >> $NUXEO_CONF

# List and install packages
packagesDir=$1
packages=$(find $packagesDir -name *.zip)
if [ -n "$packages" ]; then
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

# Reset Connect property
sed -i "/$noConnectProperty/d" $NUXEO_CONF

# Clean up package installation directories
backupDir=$NUXEO_HOME/packages/backup
tmpDir=$NUXEO_HOME/packages/tmp
echo "Clean up package installation directories: $packagesDir, $backupDir, $tmpDir"
rm -rf $packagesDir
rm -rf $backupDir
rm -rf $tmpDir
echo

# Set appropriate permissions on distribution directory
chmod -R g+rwX $NUXEO_HOME
