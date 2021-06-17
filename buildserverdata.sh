#!/bin/bash
gameVersions=( "1.16.5" "1.17" )

chmod +x gradlew buildlocal.sh
./gradlew -c settings.lite.gradle installLiteLibs

for ver in "${gameVersions[@]}"; do
    ./gradlew -PcurrentXML -PnoServer=true -PnoClient=true -PuncheckedClient=true -PaddChangeLog="$(cat change.log)" -PoverrideGameVersion="$ver" processResources
    ./gradlew -PcurrentXML -PnoServer=true -PnoClient=true -PuncheckedClient=true -PaddChangeLog="$(cat change.log)" -PoverrideGameVersion="$ver" processResources
    ./gradlew -PcurrentXML -PnoServer=true -PnoClient=true -PuncheckedClient=true -PaddChangeLog="$(cat change.log)" -PoverrideGameVersion="$ver" serverDownloads
done
