#!/bin/bash
eval "$(head config.gradle -n12 | tail -n+3 | sed "s/ext\.//g")"

refs="$(curl "https://hub.spigotmc.org/versions/$minecraft.json" -s | jq .refs)"
craftbukkit="$(curl "https://hub.spigotmc.org/stash/projects/SPIGOT/repos/craftbukkit/raw/pom.xml?at=$(echo "$refs" | jq -r .CraftBukkit)" -s)"

build="$(echo "$craftbukkit" | xq '.project.properties.minecraft_version' -r)"
commit="$(echo "$refs" | jq -r .BuildData)"

paper="$(curl -s "https://papermc.io/api/v2/projects/paper/versions/$minecraft/" | jq '.builds[-1]')"

echo "version \"$commit:$build\""
echo "modloader \"$paper\""
