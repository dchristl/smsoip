@echo on

set REPO_DIR=%userprofile%\.m2\repository

cd /d %~dp0
rd .\target\lint /s /q
call D:\android-sdk-windows\tools\lint --html .\target\lint --libraries %REPO_DIR%\com\mobclix\mobclix\4.0.3\ .
start %~dp0\target\lint\index.html
