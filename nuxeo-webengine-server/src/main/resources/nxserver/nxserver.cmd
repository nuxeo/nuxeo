@echo off

set JAVA_OPTS=-Djava.rmi.server.RMIClassLoaderSpi=org.nuxeo.runtime.launcher.NuxeoRMIClassLoader -Dsun.lang.ClassLoader.allowArraySyntax=true
set JAVA_OPTS=%JAVA_OPTS% -Dderby.system.home=data/derby

if "%1" == "-debug" set JAVA_OPTS=%JAVA_OPTS% -Xdebug -Xrunjdwp:transport=dt_socket,address=127.0.0.1:8788,server=y,suspend=y

if not "%JAVA_HOME%" == "" goto SET_JAVA

set JAVA=java

echo JAVA_HOME is not set.  Unexpected results may occur.
echo Set JAVA_HOME to the directory of your local JDK to avoid this message.
goto SKIP_SET_JAVA

:SET_JAVA

set JAVA=%JAVA_HOME%\bin\java

:SKIP_SET_JAVA

"%JAVA%" %JAVA_OPTS% -jar nuxeo-runtime-launcher-1.5-SNAPSHOT.jar bundles/nuxeo-runtime-osgi-1.5-SNAPSHOT.jar/org.nuxeo.osgi.application.Main bundles/.:lib/.:config -bundles=bundles/nuxeo-shell-commands-base.jar@3 -home . %*
