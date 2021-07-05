#!/bin/bash

#
# Cyan AutoRelease Build Script, installs the cyan update on the remote server.
# Only works on AutoRelease enabled repository services.
#
# Also, it needs the autorelease.allow.install file on the server.
#

function prepare() {
    commit="$(git log -1 --pretty=%B)"
    start=${commit//*\#Release/}
    start="$(echo $start)"
    
    if [[ "$start" == CyanWeb* ]]; then
        destination "/etc/connective-http"
        buildOutput "CyanWeb/build/jazzcode"
    else
        destination "/etc/connective-http/cyan-releases"
        buildOutput "build/Wrapper/serverdata/"
    fi
}

function build() {
	commit="$(git log -1 --pretty=%B)"
    start=${commit//*\#Release/}
    start="$(echo $start)"
    
    if [[ "$start" == CyanWeb* ]]; then
        chmod +x gradlew
        ./gradlew -c settings.lite.gradle installLiteLibs
        cd CyanWeb

        chmod +x gradlew
        ./gradlew build
        
        cd build/jazzcode/*
        mv root cyan
        mkdir root
        mv cyan root
        
        cd ../..
        mv jazzcode/*/ jazzcode-new
        rm -r jazzcode
        mv jazzcode-new jazzcode
        cd ..
    else
        gameVersions=( "1.16.5" "1.17" )
        
        chmod +x gradlew buildlocal.sh
        ./gradlew -c settings.lite.gradle installLiteLibs
        
        for ver in "${gameVersions[@]}"; do
            ./gradlew -PcurrentXML -PnoServer=true -PnoClient=true -PuncheckedClient=true -PaddChangeLog="$(cat change.log)" -PoverrideGameVersion="$ver" processResources
            ./gradlew -PcurrentXML -PnoServer=true -PnoClient=true -PuncheckedClient=true -PaddChangeLog="$(cat change.log)" -PoverrideGameVersion="$ver" processResources
            ./gradlew -PcurrentXML -PnoServer=true -PnoClient=true -PuncheckedClient=true -PaddChangeLog="$(cat change.log)" -PoverrideGameVersion="$ver" serverDownloads
        done
    fi
}

function install() {
    if [ "$BUILDDIR" == "CyanWeb/build/jazzcode" ]; then
        rm "$DEST/modules/CyanWeb-"*
    fi
    
	cp -rfv "$BUILDDIR/." "$DEST"
	
    if [ "$BUILDDIR" == "CyanWeb/build/jazzcode" ]; then
        systemctl restart connective-http
    fi
}
