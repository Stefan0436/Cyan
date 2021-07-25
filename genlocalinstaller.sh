#!/bin/bash

#
# Written by Sky Swimmer
#

if [ "$1" == "" ] && [ -d build/Wrapper/maven/ ]; then
    1>&2 echo Usage: "./genlocalinstaller.sh <version> [<loader> <version>]"
    1>&2 echo
    1>&2 echo NOTE: This script will not work unless Cyan has been build at least once.
    exit 1
fi

eval "$(head config.gradle -n12 | tail -n+3 | sed "s/ext\.//g")"
chmod +x genlocalrepos.sh

rm -rf build/Installer/libs
cd Installer
./gradlew build || exit 1
cd ..

./gradlew serverDownloads -PuncheckedClient=true -PnoServer=true -PnoClient=true -PoverrideGameVersion="$1" || exit 1

installer=$1
if [ "$2" != "" ]; then
    installer+="-$2"
fi
if [ "$3" != "" ]; then
    installer+="-$3"
fi

project="$(unzip -p "build/Wrapper/serverdata/$cyanversion/installers/$1/$installer-cyan-$cyanversion-installer.jar" project.ccfg | cq org.asf.cyan.ProjectConfig --source-jar "build/Wrapper/serverdata/$cyanversion/installers/$1/$installer-cyan-$cyanversion-installer.jar" - . -s repositories "$(./genlocalrepos.sh | head -n-1 | tail -n+2 | sed "s/    //g")" --ccfg-output)"

cp "build/Installer/libs/Installer-"*"-all.jar" "build/$installer-cyan-$cyanversion-installer-local.jar" -f
echo "$project" > build/project.ccfg
cd build
zip -q "$installer-cyan-$cyanversion-installer-local.jar" project.ccfg
cd ..
rm build/project.ccfg

echo Saved in: "build/$installer-cyan-$cyanversion-installer-local.jar"
echo Done.
