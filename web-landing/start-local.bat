@echo off
cd /d "%~dp0"
echo Report It will be available at http://127.0.0.1:8000/app.html
echo Keep this window open while using the website.
php -S 127.0.0.1:8000
