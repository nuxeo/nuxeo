#!/bin/sh

cd /Users/bstefanescu/work/nuxeo/nuxeo-features/nuxeo-automation/nuxeo-automation-shell

java -Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n -cp target/nuxeo-automation-shell-1.0-SNAPSHOT.jar:target/nuxeo-automation-shell-1.0-SNAPSHOT-all.jar org.nuxeo.ecm.shell.Main http://localhost:8080/nuxeo/site/automation


#sleep 10
