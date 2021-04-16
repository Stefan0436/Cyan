#!/bin/bash
remote="https://aerialworks.ddns.net/maven"

if [ ! -f "build/Wrapper/resources/main/versions.ccfg" ]; then
    1>&2 echo No version manifest has been build, please run ./gradlew processResources
    exit 1
fi

eval "$(head config.gradle -n12 | tail -n+3 | sed "s/ext\.//g")"

nano build/Wrapper/resources/main/versions.ccfg

read -rp "Maven Username: " username
read -rsp "Maven Password: " password
echo

sha1="$(sha1sum build/Wrapper/resources/main/versions.ccfg)"
md5="$(md5sum build/Wrapper/resources/main/versions.ccfg)"
sha256="$(sha256sum build/Wrapper/resources/main/versions.ccfg)"
sha512="$(sha512sum build/Wrapper/resources/main/versions.ccfg)"

echo "build/Wrapper/resources/main/versions.ccfg -> $remote/org/asf/cyan/CyanVersionHolder/generic/"
cat build/Wrapper/resources/main/versions.ccfg | curl -X PUT --data-binary "@-" "$remote/org/asf/cyan/CyanVersionHolder/generic/CyanVersionHolder-generic-versions.ccfg" -u "$username:$password" || exit 1

echo "SHA-1 -> $remote/org/asf/cyan/CyanVersionHolder/generic/"
echo "$sha1" | curl -X PUT --data-binary "@-" "$remote/org/asf/cyan/CyanVersionHolder/generic/CyanVersionHolder-generic-versions.ccfg.sha1" -u "$username:$password" || exit 1
echo "SHA-256 -> $remote/org/asf/cyan/CyanVersionHolder/generic/"
echo "$sha256" | curl -X PUT --data-binary "@-" "$remote/org/asf/cyan/CyanVersionHolder/generic/CyanVersionHolder-generic-versions.ccfg.sha256" -u "$username:$password" || exit 1
echo "SHA-512 -> $remote/org/asf/cyan/CyanVersionHolder/generic/"
echo "$sha512" | curl -X PUT --data-binary "@-" "$remote/org/asf/cyan/CyanVersionHolder/generic/CyanVersionHolder-generic-versions.ccfg.sha512" -u "$username:$password" || exit 1
echo "MD-5 -> $remote/org/asf/cyan/CyanVersionHolder/generic/"
echo "$md5" | curl -X PUT --data-binary "@-" "$remote/org/asf/cyan/CyanVersionHolder/generic/CyanVersionHolder-generic-versions.ccfg.md5" -u "$username:$password" || exit 1


echo "build/Wrapper/resources/main/versions.ccfg -> $remote/org/asf/cyan/CyanVersionHolder/$cyanversion/"
cat build/Wrapper/resources/main/versions.ccfg | curl -X PUT --data-binary "@-" "$remote/org/asf/cyan/CyanVersionHolder/$cyanversion/CyanVersionHolder-$cyanversion-versions.ccfg" -u "$username:$password" || exit 1

echo "SHA-1 -> $remote/org/asf/cyan/CyanVersionHolder/$cyanversion/"
echo "$sha1" | curl -X PUT --data-binary "@-" "$remote/org/asf/cyan/CyanVersionHolder/$cyanversion/CyanVersionHolder-$cyanversion-versions.ccfg.sha1" -u "$username:$password" || exit 1
echo "SHA-256 -> $remote/org/asf/cyan/CyanVersionHolder/$cyanversion/"
echo "$sha256" | curl -X PUT --data-binary "@-" "$remote/org/asf/cyan/CyanVersionHolder/$cyanversion/CyanVersionHolder-$cyanversion-versions.ccfg.sha256" -u "$username:$password" || exit 1
echo "SHA-512 -> $remote/org/asf/cyan/CyanVersionHolder/$cyanversion/"
echo "$sha512" | curl -X PUT --data-binary "@-" "$remote/org/asf/cyan/CyanVersionHolder/$cyanversion/CyanVersionHolder-$cyanversion-versions.ccfg.sha512" -u "$username:$password" || exit 1
echo "MD-5 -> $remote/org/asf/cyan/CyanVersionHolder/$cyanversion/"
echo "$md5" | curl -X PUT --data-binary "@-" "$remote/org/asf/cyan/CyanVersionHolder/$cyanversion/CyanVersionHolder-$cyanversion-versions.ccfg.md5" -u "$username:$password" || exit 1
