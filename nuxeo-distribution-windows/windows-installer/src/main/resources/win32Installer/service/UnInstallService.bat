@echo off

rem * JavaService uninstall script for JBoss Application Server
rem *
rem * JavaService - Windows NT Service Daemon for Java applications
rem * Copyright (C) 2004 Multiplan Consultants Ltd. LGPL Licensing applies
rem * Information about the JavaService software is available at the ObjectWeb
rem * web site. Refer to http://javaservice.objectweb.org for more details.

SETLOCAL

rem check that JBoss exists and environment variable is set up
if "%JBOSS_HOME%" == "" goto no_jboss
if not exist "%JBOSS_HOME%\bin" goto no_jboss

rem verify that the JBoss JavaService exe file is available
SET jbossexe="%JBOSS_HOME%\bin\JBossService.exe"
if not exist "%jbossexe%" goto no_jsexe


rem parameters and files seem ok, go ahead with the service uninstall

@echo .

"%jbossexe%" -uninstall JBoss
if ERRORLEVEL 1 goto js_error

del "%jbossexe%"

goto end



:no_jboss
@echo . JavaService uninstall script requires the JBOSS_HOME environment variable
goto error_exit

:no_jsexe
@echo . JBoss JavaService executable file not found, uninstall script cannot be run
goto error_exit

:js_error
@echo . JavaService indicated an error in attempting to uninstall the service
goto error_exit

:error_exit

@echo .
@echo . Failed to uninstall JBoss system service
@echo .


:end
ENDLOCAL
@echo .
@pause
