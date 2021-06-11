#!/bin/bash

#
# Cyan AutoRelease Build Script, installs the cyan update on the remote server.
# Only works on AutoRelease enabled repository services.
#
# Also, it needs the autorelease.allow.install file on the server.
#

function prepare() {
	destination "/etc/connective-http/cyan-releases"
	buildOutput "build/Wrapper/serverdata/"
}

function build() {
    gameVersions=( "1.16.5" "1.17" )
    
    chmod +x gradlew buildlocal.sh
    ./gradlew -c settings.lite.gradle installLiteLibs
    
    for ver in "${gameVersions[@]}"; do
        ./gradlew -PcurrentXML -PnoServer=true -PnoClient=true -PuncheckedClient=true -PaddChangeLog="$(cat change.log)" -PoverrideGameVersion="$ver" processResources
        ./gradlew -PcurrentXML -PnoServer=true -PnoClient=true -PuncheckedClient=true -PaddChangeLog="$(cat change.log)" -PoverrideGameVersion="$ver" processResources
        ./gradlew -PcurrentXML -PnoServer=true -PnoClient=true -PuncheckedClient=true -PaddChangeLog="$(cat change.log)" -PoverrideGameVersion="$ver" serverDownloads
    done
}

function install() {
	cp -rfv "$BUILDDIR/." "$DEST"
}
