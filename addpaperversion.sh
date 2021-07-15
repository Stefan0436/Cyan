#!/bin/bash
eval "$(head config.gradle -n12 | tail -n+3 | sed "s/ext\.//g")"

read -rp "Game version: " gameversion
read -rp "Paper version: " version
read -rp "Mappings version: " mappings

echo "Adding support for $gameversion Paper $version..."
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

fabric(
)

forge(
)
"
fi

if [[ "$buildfile" =~ $'paper ([\n' ]] || [[ "$buildfile" =~ $'paper([\n' ]]; then
    buildfile="$(echo "$buildfile" | sed -E "s/paper ?\(\[/&\n    '$version': '$mappings',/g")"
else
    buildfile="${buildfile}

paper([
    '$version': '$mappings'
])"
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
    buildfile="$(cat "workflowbuilddata-$gameversion.bash" | sed -E "s/(paper=)([0-9]+)/\1$version/g" | sed -E "s/(mappings_paper=)(.*)/\1$mappings/g")"
else
    buildfile='forge=
fabric=
paper=$version

mappings_paper=$mappings
'
fi
echo "$buildfile" > "workflowbuilddata-$gameversion.bash"

if [ -d "modkits/modkit-$gameversion-$cyanversion" ]; then
    echo Updating modkit...
    modkit="$(cat "modkits/modkit-$gameversion-$cyanversion/build.gradle")"
    oldblock="$(echo "$modkit" | sed -nE "/SPIGOT ?\{/,/\}/{p}")"
    newblock="$(echo "$oldblock" | sed -E "s/(modloader \")(.*)(\")/\\1$version\\3/g" | sed -E "s/(version \")(.*)(\")/\\1$mappings\\3/g")"
    modkit="${modkit//$oldblock/$newblock}"
    echo "$modkit" > "modkits/modkit-$gameversion-$cyanversion/build.gradle"
fi

if [ -d "coremodkits/coremodkit-$gameversion-$cyanversion" ]; then
    echo Updating coremodkit...
    modkit="$(cat "coremodkits/coremodkit-$gameversion-$cyanversion/build.gradle")"
    oldblock="$(echo "$modkit" | sed -nE "/SPIGOT ?\{/,/\}/{p}")"
    newblock="$(echo "$oldblock" | sed -E "s/(modloader \")(.*)(\")/\\1$version\\3/g" | sed -E "s/(version \")(.*)(\")/\\1$mappings\\3/g")"
    modkit="${modkit//$oldblock/$newblock}"
    echo "$modkit" > "coremodkits/coremodkit-$gameversion-$cyanversion/build.gradle"
fi

if [ ! -f "MTK/src/main/resources/mappings/inconsistencies/inconsistencies-paper-fallback-$gameversion.ccfg" ] && [[ ! "$gameversion" =~ 1\.1?[0-6]+(\.[0-9]+)? ]]; then
   while [ ! -f "MTK/src/main/resources/mappings/inconsistencies/inconsistencies-paper-fallback-$gameversion.ccfg" ]; do
        echo
        echo WARNING!
        echo Missing fallback inconsistency mappings!
        echo
        echo This is only a problem for versions above 1.17, please create the following file:
        echo "MTK/src/main/resources/mappings/inconsistencies/inconsistencies-paper-fallback-$gameversion.ccfg"
        echo
        echo You can use one of the existing fallback mappings as template, however, copying everything is not allowed.
        echo
        read -p "Press enter to try again..."
   done
elif [ ! -f "MTK/src/main/resources/mappings/inconsistencies/inconsistencies-paper-$gameversion-$version.ccfg" ] && [[ ! "$gameversion" =~ 1\.1?[0-6]+(\.[0-9]+)? ]]; then
    echo Creating inconsistency mappings...
    echo
    read -rp "Your name: " name
    read -rp "License for mappings: " license
    echo "#
# Cyan Inconsistency Mappings, for Paper $gameversion compatibility.
# Written by hand, Copyright(c) $(date +%Y) $name, $license.
#

mappingsVersion> '$mappings'
$(cat "MTK/src/main/resources/mappings/inconsistencies/inconsistencies-paper-fallback-$gameversion.ccfg" | sed -E "0,/^[^#]/d")
" > "MTK/src/main/resources/mappings/inconsistencies/inconsistencies-paper-$gameversion-$version.ccfg"
    
    if command -v nano &>/dev/null; then
        nano "MTK/src/main/resources/mappings/inconsistencies/inconsistencies-paper-$gameversion-$version.ccfg"
    elif command -v vi &>/dev/null; then
        vi "MTK/src/main/resources/mappings/inconsistencies/inconsistencies-paper-$gameversion-$version.ccfg"
    elif command -v xdg-open &>/dev/null; then
        xdg-open "$(realpath "MTK/src/main/resources/mappings/inconsistencies/inconsistencies-paper-$gameversion-$version.ccfg")"
    else
        echo "Cannot open any editor to open mappings file, please edit the following file:"
        echo "MTK/src/main/resources/mappings/inconsistencies/inconsistencies-paper-$gameversion-$version.ccfg"
    fi
fi

echo
echo Finsihed adding the paper version.
echo The readme file needs to be manually updated.
echo
