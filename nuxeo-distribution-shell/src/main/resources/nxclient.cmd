@echo off

set JAVA_OPTS=-Djava.rmi.server.RMIClassLoaderSpi=org.nuxeo.runtime.launcher.NuxeoRMIClassLoader -Dsun.lang.ClassLoader.allowArraySyntax=true
set JAVA_OPTS=%JAVA_OPTS%
rem set JAVA_OPTS=%JAVA_OPTS% -Dorg.nuxeo.runtime.1.3.3.streaming.port=3233

if "%1" == "-debug" set JAVA_OPTS=%JAVA_OPTS% -Xdebug -Xrunjdwp:transport=dt_socket,address=127.0.0.1:8788,server=y,suspend=y

if not "%JAVA_HOME%" == "" goto SET_JAVA

set JAVA=java

echo JAVA_HOME is not set.  Unexpected results may occur.
echo Set JAVA_HOME to the directory of your local JDK to avoid this message.
goto SKIP_SET_JAVA

:SET_JAVA

set JAVA=%JAVA_HOME%\bin\java

:SKIP_SET_JAVA

Setlocal Enabledelayedexpansion
for %%A in ("nuxeo-runtime-launcher-*.jar") do (
    set "FileNameWithoutExtension=%%~nA"
    set "SubString=!FileNameWithoutExtension:*nuxeo-runtime-launcher-=!"
    set NXC_VERSION=!SubString!
)
for %%A in ("bundles\nuxeo-shell-commands-base-*.jar") do (
    set "FileNameWithoutExtension=%%~nA"
    set "SubString=!FileNameWithoutExtension:*bundles\nuxeo-shell-commands-base-=!"
    set NXP_VERSION=!SubString!
)

@echo on

"%JAVA%" %JAVA_OPTS% -jar nuxeo-runtime-launcher-%NXC_VERSION%.jar bundles/nuxeo-runtime-osgi-%NXC_VERSION%.jar/org.nuxeo.osgi.application.Main bundles/.:lib/.:config -bundles=bundles/nuxeo-shell-commands-base-%NXP_VERSION%.jar@3 -home . -console %*
