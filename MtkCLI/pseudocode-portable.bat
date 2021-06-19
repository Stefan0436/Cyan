@echo off
setlocal EnableDelayedExpansion

SET libs=MtkCLI.jar
for /r "C:\Program Files (x86)\MTK" %%i in (*.jar) do SET libs=!libs!;%%i

java -cp "%libs%" org.asf.cyan.PcCLI %*
