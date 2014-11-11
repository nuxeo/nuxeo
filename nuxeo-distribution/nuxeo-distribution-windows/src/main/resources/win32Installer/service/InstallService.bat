@echo off


SETLOCAL

SET JAVA_HOME=C:\Progra~1\Java\jdk1.5.0_14
SET JBOSS_HOME=C:\Progra~1\Nuxeo5~1\NuxeoServer

SET jvmdll=%JAVA_HOME%\jre\bin\server\jvm.dll
SET toolsjar=%JAVA_HOME%\lib\tools.jar
SET jbossjar=%JBOSS_HOME%\bin\run.jar


SET svcmode="-manual"
SET dependson=""

echo "%JBOSS_HOME%\bin\JbossService.exe" -install JBoss "%jvmdll%" -Djava.class.path="%jbossjar%;%toolsjar%" -Xms128M -Xmx512M -XX:MaxPermSize=128m -start org.jboss.Main -b 0.0.0.0 -stop org.jboss.Main -method systemExit -out "%JBOSS_HOME%\bin\out.log" -err "%JBOSS_HOME%\bin\err.log" -current "%JBOSS_HOME%\bin" %svcmode% -overwrite -startup 6
"%JBOSS_HOME%\bin\JbossService.exe" -install JBoss "%jvmdll%" -Djava.class.path="%jbossjar%;%toolsjar%" -Xms128M -Xmx512M -XX:MaxPermSize=128m -start org.jboss.Main -b 0.0.0.0 -stop org.jboss.Main -method systemExit -out "%JBOSS_HOME%\bin\out.log" -err "%JBOSS_HOME%\bin\err.log" -current "%JBOSS_HOME%\bin" %svcmode% -overwrite -startup 6
