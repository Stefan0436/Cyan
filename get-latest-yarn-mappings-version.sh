#!/bin/bash
eval "$(head config.gradle -n12 | tail -n+3 | sed "s/ext\.//g")"

mappingsversion=""
document="$(curl -s "http://maven.modmuss50.me/net/fabricmc/yarn/maven-metadata.xml")"

versions="$(echo "$document" | xq .metadata.versioning.versions.version -r | sed "s/\[/(/g" | sed "s/\]/)/g" | sed "s/,//g")"
newline=$'\n'

versions="${versions//$newline /}"
eval "versions=${versions//$newline/ }"

index=$((${#versions[*]} - 1))
while :
do
    version=${versions[index]}
    if [ "$version" == "" ]; then
        break
    fi
    
    if [[ "$version" =~ ^"$minecraft+".*$ ]]; then
        echo "version \"$version\""
        echo "modloader \"$(curl "https://meta.fabricmc.net/v2/versions/loader/$minecraft" -s --output - | jq ".[0].loader.version" -r)\""
        break
    fi
    
    index=$((index-1))
done
