@echo off
cd /d "%~dp0"

call .\gradlew.bat clean build
docker compose up --build