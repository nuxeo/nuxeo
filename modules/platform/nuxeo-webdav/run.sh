#!/bin/sh

mvn -Dexec.classpathScope="test" \
  -Dexec.executable="java" \
  -Dexec.args="-Xmx1024m -classpath %classpath org.nuxeo.ecm.webdav.Server" \
  exec:exec

exit 0

# Use code below if you want to use the Yourkit profiler

YK_ARGS="-agentpath:/Applications/YourKit_Java_Profiler_9.5.0_EAP_build9524.app/bin/mac/libyjpagent.jnilib=disablestacktelemetry,disableexceptiontelemetry,builtinprobes=none,delay=10000"

mvn -Dexec.classpathScope="test" \
  -Dexec.args="$YK_ARGS -Xmx1024m -classpath %classpath org.nuxeo.ecm.webdav.Server" \
  -Dexec.executable="java" \
  exec:exec
