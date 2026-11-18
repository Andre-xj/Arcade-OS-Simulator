@echo off
title Arcade OS - 8-Bit Retro Operating System Simulator
echo Starting Arcade OS Simulator...

if exist "C:\Program Files\Java\jdk-24\bin\java.exe" (
    set "PATH=C:\Program Files\Java\jdk-24\bin;%PATH%"
)

if exist ArcadeOS.jar (
    java -jar ArcadeOS.jar
) else (
    if not exist bin\arcadeos\ArcadeOS.class (
        echo Compiling source files...
        call compile.bat
    )
    java -cp "bin" arcadeos.ArcadeOS
)
