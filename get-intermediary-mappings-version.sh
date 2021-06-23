#!/bin/bash
eval "$(head config.gradle -n12 | tail -n+3 | sed "s/ext\.//g")"
echo "version \"$minecraft\""
echo "modloader \"$(curl "https://meta.fabricmc.net/v2/versions/loader/$minecraft" -s --output - | jq ".[0].loader.version" -r)\""
