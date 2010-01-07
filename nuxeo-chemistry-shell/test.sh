#!/bin/sh

echo "This shell script runs some tests against a Nuxeo instance"

./run.sh -b << EOF
connect http://Administrator:Administrator@localhost:8080/nuxeo/site/cmis/repository  

; Test main commands on root
id

; Don't work, maybe should
;ls
;tree
;props

; Test main commands on 'default' object
cd default
id
ls
tree
props

; navigate around
cd default-domain
pwd
ls
cd workspaces
pwd
ls
cd ..
pwd
cd /default/default-domain/workspaces

; Create an object (a folder), test commands on it
mkdir testdir
id testdir
ls testdir
tree testdir
props testdir

; Now a file
cd testdir
mkfile testfile
id testfile
ls testfile
tree testfile
props testfile
getStream testfile

; Clean up
cd ..
rm testdir
EOF

