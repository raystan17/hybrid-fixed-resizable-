@echo off
echo Opening fork page - click Fork when it loads...
start https://github.com/runelite/plugin-hub/fork
echo.
echo After you click Fork, press any key to push...
pause >nul

cd /d C:\Users\GUEST1\plugin-hub
git remote remove fork 2>nul
git remote add fork https://github.com/raystan17/plugin-hub.git
git push fork hybrid-inventory

if errorlevel 1 (
    echo.
    echo Push failed - make sure you clicked Fork on the page that opened.
    echo Then run this script again.
    pause
    exit /b 1
)

echo.
echo Success! Opening PR page...
start https://github.com/runelite/plugin-hub/compare/master...raystan17:plugin-hub:hybrid-inventory
echo.
echo Click "Create pull request" to submit.
pause
