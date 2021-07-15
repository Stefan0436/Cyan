#!/bin/bash
if [ "$(git status --porcelain)" == "" ]; then
    1>&2 echo No changes detected, cannot create soft-patch.
    1>&2 echo Soft-patching requires uncommitted changes, please reset if you have already committed your work.
    exit 1
fi
echo Creating soft-patch...
chmod +x gradlew

echo Preparing LiteLibs...
./gradlew -c settings.lite.gradle installLiteLibs || exit 1

echo Creating patch directory...
./gradlew updateServerData || exit 1

echo Soft-patch created in build/Wrapper/softpatch.
