#!/bin/bash
eval "$(head config.gradle -n12 | tail -n+3 | sed "s/ext\.//g")"

read -rp "Game version: " gameversion
read -rp "Fabric version: " version

echo "Adding support for $gameversion Fabric $version..."
majorversion="$gameversion"
if [[ "$majorversion" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    majorversion="${majorversion%.*}"
fi

echo Updating modloaderbuild...
buildfile=
if [ -f "modloaderbuild-$gameversion.gradle" ]; then
    buildfile="$(cat "modloaderbuild-$gameversion.gradle")"
elif [ -f "modloaderbuild-$majorversion.gradle" ]; then
    buildfile="$(cat "modloaderbuild-$majorversion.gradle")"
fi

if [ "$buildfile" == "" ]; then
    buildfile="stability 'testing'

paper([
])

forge(
)
"
fi

if [[ "$buildfile" =~ $'fabric (\n' ]] || [[ "$buildfile" =~ $'fabric(\n' ]]; then
    buildfile="$(echo "$buildfile" | sed -E "s/fabric ?\(/&\n    '$version',/g")"
else
    buildfile="${buildfile}

fabric(
    '$version'
)"
fi

if [ -f "modloaderbuild-$gameversion.gradle" ]; then
    echo "$buildfile" > "modloaderbuild-$gameversion.gradle"
elif [ -f "modloaderbuild-$majorversion.gradle" ]; then
    echo "$buildfile" > "modloaderbuild-$majorversion.gradle"
else
    echo "$buildfile" > "modloaderbuild-$gameversion.gradle"
fi

echo Updating workflowbuilddata...
buildfile=
if [ -f "workflowbuilddata-$gameversion.bash" ]; then
    source "workflowbuilddata-$gameversion.bash"
    buildfile="$(cat "workflowbuilddata-$gameversion.bash" | sed -E "s/(fabric=)(.+)/\1$version/g")"
else
    buildfile='forge=
fabric=$version
paper=

mappings_paper=
'
fi
echo "$buildfile" > "workflowbuilddata-$gameversion.bash"

if [ -d "modkits/modkit-$gameversion-$cyanversion" ]; then
    echo Updating modkit...
    modkit="$(cat "modkits/modkit-$gameversion-$cyanversion/build.gradle")"
    oldblock="$(echo "$modkit" | sed -nE "/INTERMEDIARY ?\{/,/\}/{p}")"
    newblock="$(echo "$oldblock" | sed -E "s/(modloader \")(.*)(\")/\\1$version\\3/g")"
    modkit="${modkit//$oldblock/$newblock}"
    echo "$modkit" > "modkits/modkit-$gameversion-$cyanversion/build.gradle"
fi

if [ -d "coremodkits/coremodkit-$gameversion-$cyanversion" ]; then
    echo Updating coremodkit...
    modkit="$(cat "coremodkits/coremodkit-$gameversion-$cyanversion/build.gradle")"
    oldblock="$(echo "$modkit" | sed -nE "/INTERMEDIARY ?\{/,/\}/{p}")"
    newblock="$(echo "$oldblock" | sed -E "s/(modloader \")(.*)(\")/\\1$version\\3/g")"
    modkit="${modkit//$oldblock/$newblock}"
    echo "$modkit" > "coremodkits/coremodkit-$gameversion-$cyanversion/build.gradle"
fi

echo
echo Finsihed adding the fabric version.
echo The readme file needs to be manually updated.
echo
