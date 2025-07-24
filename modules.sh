#!/bin/zsh

for J in `find ./scenario -name '*.jar'`; do
cp $J modules/
#ls -d $J
done

