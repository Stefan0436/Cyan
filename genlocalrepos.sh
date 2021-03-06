#!/bin/bash

#
# Written by Martine Eekhof
#

index=0
echo "repositories> {"
echo "    central> 'https://repo1.maven.org/maven2'" 

while read -r line; do
    index=$((index+1))
    echo "    local$index> 'file://$(realpath "$line")'"
done < <(find build/ -type d -name maven)
echo "    asf> 'https://aerialworks.ddns.net/maven'"

echo "}"
