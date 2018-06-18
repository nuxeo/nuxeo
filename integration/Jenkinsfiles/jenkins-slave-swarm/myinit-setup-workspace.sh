#!/bin/bash -x

if [ -d /opt/jenkins/workspace ]; then 
  exit 0
fi
mkdir -p /opt/jenkins/workspace 
chown jenkins:jenkins /opt/jenkins/workspace
