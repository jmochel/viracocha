@echo off
setlocal EnableExtensions

rem Launch Viracocha CLI (`vira`) from the repo: runs the packaged fat JAR in target\.
rem Update JAR_NAME if pom.xml ^<version^> changes.

set "SCRIPT_DIR=%~dp0"
set "ROOT=%SCRIPT_DIR%.."
cd /d "%ROOT%" || exit /b 1

set "JAR_NAME=viracocha-0.1.jar"
set "JAR=%CD%\target\%JAR_NAME%"

if not exist "%JAR%" (
    echo vira: %JAR_NAME% not found - building ^(mvn package^)...
    call "%CD%\mvnw.cmd" -q -DskipTests package || exit /b 1
)

java -jar "%JAR%" %*
