#!/bin/bash
if [ ! -d "build" ]; then echo Please build first.; fi
if [ -d Cornflower/build ]; then rm -rf Cornflower/build; fi
ln -s "$(realpath build/Cornflower)" "$(realpath Cornflower/build)"
echo Done.
