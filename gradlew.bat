@rem Gradle startup script for Windows
@rem Use gradlew.bat build to build the project
@if "%DEBUG%"=="" @echo off
@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.

@rem Find java.exe
set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1 || (echo "ERROR: JAVA_HOME is not set" && goto error)

:execute
@rem Execute Gradle
"%DIRNAME%\gradle\wrapper\gradle-wrapper.jar" %CMD_LINE_ARGS%

:end
if "%ERRORLEVEL%"=="0" goto mainEnd
:fail
exit /b 1
:mainEnd
if "%OS%"=="Windows_NT" endlocal
