@echo off
SETLOCAL ENABLEEXTENSIONS
Setlocal Enabledelayedexpansion

set JAVA_OPTS=-Djava.rmi.server.RMIClassLoaderSpi=org.nuxeo.runtime.launcher.NuxeoRMIClassLoader -Dsun.lang.ClassLoader.allowArraySyntax=true
set JAVA_OPTS=%JAVA_OPTS% -Dderby.system.home=data/derby

set DEV_OPTS=""

if "%1" == "-dev" (goto :devmode) else (goto :normal_mode) 

:devmode
set CMD_ARGS=%2 %3 %4 %5 %6 %7 %8 %9
set JAVA_OPTS=%JAVA_OPTS% -Dorg.nuxeo.dev=true -Xdebug -Xrunjdwp:transport=dt_socket,address=127.0.0.1:8788,server=y,suspend=n
set DEV_OPTS=-clear -console
goto :launch

:normal_mode
set CMD_ARGS=%*

:launch

if not "%JAVA_HOME%" == "" goto SET_JAVA

set JAVA=java

echo JAVA_HOME is not set.  Unexpected results may occur.
echo Set JAVA_HOME to the directory of your local JDK to avoid this message.
goto SKIP_SET_JAVA

:SET_JAVA

set JAVA=%JAVA_HOME%\bin\java

:SKIP_SET_JAVA

for %%A in ("nuxeo-runtime-launcher-*.jar") do (
    set "FileNameWithoutExtension=%%~nA"
    set "SubString=!FileNameWithoutExtension:*nuxeo-runtime-launcher-=!"
    set NXC_VERSION=!SubString!
)

rem example on how to add external bundles to your environment. Usefull to dev. using IDEs.
rem the eclipse plugin is using this option to start webengine.
rem POST_BUNDLES="-post-bundles /path/to/your/external/bundle:/path/to/second/bundle:/etc"


"%JAVA%" %JAVA_OPTS% -jar nuxeo-runtime-launcher-%NXC_VERSION%.jar bundles/nuxeo-runtime-osgi-%NXC_VERSION%.jar/org.nuxeo.osgi.application.Main bundles/.:lib/.:config %POST_BUNDLES% -home . %DEV_OPTS% %CMD_ARGS%


