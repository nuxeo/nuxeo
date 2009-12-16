#!/bin/sh

YK="$HOME/Applications/YourKit.app"

mvn -Dexec.classpathScope="test" \
  -Dexec.args="-agentpath:$YK/bin/mac/libyjpagent.jnilib -Xmx512m -classpath %classpath org.nuxeo.ecm.webdav.Server" \
  -Dexec.executable="java" \
  exec:exec

