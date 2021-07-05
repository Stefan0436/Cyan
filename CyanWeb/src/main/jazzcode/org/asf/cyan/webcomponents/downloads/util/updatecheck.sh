#!/bin/bash
if ! ping google.com -c 1 -W .25 &>/dev/null; then
    >&2 echo Not connected to the internet, unable to check for updates.
    echo
    exit 1
fi
source versions.info
if [ "$loader" == "vanilla" ]; then
    loader=""
    loaderInfo='.byGameVersions.'\""$minecraft-$repository"\"
    versionInfo="$loaderInfo"
    loadertemplate=""
else
    loaderInfo=".$loader"'Support.'\""$repository-$loader-$minecraft"\"
    versionInfo=".$loader"'Support.'\""$minecraft-&newver-$repository"\"
    loader="-$loader"
    loadertemplate="-&ver"
fi

OLDVER=
OLDCYAN=
if [ -f oldver ]; then OLDVER="$(cat oldver)"; fi
if [ -f oldcyanver ]; then OLDCYAN="$(cat oldcyanver)"; fi

echo Comparing remote versions...
MANIFEST="$(curl https://aerialworks.ddns.net/maven/org/asf/cyan/CyanVersionHolder/generic/CyanVersionHolder-generic-versions.ccfg -s --output -)"

NEWVER=$(echo "$MANIFEST" | cq org.asf.cyan.core.CyanUpdateInfo --source-jar CyanCore.jar - "$loaderInfo" --raw)
NEWCYAN=$(echo "$MANIFEST" | cq org.asf.cyan.core.CyanUpdateInfo --source-jar CyanCore.jar - "${versionInfo//&newver/$NEWVER}" --raw)
OLDINSTALLER=$(sha256sum installer.jar | sed "s/ .*//g")
NEWINSTALLER=$(curl https://aerialworks.ddns.net/cyan/releases/$NEWCYAN/installers/$minecraft/$minecraft${loadertemplate//&ver/$NEWVER}-cyan-$NEWCYAN-installer.jar -s --output - | sha256sum | sed "s/ .*//g")

echo "Remote version: $NEWVER"
echo "Remote cyan version: $NEWCYAN"
if [ "$OLDINSTALLER" == "$NEWINSTALLER" ] && [ "$OLDVER" == "$NEWVER" ] && [ "$OLDCYAN" == "$NEWCYAN" ] && [ -f server.jar ]; then echo No update available.; echo; exit; fi

echo Downloading installer...
rm installer.jar -f
curl -s https://aerialworks.ddns.net/cyan/releases/$NEWCYAN/installers/$minecraft/$minecraft$loader${loadertemplate//&ver/$NEWVER}-cyan-$NEWCYAN-installer.jar --output installer.jar
echo Compiling server...
java -jar installer.jar install server .
if [ "$?" != "0" ]; then
   >&2 echo COMPILER FAILED! Check log
   echo
   exit 1
fi

echo Installing server...
rm -f server.jar
mv "minecraft-$minecraft$loader"-*.jar server.jar
echo "$NEWVER" > oldver
echo "$NEWCYAN" > oldcyanver
echo Updated to $NEWVER.
echo
