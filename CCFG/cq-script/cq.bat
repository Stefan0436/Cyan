@echo off
setlocal EnableDelayedExpansion

SET libs=
for /r "C:\Program Files (x86)\CCFG-CQ-LIBS" %%i in (*.jar) do SET libs=!libs!:%%i
SET libs=%libs:~1%

java -cp "%libs%" org.asf.cyan.api.config.CqMain %*
