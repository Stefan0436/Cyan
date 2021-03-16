#!/bin/bash
args=( "$@" )
extraargs=""
index=0
modloader=
modloaderversion=
skip=false
processnormal=false
gameversion=default

for arg in "${args[@]}"; do
    if [ "$processnormal" == "true" ]; then
        if [[ "$arg" =~ " " ]]; then arg="\"$arg\""; fi
        extraargs+=" $arg"
        continue
    fi
    if [ "$arg" == "--" ]; then
        processnormal=true
    fi
    if [ "$skip" == "true" ]; then
        index=$((index+1))
        skip=false
        continue
    fi
    if [ "$arg" == "--modloader" ]; then
        modloader=${args[index + 1]}
        skip=true
    elif [ "$arg" == "--modloader-version" ]; then
        modloaderversion=${args[index + 1]}
        skip=true
    elif [ "$arg" == "--version" ]; then
        gameversion=${args[index + 1]}
        skip=true
    fi
    index=$((index+1))
done

if [ "$gameversion" != "default" ]; then
    extraargs+=" -PoverrideGameVersion=\"$gameversion\""
fi

eval "$(head config.gradle -n12 | tail -n+3 | sed "s/ext\.//g")"
if [ "$gameversion" == "default" ]; then gameversion=$minecraft; fi

BUILDCMD="publish gameData serverJar"

modloader=${modloader,,}
if [ "$modloader" != "" ]; then
    case $modloader in
        forge)
        if [ "$modloaderversion" == "" ]; then forgeversion="$(curl https://files.minecraftforge.net/maven/net/minecraftforge/forge/promotions_slim.json -s --output - | jq ".promos.\"$gameversion-latest\"" -r)"
        else 
        	1>&2 echo
        	1>&2 echo
        	1>&2 echo WARNING! Setting the FORGE version is unsafe and can cause world damage.
        	1>&2 echo Make sure the mappings are correct before launching the server!
        	1>&2 echo
        	1>&2 echo
        	sleep 3
        	forgeversion=$modloaderversion
        fi
        extraargs+=" -PoverrideLaunchWrapperClient=CyanForgeClientWrapper -PoverrideLaunchWrapperServer=CyanForgeServerWrapper -PsetModLoader=\"forge-$forgeversion\" -PsetInheritsFromVersion=\"$gameversion-forge-$forgeversion\""
        ;;
        fabric)
        if [ "$modloaderversion" == "" ]; then fabricversion="$(curl "https://meta.fabricmc.net/v2/versions/loader/$gameversion" -s --output - | jq ".[0].loader.version" -r)"
        else
        	1>&2 echo
        	1>&2 echo
        	1>&2 echo WARNING! Setting the FABRIC version is unsafe and can cause world damage.
        	1>&2 echo Make sure the mappings are correct before launching the server!
        	1>&2 echo
        	1>&2 echo
        	sleep 3
        	fabricversion=$modloaderversion        	
        fi
        extraargs+=" -PoverrideLaunchWrapperClient=CyanFabricClientWrapper -PoverrideLaunchWrapperServer=CyanFabricServerWrapper -PsetModLoader=\"fabric-loader-$fabricversion\" -PsetInheritsFromVersion=\"fabric-loader-$fabricversion-$gameversion\""
        ;;
        paper)
        BUILDCMD="publish serverJar"
        if [ "$modloaderversion" == "" ]; then paperversion="$(curl "https://papermc.io/api/v2/projects/paper/versions/$gameversion" -s --output - | jq -r ".builds[-1]")"
        else
        	1>&2 echo
        	1>&2 echo
        	1>&2 echo WARNING! Setting the PAPER version is unsafe and can cause world damage.
        	1>&2 echo Make sure the mappings are correct before launching the server!
        	1>&2 echo
        	1>&2 echo
        	sleep 3
        	paperversion=$modloaderversion
        fi
        extraargs+=" -PoverrideLaunchWrapperServer=CyanPaperServerWrapper -PsetModLoader=\"paper-$paperversion\" -PsetInheritsFromVersion=\"$gameversion-paper-$paperversion\""
        ;;
    esac
fi

eval './gradlew '"$extraargs"' -PresetLibSourceCache -PoverrideCyanLibraryURL="" -PcurrentXML processResources && ./gradlew '"$extraargs"' -PoverrideCyanLibraryURL="" -PcurrentXML build '"$BUILDCMD"' && echo && echo Done, saved in build.'