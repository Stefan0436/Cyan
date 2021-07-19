#!/bin/bash
eval "$(head config.gradle -n12 | tail -n+3 | sed "s/ext\.//g")"

gameVersions=( "1.16.5" "1.17" "1.17.1" )

echo Building LiteCyan...
chmod +x gradlew buildlocal.sh
./gradlew -c settings.lite.gradle installLiteLibs || exit 1
./gradlew -PresetLibSourceCache processResources -PcurrentXML -PnoServer=true -PnoClient=true -PuncheckedClient=true -PmergeVersionManifests="build/Wrapper/serverdata/$cyanversion/versions.ccfg" -PaddChangeLog="$(cat change.log)" || exit 1

for ver in "${gameVersions[@]}"; do
    echo Building for "$ver"...
    echo Processing resources...
    
    ./gradlew -PcurrentXML -PnoServer=true -PnoClient=true -PuncheckedClient=true -PaddChangeLog="$(cat change.log)" -PmergeVersionManifests="build/Wrapper/serverdata/$cyanversion/versions.ccfg" -PoverrideGameVersion="$ver" processResources || exit 1
    ./gradlew -PcurrentXML -PnoServer=true -PnoClient=true -PuncheckedClient=true -PaddChangeLog="$(cat change.log)" -PmergeVersionManifests="build/Wrapper/serverdata/$cyanversion/versions.ccfg" -PoverrideGameVersion="$ver" processResources || exit 1
    
    echo Building server data...
    ./gradlew -PcurrentXML -PnoServer=true -PnoClient=true -PuncheckedClient=true -PaddChangeLog="$(cat change.log)" -PmergeVersionManifests="build/Wrapper/serverdata/$cyanversion/versions.ccfg" -PoverrideGameVersion="$ver" serverDownloads || exit 1
    
    echo
done
