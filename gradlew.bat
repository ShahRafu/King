@echo off
REM gradlew.bat - Gradle wrapper batch file (Windows)

set DIRNAME=%~dp0
set WRAPPER_JAR=%DIRNAME%gradle\wrapper\gradle-wrapper.jar

if not exist "%WRAPPER_JAR%" (
  echo ERROR: gradle-wrapper.jar not found in %DIRNAME%gradle\wrapper
  echo Please upload 'gradle/wrapper/gradle-wrapper.jar' into the repository.
  exit /b 1
)

java -cp "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
