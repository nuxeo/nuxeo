@echo off
rem #####
rem #
rem # Nuxeo windows startup script
rem # Inspired/copied from RedHat JBoss's run.bat
rem #
rem #####
@if "%OS%" == "Windows_NT" setlocal

set DIRNAME=.\
if "%OS%" == "Windows_NT" set DIRNAME=%~dp0%

set PROGNAME=run.bat
if "%OS%" == "Windows_NT" set PROGNAME=%~nx0%

pushd %DIRNAME%..
set NUXEO_HOME=%CD%
popd

set PARAM_NUXEO_HOME="-Dnuxeo.home=%NUXEO_HOME%"
set PARAM_NUXEO_CONF="-Dnuxeo.home=%NUXEO_HOME%\bin\nuxeo.conf"
set NUXEO_LAUNCHER=%NUXEO_HOME%\bin\nuxeo-launcher.jar
if exist "%NUXEO_LAUNCHER%" goto FOUND_NUXEO_LAUNCHER
echo Could not locate %NUXEO_LAUNCHER%. 
echo Please check that you are in the bin directory when running this script.
goto END

set PARAM_JAVA_OPTS="-Djava.launcher.opts=-Xms512m -Xmx1024m -XX:MaxPermSize=512m"

:FOUND_NUXEO_LAUNCHER

echo Launcher command: java "%PARAM_JAVA_OPTS%" "%PARAM_NUXEO_HOME%" "%PARAM_NUXEO_CONF%" -jar %NUXEO_LAUNCHER% %*
java "%PARAM_JAVA_OPTS%" "%PARAM_NUXEO_HOME%" "%PARAM_NUXEO_CONF%" -jar %NUXEO_LAUNCHER% %*

:END

