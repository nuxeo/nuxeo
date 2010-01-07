#!/bin/sh

echo "This shell script runs some tests against a Nuxeo instance"

./run.sh -b << EOF
connect http://Administrator:Administrator@localhost:8080/nuxeo/site/cmis/repository  
cd default
ls
cd default-domain
ls
cd workspaces
ls
cd /default/default-domain/workspaces
mkdir test
EOF

./run.sh -b << EOF
connect http://Administrator:Administrator@localhost:8080/nuxeo/site/cmis/repository  
cd /default/default-domain/workspaces
ls
EOF
