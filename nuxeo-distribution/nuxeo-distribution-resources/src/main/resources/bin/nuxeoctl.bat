@echo off
rem ##
rem ## (C) Copyright 2010-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
rem ##
rem ## All rights reserved. This program and the accompanying materials
rem ## are made available under the terms of the GNU Lesser General Public License
rem ## (LGPL) version 2.1 which accompanies this distribution, and is available at
rem ## http://www.gnu.org/licenses/lgpl.html
rem ##
rem ## This library is distributed in the hope that it will be useful,
rem ## but WITHOUT ANY WARRANTY; without even the implied warranty of
rem ## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
rem ## Lesser General Public License for more details.
rem ##
rem ## Contributors:
rem ##     Julien Carsique
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
goto END

:FOUND_NUXEO_CONF
echo Found NUXEO_CONF = %NUXEO_CONF%
echo. >> "%NUXEO_CONF%" || (
  echo ERROR: "%NUXEO_CONF%" must be writable. Run as the right user or set NUXEO_CONF point to another nuxeo.conf file.
  goto END
)

REM ***** Read nuxeo.conf *****
FOR /F "eol=# tokens=1,2 delims==" %%A in ("%NUXEO_CONF%") do (
    if "%%A" == "JAVA_HOME" set JAVA_HOME=%%B
    if "%%A" == "JAVA_OPTS" set JAVA_OPTS=%%B
    if "%%A" == "nuxeo.log.dir" set NUXEO_LOG_DIR=%%B
	if "%%A" == "nuxeo.tmp.dir" set NUXEO_TMP_DIR=%%B
)


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


REM ***** Check for JAVA_HOME environment variable *****
if not "%JAVA_HOME%" == "" goto HAS_JAVA_HOME

REM *****  Look for java in path *****
set FOUND=
set PROG=java.exe
for %%D in (%PROG%) do (set FOUND=%%~$PATH:D)

if "%FOUND%" == "" goto JAVA_NOT_IN_PATH
echo Found in path : %FOUND%
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

REM ***** Look for JRE in registry *****
for /F "skip=2 tokens=2*" %%A in ('REG QUERY "HKEY_LOCAL_MACHINE\Software\JavaSoft\Java Runtime Environment" /v CurrentVersion 2^>nul') do set CurVer=%%B
for /F "skip=2 tokens=2*" %%A in ('REG QUERY "HKEY_LOCAL_MACHINE\Software\JavaSoft\Java Runtime Environment\%CurVer%" /v JavaHome 2^>nul') do set JAVA_HOME=%%B
if not "%JAVA_HOME%" == "" goto HAS_JAVA_HOME

for /F "skip=2 tokens=2*" %%A in ('REG QUERY "HKEY_LOCAL_MACHINE\Software\Wow6432Node\JavaSoft\Java Runtime Environment" /v CurrentVersion 2^>nul') do set CurVer=%%B
for /F "skip=2 tokens=2*" %%A in ('REG QUERY "HKEY_LOCAL_MACHINE\Software\Wow6432Node\JavaSoft\Java Runtime Environment\%CurVer%" /v JavaHome 2^>nul') do set JAVA_HOME=%%B
if not "%JAVA_HOME%" == "" goto HAS_JAVA_HOME

REM ***** All checks failed *****
echo Could not find java.exe in the path, the environment or the registry
goto END

:HAS_JAVA_HOME
echo Found JAVA_HOME = %JAVA_HOME%
set JAVA=%JAVA_HOME%\bin\java.exe
if exist "%JAVA%" goto HAS_JAVA
echo Could not find java.exe in JAVA_HOME\bin. Please fix or remove JAVA_HOME; ensure Java 6 is properly installed.
goto END

:HAS_JAVA
echo Using JAVA = %JAVA%

if "%JAVA_OPTS%" == "" set JAVA_OPTS=-Xms512m -Xmx1024m -XX:MaxPermSize=256m -Djava.net.preferIPv4Stack=true -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000 -Dfile.encoding=UTF-8
REM ***** Add third-party packages from the installer to the path *****
set PATH=%NUXEO_HOME%\3rdparty\ffmpeg\bin;%NUXEO_HOME%\3rdparty\ImageMagick\bin;%PATH%;%NUXEO_HOME%\3rdparty\pdftohtml;%NUXEO_HOME%\3rdparty\gs\bin


echo [%DATE%] Command: %0 %1 %2 %3 %4 >> "%NUXEO_LOG_DIR%\nuxeoctl.log"
REM *****  Check for gui/nogui parameter *****
if "%1" == "nogui" (
  goto GUI_NO
) else if "%1" == "gui" (
  goto GUI_DONE
) else if "%1" == "--gui=false" (
  goto GUI_DONE
) else if "%1" == "--gui=true" (
  goto GUI_DONE
) else if "%1" == "--gui" (
  if "%2" == "false" (
    SHIFT
    goto GUI_NO
  ) else if "%2" == "true" (
    SHIFT
    goto GUI_YES
  ) else (
    SHIFT
    set GUI_OPTION=--gui=true
    goto GUI_DONE
  )
) else goto ADD_GUI
:GUI_NO
SHIFT
set GUI_OPTION=--gui=false
goto GUI_DONE
:GUI_YES
SHIFT
set GUI_OPTION=--gui=true
goto GUI_DONE
:ADD_GUI
set GUI_OPTION=--gui=true
:GUI_DONE

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
echo [%DATE%] Launcher command: "%JAVA%" -Dlauncher.java.opts="%JAVA_OPTS%" -Dnuxeo.home="%NUXEO_HOME%" -Dnuxeo.conf="%NUXEO_CONF%" -Dnuxeo.log.dir="%NUXEO_LOG_DIR%" -Dlog.id="-%LOGTIME%" -jar "%TMPLAUNCHER%" %GUI_OPTION% %1 %2 %3 %4 %5 %6 %7 %8 %9 >> "%NUXEO_LOG_DIR%\nuxeoctl.log"
echo on
"%JAVA%" %LAUNCHER_DEBUG% -Dlauncher.java.opts="%JAVA_OPTS%" -Dnuxeo.home="%NUXEO_HOME%" -Dnuxeo.conf="%NUXEO_CONF%" -Dnuxeo.log.dir="%NUXEO_LOG_DIR%" -Dlog.id="-%LOGTIME%" -jar "%TMPLAUNCHER%" %GUI_OPTION% %1 %2 %3 %4 %5 %6 %7 %8 %9
@set exitcode=%ERRORLEVEL%
@echo off
del /F /Q "%TMPLAUNCHER%"
if "%exitcode%" == "128" GOTO RESTARTLAUNCHER

:END
