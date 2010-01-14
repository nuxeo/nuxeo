#!/bin/sh

mvn -Dexec.classpathScope="test" \
  -Dexec.executable="java" \
  -Dexec.args="-Xmx512m -classpath %classpath org.nuxeo.ecm.webdav.Server" \
  exec:exec

exit 0

# Use code below if you want to use the Yourkit profiler

YK="$HOME/Applications/YourKit.app"

mvn -Dexec.classpathScope="test" \
  -Dexec.args="-agentpath:$YK/bin/mac/libyjpagent.jnilib -Xmx512m -classpath %classpath org.nuxeo.ecm.webdav.Server" \
  -Dexec.executable="java" \
  exec:exec

