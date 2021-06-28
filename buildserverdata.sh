#!/bin/bash
gameVersions=( "1.16.5" "1.17" )

echo Building LiteCyan...
chmod +x gradlew buildlocal.sh
./gradlew -c settings.lite.gradle installLiteLibs
./gradlew -PresetLibSourceCache processResources -PcurrentXML -PnoServer=true -PnoClient=true -PuncheckedClient=true -PaddChangeLog="$(cat change.log)"

for ver in "${gameVersions[@]}"; do
    echo Building for "$ver"...
    echo Processing resources...
    
    ./gradlew -PcurrentXML -PnoServer=true -PnoClient=true -PuncheckedClient=true -PaddChangeLog="$(cat change.log)" -PoverrideGameVersion="$ver" processResources
    ./gradlew -PcurrentXML -PnoServer=true -PnoClient=true -PuncheckedClient=true -PaddChangeLog="$(cat change.log)" -PoverrideGameVersion="$ver" processResources
    
    echo Building server data...
    ./gradlew -PcurrentXML -PnoServer=true -PnoClient=true -PuncheckedClient=true -PaddChangeLog="$(cat change.log)" -PoverrideGameVersion="$ver" serverDownloads
    
    echo
done
