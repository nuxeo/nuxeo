@echo off
rem #####
rem # Convenience script for Windows
rem #####
@if "%OS%" == "Windows_NT" setlocal

set DIRNAME=.\
if "%OS%" == "Windows_NT" set DIRNAME=%~dp0%

"%DIRNAME%\nuxeoctl.bat" start
