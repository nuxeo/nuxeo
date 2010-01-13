@echo off

set CMD_LINE_ARGS=
:setArgs
if ""%1""=="""" goto doneSetArgs
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setArgs
:doneSetArgs

java %JAVA_OPTS% -jar nuxeo-chemistry-shell.jar %CMD_LINE_ARGS%

