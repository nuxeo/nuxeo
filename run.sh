#!/bin/sh

mvn -Dexec.classpathScope="test" \
  -Dexec.args="-classpath %classpath org.nuxeo.ecm.webdav.Server" \
  -Dexec.executable="java" \
  exec:exec

