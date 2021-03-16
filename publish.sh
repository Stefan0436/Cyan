#!/bin/bash
if [ ! -f "change.log" ]; then echo "<insert changelog here, remove all text first>" > change.log; fi
newline=$'\n'
nano change.log
changelog="$(cat change.log)"
changelog=${changelog//$newline/\\n}

read -rp "Maven username: " username
read -rsp "Maven password: " password
echo

args="-PcurrentXML -PaddChangeLog=\"$changelog\" -Pmavenusername=\"$username\" -Pmavenpassword=\"$password\""

eval './gradlew '"$args"' "$@" -PresetLibSourceCache build' || exit 1
eval './gradlew '"$args"' "$@" build gameData' || exit 1
eval './gradlew '"$args"' "$@" publish'
