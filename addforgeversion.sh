#!/bin/bash
eval "$(head config.gradle -n12 | tail -n+3 | sed "s/ext\.//g")"

read -rp "Game version: " gameversion
read -rp "Forge version: " version

echo "Finding MCP version..."
forgeuniversaltemplate="https://maven.minecraftforge.net/net/minecraftforge/forge/%game%-%forgeversion%/forge-%game%-%forgeversion%-universal.jar"
if [[ "$gameversion" =~ ^1\.1?[0-6]+(\.[0-9]+)?$ ]]; then
    manifestMF="$(curl -L "${forgeurltemplate//%game%-%forgeversion%/$gameversion-$version}" -s --output - | bsdtar -O -xvf - maven/net/minecraftforge/forge/$gameversion-$version/forge-$gameversion-$version.jar 2>/dev/null | bsdtar -O -xvf - META-INF/MANIFEST.MF 2>/dev/null)"
    mappings=$(echo "$manifestMF" | grep --after-context=4 'Implementation-Title: MCP' | grep 'Implementation-Version: ' | sed "s/Implementation-Version: //g" | sed "s/\r//g")
else
    manifestMF="$(curl -L "${forgeuniversaltemplate//%game%-%forgeversion%/$gameversion-$version}" -s --output - | bsdtar -O -xvf - META-INF/MANIFEST.MF 2>/dev/null)"
    mappings=$(echo "$manifestMF" | grep --after-context=4 'Implementation-Title: MCP' | grep 'Implementation-Version: ' | sed "s/Implementation-Version: //g" | sed "s/\r//g")
fi
echo MCP: "$mappings"

echo "Adding support for $gameversion Forge $version..."
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

paper([
])
"
fi

if [[ "$buildfile" =~ $'forge (\n' ]] || [[ "$buildfile" =~ $'forge(\n' ]]; then
    buildfile="$(echo "$buildfile" | sed -E "s/forge ?\(/&\n    '$version',/g")"
else
    buildfile="${buildfile}

forge(
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
    buildfile="$(cat "workflowbuilddata-$gameversion.bash" | sed -E "s/(forge=)(.+)/\1$version/g")"
else
    buildfile='forge=$version
fabric=
paper=

mappings_paper=
'
fi
echo "$buildfile" > "workflowbuilddata-$gameversion.bash"

if [ -d "modkits/modkit-$gameversion-$cyanversion" ]; then
    echo Updating modkit...
    modkit="$(cat "modkits/modkit-$gameversion-$cyanversion/build.gradle")"
    oldblock="$(echo "$modkit" | sed -nE "/MCP ?\{/,/\}/{p}")"
    newblock="$(echo "$oldblock" | sed -E "s/(modloader \")(.*)(\")/\\1$version\\3/g" | sed -E "s/(version \")(.*)(\")/\\1$mappings\\3/g")"
    modkit="${modkit//$oldblock/$newblock}"
    echo "$modkit" > "modkits/modkit-$gameversion-$cyanversion/build.gradle"
fi

if [ -d "coremodkits/coremodkit-$gameversion-$cyanversion" ]; then
    echo Updating coremodkit...
    modkit="$(cat "coremodkits/coremodkit-$gameversion-$cyanversion/build.gradle")"
    oldblock="$(echo "$modkit" | sed -nE "/MCP ?\{/,/\}/{p}")"
    newblock="$(echo "$oldblock" | sed -E "s/(modloader \")(.*)(\")/\\1$version\\3/g" | sed -E "s/(version \")(.*)(\")/\\1$mappings\\3/g")"
    modkit="${modkit//$oldblock/$newblock}"
    echo "$modkit" > "coremodkits/coremodkit-$gameversion-$cyanversion/build.gradle"
fi

echo
echo Finsihed adding the forge version.
echo The readme file needs to be manually updated.
echo
