@echo off
rem ##
rem ## (C) Copyright 2010-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
rem ##
rem ## All rights reserved. This program and the accompanying materials
rem ## are made available under the terms of the GNU Lesser General Public License
rem ## (LGPL) version 2.1 which accompanies this distribution, and is available at
rem ## http://www.gnu.org/licenses/lgpl-2.1.html
rem ##
rem ## This library is distributed in the hope that it will be useful,
rem ## but WITHOUT ANY WARRANTY; without even the implied warranty of
rem ## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
rem ## Lesser General Public License for more details.
rem ##
rem ## Contributors:
rem ##     Julien Carsique
rem ##     Mickael Schoentgen
rem ##
rem ## DOS script calling a multi-OS Nuxeo Java launcher
rem ##

@if "%OS%" == "Windows_NT" setlocal

set DIRNAME=.\
if "%OS%" == "Windows_NT" set DIRNAME=%~dp0%

pushd %DIRNAME%..
set NUXEO_HOME=%CD%
popd


REM *****  Check for the Java launcher *****
set NUXEO_LAUNCHER=%NUXEO_HOME%\bin\nuxeo-launcher.jar
if exist "%NUXEO_LAUNCHER%" goto FOUND_NUXEO_LAUNCHER
echo Could not locate %NUXEO_LAUNCHER%.
echo Please check that you are in the bin directory when running this script.
REM Using the full path to prevent issues when the %PATH% is altered by some third-party
REM softwares that ship their own version of the "timeout.exe" executable.
%systemroot%\system32\timeout.exe /t 30
goto END
:FOUND_NUXEO_LAUNCHER


REM *****  Look for nuxeo.conf *****
set ALREADY_SET_NUXEO_CONF=%NUXEO_CONF%
REM ***** Check registry for nuxeo.conf *****
if not exist "%NUXEO_HOME%\bin\ProductName.txt" goto SKIP_REGISTRY
set /p PRODNAME=<"%NUXEO_HOME%\bin\ProductName.txt"
for /F "skip=2 tokens=2*" %%A in ('REG QUERY "HKEY_LOCAL_MACHINE\Software\%PRODNAME%" /v ConfigFile 2^>nul') do set NUXEO_CONF=%%B
if not "%NUXEO_CONF%" == "" if exist "%NUXEO_CONF%" goto FOUND_NUXEO_CONF
for /F "skip=2 tokens=2*" %%A in ('REG QUERY "HKEY_LOCAL_MACHINE\Software\Wow6432Node\%PRODNAME%" /v ConfigFile 2^>nul') do set NUXEO_CONF=%%B
if not "%NUXEO_CONF%" == "" if exist "%NUXEO_CONF%" goto FOUND_NUXEO_CONF
:SKIP_REGISTRY
REM ***** Check environment variable NUXEO_CONF for nuxeo.conf *****
set NUXEO_CONF=%ALREADY_SET_NUXEO_CONF%
if not "%NUXEO_CONF%" == "" if exist "%NUXEO_CONF%" goto FOUND_NUXEO_CONF

REM ***** Check working directory, Desktop, NUXEO_HOME for nuxeo.conf *****
set NUXEO_CONF=%CD%\nuxeo.conf
if exist "%NUXEO_CONF%" goto FOUND_NUXEO_CONF

set NUXEO_CONF=%USERPROFILE%\Desktop\nuxeo.conf
if exist "%NUXEO_CONF%" goto FOUND_NUXEO_CONF

set NUXEO_CONF=%NUXEO_HOME%\bin\nuxeo.conf
if exist "%NUXEO_CONF%" goto FOUND_NUXEO_CONF

REM ***** All checks failed *****
echo Could not find nuxeo.conf in the path, the environment or the registry
%systemroot%\system32\timeout.exe /t 30
goto END

:FOUND_NUXEO_CONF
echo Found NUXEO_CONF = %NUXEO_CONF%
echo. >> "%NUXEO_CONF%" || (
  echo ERROR: "%NUXEO_CONF%" must be writable. Run as the right user or set NUXEO_CONF point to another nuxeo.conf file.
  %systemroot%\system32\timeout.exe /t 30
  goto END
)

REM ***** Read nuxeo.conf *****
VERIFY other 2>nul
SETLOCAL EnableDelayedExpansion
if errorlevel 1 (
  echo Cannot enable delayed expansion - using basic parsing of nuxeo.conf.
  goto NODELAYEDEXPANSION
)
FOR /F "usebackq eol=# tokens=1,* delims==" %%A in ("%NUXEO_CONF%") do (
  if "%%A" == "JAVA_HOME" set LOCAL_JAVA_HOME=%%B
  if "%%A" == "nuxeo.log.dir" set LOCAL_NUXEO_LOG_DIR=%%B
  if "%%A" == "nuxeo.tmp.dir" set LOCAL_NUXEO_TMP_DIR=%%B
  if "%%A" == "JAVA_OPTS" (
    set __JAVA_OPTS=%%B
    REM ***** Expand local string and replace $JAVA_OPTS with %LOCAL_JAVA_OPTS% *****
    set LOCAL_JAVA_OPTS=!LOCAL_JAVA_OPTS!!__JAVA_OPTS:$JAVA_OPTS=%LOCAL_JAVA_OPTS%!
  )
)
( ENDLOCAL
  REM ***** Save local variables to global/system variables *****
  if NOT "%LOCAL_JAVA_HOME%" == "" set "JAVA_HOME=%LOCAL_JAVA_HOME%"
  set "JAVA_OPTS=%LOCAL_JAVA_OPTS%"
  set "NUXEO_LOG_DIR=%LOCAL_NUXEO_LOG_DIR%"
  set "NUXEO_TMP_DIR=%LOCAL_NUXEO_TMP_DIR%"
)
goto NUXEO_CONF_READ
:NODELAYEDEXPANSION
FOR /F "usebackq eol=# tokens=1,* delims==" %%A in ("%NUXEO_CONF%") do (
  if "%%A" == "JAVA_HOME" set JAVA_HOME=%%B
  if "%%A" == "JAVA_OPTS" set JAVA_OPTS=%%B
  if "%%A" == "nuxeo.log.dir" set NUXEO_LOG_DIR=%%B
  if "%%A" == "nuxeo.tmp.dir" set NUXEO_TMP_DIR=%%B
)
:NUXEO_CONF_READ

REM ***** Check log directory *****
if "%NUXEO_LOG_DIR%" == "" set NUXEO_LOG_DIR=%NUXEO_HOME%\log
if not exist "%NUXEO_LOG_DIR%" (
  mkdir "%NUXEO_LOG_DIR%" || goto SET_DEFAULT_LOG_DIR
)
echo. >> "%NUXEO_LOG_DIR%\console.log" || goto SET_DEFAULT_LOG_DIR
goto LOG_DIR_OK

:SET_DEFAULT_LOG_DIR
set NUXEO_LOG_DIR=%APPDATA%\Nuxeo\log
if not exist "%NUXEO_LOG_DIR%" mkdir "%NUXEO_LOG_DIR%"
:LOG_DIR_OK


REM ***** Check tmp directory *****
if "%NUXEO_TMP_DIR%" == "" set NUXEO_TMP_DIR=%TMP%
if not exist "%NUXEO_TMP_DIR%" (
  mkdir "%NUXEO_TMP_DIR%" || goto SET_DEFAULT_TMP_DIR
)
echo. >> "%NUXEO_TMP_DIR%\test_tmp_writable.txt" || goto SET_DEFAULT_TMP_DIR
goto TMP_DIR_OK

:SET_DEFAULT_TMP_DIR
set NUXEO_TMP_DIR=%APPDATA%\Nuxeo\tmp
if not exist "%NUXEO_TMP_DIR%" mkdir "%NUXEO_TMP_DIR%"
:TMP_DIR_OK
set TMP=%NUXEO_TMP_DIR%
set TEMP=%NUXEO_TMP_DIR%

REM ***** Add third-party packages from the installer to the path *****
set PATH=%NUXEO_HOME%\3rdparty\java\bin;%NUXEO_HOME%\3rdparty\ffmpeg\bin;%NUXEO_HOME%\3rdparty\ImageMagick;%PATH%;%NUXEO_HOME%\3rdparty\pdftohtml;%NUXEO_HOME%\3rdparty\gs\bin;%NUXEO_HOME%\3rdparty\misc\bin

REM ***** Check for JAVA_HOME environment variable *****
if not "%JAVA_HOME%" == "" goto HAS_JAVA_HOME

REM *****  Look for java in path *****
set FOUND=
set PROG=java.exe
for %%D in (%PROG%) do (set FOUND=%%~$PATH:D)

if "%FOUND%" == "" goto JAVA_NOT_IN_PATH
set JAVA=%FOUND%
goto HAS_JAVA

:JAVA_NOT_IN_PATH
REM ***** Check for JAVA environment variable *****
if not "%JAVA%" == "" goto HAS_JAVA

REM ***** Look for JDK in registry *****
for /F "skip=2 tokens=2*" %%A in ('REG QUERY "HKEY_LOCAL_MACHINE\Software\JavaSoft\Java Development Kit" /v CurrentVersion 2^>nul') do set CurVer=%%B
for /F "skip=2 tokens=2*" %%A in ('REG QUERY "HKEY_LOCAL_MACHINE\Software\JavaSoft\Java Development Kit\%CurVer%" /v JavaHome 2^>nul') do set JAVA_HOME=%%B
if not "%JAVA_HOME%" == "" goto HAS_JAVA_HOME

for /F "skip=2 tokens=2*" %%A in ('REG QUERY "HKEY_LOCAL_MACHINE\Software\Wow6432Node\JavaSoft\Java Development Kit" /v CurrentVersion 2^>nul') do set CurVer=%%B
for /F "skip=2 tokens=2*" %%A in ('REG QUERY "HKEY_LOCAL_MACHINE\Software\Wow6432Node\JavaSoft\Java Development Kit\%CurVer%" /v JavaHome 2^>nul') do set JAVA_HOME=%%B
if not "%JAVA_HOME%" == "" goto HAS_JAVA_HOME

REM ***** All checks failed *****
echo Could not find java.exe in the path, the environment or the registry
%systemroot%\system32\timeout.exe /t 30
goto END

:FIND_JAVA_HOME
%JAVA% -XshowSettings:properties -version 2>&1 | find "java.home" | "%NUXEO_HOME%\bin\repl.bat" "^ *java.home = (.*)" "set JAVA_HOME=$1"  | "%NUXEO_HOME%\bin\repl.bat" "\\jre$" "" > "%NUXEO_HOME%\bin\java-home.bat"
call "%NUXEO_HOME%\bin\java-home.bat"
goto HAS_JAVA_HOME

:HAS_JAVA_HOME
echo Found JAVA_HOME = %JAVA_HOME%
set PATH=%JAVA_HOME%\bin;%PATH%
set JAVA=%JAVA_HOME%\bin\java.exe
set JAVA_TOOLS=%JAVA_HOME%\lib\tools.jar
if not exist "%JAVA%" (
echo Could not find java.exe in JAVA_HOME\bin. Please fix or remove JAVA_HOME; ensure JDK is properly installed.
%systemroot%\system32\timeout.exe /t 30
goto END
)
goto HAS_JAVA

:HAS_JAVA
if  "%JAVA_HOME%" == "" goto FIND_JAVA_HOME
echo Using JAVA = %JAVA%
REM ***** Check Java version
set REQUIRED_JAVA_VERSION=180
set REQUIRED_JAVA_VERSION_LABEL=1.8.0
for /f tokens^=2-5^ delims^=.-_+^" %%j in ('java -fullversion 2^>^&1') do (
    set "JAVA_VERSION=%%j%%k%%l"
    set "JAVA_VERSION_LABEL=%%j.%%k.%%l"
)
if %JAVA_VERSION% lss %REQUIRED_JAVA_VERSION% (
  echo Nuxeo requires Java JDK %REQUIRED_JAVA_VERSION_LABEL%+ ^(detected %JAVA_VERSION_LABEL%^)
  %systemroot%\system32\timeout.exe /t 30
  goto END
)
set JAVA_VERSION_TOOLS=900
if %JAVA_VERSION% lss %JAVA_VERSION_TOOLS% (
  if not exist "%JAVA_TOOLS%" (
    echo Could not find tools.jar in JAVA_HOME\lib. Please fix or remove JAVA_HOME; ensure JDK is properly installed.
    %systemroot%\system32\timeout.exe /t 30
    goto END
  )
)
REM ***** Check Java JDK
set JAVAC=%JAVA_HOME%\bin\javac.exe
if not exist "%JAVAC%" (
  echo Could not find a JDK. Please ensure a Java JDK is properly installed.
  %systemroot%\system32\timeout.exe /t 30
goto END
)

if "!JAVA_OPTS!" == "" set JAVA_OPTS=-Xms512m -Xmx1024m -Djava.net.preferIPv4Stack=true -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000 -Dfile.encoding=UTF-8
set JAVA_OPTS=%JAVA_OPTS:"=\\\"%

echo [%DATE%] Command: %0 %1 %2 %3 %4 %5 %6 %7 %8 %9 >> "%NUXEO_LOG_DIR%\nuxeoctl.log"

REM set LAUNCHER_DEBUG=-Xdebug -Xrunjdwp:transport=dt_socket,address=8788,server=y,suspend=y

set LOGTIME=%date%_%time%
set LOGTIME=%LOGTIME: =%
set LOGTIME=%LOGTIME::=%
set LOGTIME=%LOGTIME:/=%
set LOGTIME=%LOGTIME:.=%
set LOGTIME=%LOGTIME:,=%

:RESTARTLAUNCHER
:GETTMPLAUNCHER
set TMPLAUNCHER=%NUXEO_TMP_DIR%\nuxeo-launcher-%RANDOM%.jar
if exist "%TMPLAUNCHER%" GOTO GETTMPLAUNCHER
COPY /V "%NUXEO_LAUNCHER%" "%TMPLAUNCHER%"
echo [%DATE%] Launcher command: "%JAVA%" -Dlauncher.java.opts="%JAVA_OPTS%" -Dnuxeo.home="%NUXEO_HOME%" -Dnuxeo.conf="%NUXEO_CONF%" -Dnuxeo.log.dir="%NUXEO_LOG_DIR%" -Dlog.id="-%LOGTIME%" -jar "%TMPLAUNCHER%" %1 %2 %3 %4 %5 %6 %7 %8 %9 >> "%NUXEO_LOG_DIR%\nuxeoctl.log"
echo on

"%JAVA%" %LAUNCHER_DEBUG% "-Xbootclasspath/a:%JAVA_TOOLS%" -Dlauncher.java.opts="%JAVA_OPTS%" -Dnuxeo.home="%NUXEO_HOME%" -Dnuxeo.conf="%NUXEO_CONF%" -Dnuxeo.log.dir="%NUXEO_LOG_DIR%" -Dlog.id="-%LOGTIME%" -jar "%TMPLAUNCHER%" %1 %2 %3 %4 %5 %6 %7 %8 %9
@set exitcode=%ERRORLEVEL%
@echo off
del /F /Q "%TMPLAUNCHER%"
if "%exitcode%" == "128" GOTO RESTARTLAUNCHER
exit /b %exitcode%
:END
exit /b 1
