#!/bin/bash

#
# Written by Martine Eekhof
#

index=0
echo "repositories> {"
echo "    asf> 'https://aerialworks.ddns.net/maven'"
echo "    central> 'https://repo1.maven.org/maven2'" 

while read -r line; do
    index=$((index+1))
    echo "    local$index> 'file://$(realpath "$line")'"
done < <(find . -type d -name maven)

echo "}"
