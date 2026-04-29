@echo off
setlocal
set DIR=%~dp0
set JAVA_EXE=%JAVA_HOME%\bin\java.exe
if exist "%JAVA_EXE%" (
  "%JAVA_EXE%" -jar "%DIR%\gradle\wrapper\gradle-wrapper.jar" %*
) else (
  java -jar "%DIR%\gradle\wrapper\gradle-wrapper.jar" %*
)

