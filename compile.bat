@echo off
title Compile Arcade OS Simulator
echo Compiling Java source files...
if not exist bin mkdir bin

REM Prioritize installed JDK if Oracle javapath stub is present
if exist "C:\Program Files\Java\jdk-24\bin\javac.exe" (
    set "PATH=C:\Program Files\Java\jdk-24\bin;%PATH%"
)

javac -d bin -cp "bin" arcadeos\*.java game\snake\*.java
if %ERRORLEVEL% EQU 0 (
    echo Compilation successful! Generating ArcadeOS.jar...
    jar cfe ArcadeOS.jar arcadeos.ArcadeOS -C bin .
    echo Build complete! Run run.bat or java -jar ArcadeOS.jar to start.
) else (
    echo Compilation failed.
)
