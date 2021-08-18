#!/bin/bash
eval "$(head config.gradle -n12 | tail -n+3 | sed "s/ext\.//g")"

function greaterOrEqualToVersion() {
    if [ "$1" == "$2" ]; then return 0;
    elif [ $(echo -e "$1\n$2" | sort -V -r | head -n 1) == "$2" ]; then return 0;
    else return 1; fi
}

if [ "$1" != "" ]; then
    minecraft=$1
fi

if greaterOrEqualToVersion 1.17 "$minecraft" && [ "$1" != "spigotonly" ]; then
    paper="$(curl -s "https://papermc.io/api/v2/projects/paper/versions/$minecraft/" | jq '.builds[-1]')"
    commit="$(curl -s "https://papermc.io/api/v2/projects/paper/versions/$minecraft/builds/$paper" | jq '.changes[0].commit' -r)"
    
    build="$paper"
    while [ "$commit" == "null" ]; do
        1>&2 echo "WARNING! Build $paper has no commit hash, switching to $((paper - 1))!"
        1>&2 echo
        paper=$((paper - 1))
        commit="$(curl -s "https://papermc.io/api/v2/projects/paper/versions/$minecraft/builds/$paper" | jq '.changes[0].commit' -r)"
    done
    
    echo "version \"$commit:PB_$build\""
    echo "modloader \"$build\""
else
    refs="$(curl "https://hub.spigotmc.org/versions/$minecraft.json" -s | jq .refs)"
    craftbukkit="$(curl "https://hub.spigotmc.org/stash/projects/SPIGOT/repos/craftbukkit/raw/pom.xml?at=$(echo "$refs" | jq -r .CraftBukkit)" -s)"

    build="$(echo "$craftbukkit" | xq '.project.properties.minecraft_version' -r)"
    commit="$(echo "$refs" | jq -r .BuildData)"

    paper="$(curl -s "https://papermc.io/api/v2/projects/paper/versions/$minecraft/" | jq '.builds[-1]')"

    echo "version \"$commit:$build\""
    echo "modloader \"$paper\""
fi
