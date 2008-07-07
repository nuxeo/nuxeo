@echo off

set JAVA_OPTS=-Djava.rmi.server.RMIClassLoaderSpi=org.nuxeo.runtime.loader.NuxeoRMIClassLoader -Dsun.lang.ClassLoader.allowArraySyntax=true
set JAVA_OPTS=%JAVA_OPTS% -Dderby.system.home=data/derby

if "%1" == "-debug" set JAVA_OPTS=%JAVA_OPTS% -Xdebug -Xrunjdwp:transport=dt_socket,address=127.0.0.1:8788,server=y,suspend=y

java %JAVA_OPTS% -jar nuxeo-runtime-launcher-1.5-SNAPSHOT.jar bundles/nuxeo-runtime-osgi-1.5-SNAPSHOT.jar/org.nuxeo.osgi.application.Main bundles/.:lib/.:config -home . %*
