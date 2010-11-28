#!/bin/sh

java -Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n \
-Dshell=equinox -cp target/nuxeo-equinox-shell-1.0-SNAPSHOT.jar:../nuxeo-automation-shell/target/nuxeo-automation-shell-1.0-SNAPSHOT-all.jar org.nuxeo.ecm.shell.Main localhost:2401

