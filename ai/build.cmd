@echo off
setlocal
call "C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat" >nul
cd /d "%~dp0"
if not exist bin mkdir bin
cl /nologo /std:c++17 /EHsc /utf-8 /O2 /MD /I ..\runtime src\main.cpp /Fo:bin\main.obj /Fe:bin\study-ai.exe ws2_32.lib crypt32.lib
exit /b %errorlevel%
