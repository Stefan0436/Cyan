#!/bin/bash
source ./workflowbuilddata-$1.bash
cmd="--version \"$1\""
if [ "$2" != "" ]; then
    cmd+=" --modloader \"$2\""
    if [ "$2" == "forge" ]; then
        cmd+=" --modloader-version \"$forge\""
    elif [ "$2" == "fabric" ]; then
        cmd+=" --modloader-version \"$fabric\""
    elif [ "$2" == "paper" ]; then
        cmd+=" --modloader-version \"$paper\" --mappings-version \"$mappings_paper\""
    fi
fi

eval "./buildlocal.sh $cmd -- --stacktrace"
