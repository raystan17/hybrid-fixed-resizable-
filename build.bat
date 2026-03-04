@echo off
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.13.11-hotspot"
call gradlew.bat shadowJar
pause
