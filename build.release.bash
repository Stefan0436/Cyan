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
    chmod +x gradlew buildlocal.sh
    ./buildlocal.sh
    ./gradlew serverDownloads
}

function install() {
	cp -rfv "$BUILDDIR/." "$DEST"
}
