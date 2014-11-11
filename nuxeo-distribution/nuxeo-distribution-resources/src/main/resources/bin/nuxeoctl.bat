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

set NUXEO_LAUNCHER=%NUXEO_HOME%\bin\nuxeo-launcher.jar
if exist "%NUXEO_LAUNCHER%" goto FOUND_NUXEO_LAUNCHER
echo Could not locate %NUXEO_LAUNCHER%. 
echo Please check that you are in the bin directory when running this script.
goto END

set NUXEO_CONF="%NUXEO_HOME%\bin\nuxeo.conf"
set JAVA_OPTS="-Xms512m -Xmx1024m -XX:MaxPermSize=512m -Djava.net.preferIPv4Stack=true"

:FOUND_NUXEO_LAUNCHER

echo Launcher command: java -Dlauncher.java.opts="%JAVA_OPTS%" -Dnuxeo.home="%NUXEO_HOME%" -Dnuxeo.conf="%NUXEO_CONF%" -jar %NUXEO_LAUNCHER% %*
java -Dlauncher.java.opts="%JAVA_OPTS%" -Dnuxeo.home="%NUXEO_HOME%" -Dnuxeo.conf="%NUXEO_CONF%" -jar %NUXEO_LAUNCHER% %*

:END
