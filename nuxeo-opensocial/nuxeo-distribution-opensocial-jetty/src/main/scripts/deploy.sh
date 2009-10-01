#!/bin/bash
# Script: deploys.sh
# Author: Damien METZLER <damien.metzler@leroymerlin.fr>
# Deploys the RPM artifact on a given host
# To work, the SSH public key of the CI server must be
# in the destination accepted SSH keys
#
PORTAL_TEST_HOST=$1



usage() {
  echo "Usage : deploy.sh [rpmFile] [host]"
  echo "      rpmDir: the directory where i can find the RPM artifact"
  echo "      to deploy on host"
  echo "      host: the destination host"
  echo ""
  echo "      To have a fully non interactive script, you have"
  echo "      to declare this host's public SSH key (~/.ssh/id_rsa.pub) "
  echo "      in the destination's host authorized keys (~/.ssh/authorized_keys) "
  exit
}

main() {
  if [ "e" == "e$1" ]; then
    usage
    exit 1
  fi
  if [ "e" == "e$2" ]; then
    usage
    exit 1
  fi

  RPM_DIR=$1
  DEST_HOST=$2

  if [ ! -d $RPM_DIR ]; then
     echo "$RPM_DIR does not exists"
     usage
     exit 1
  fi

  RPM_FILE=`ls -rt $RPM_DIR | tail -n 1`

  if [ ! -f $RPM_DIR/$RPM_FILE ]; then
     echo "Unable to find the RPM artifact in $RPM_DIR"
     usage
     exit 1
  fi


  scp $RPM_DIR/$RPM_FILE root@$DEST_HOST:/tmp/
  ssh root@$DEST_HOST <<EOT
    rpm -Uvh /tmp/$RPM_FILE 
    RESULT=$?
    rm -f /tmp/$RPM_FILE
EOT


}

main $1 $2
