@echo off
echo ===================================================
echo Uploading project files to GitHub...
echo ===================================================

git --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Git is not installed on your system.
    echo Please install Git or upload files via GitHub Desktop.
    pause
    exit /b 1
)

echo [1/5] Initializing Git...
git init
git branch -M main

echo [2/5] Adding files...
git add -A

echo [3/5] Creating commit...
git commit -m "Add shopping list project files"

echo [4/5] Setting remote repository...
git remote remove origin >nul 2>&1
git remote add origin https://github.com/nikitatipikin87/shopping-list-app.git

echo [5/5] Pushing to GitHub...
git push -u origin main --force

echo ===================================================
echo SUCCESS! Files pushed to GitHub.
echo ===================================================
pause
