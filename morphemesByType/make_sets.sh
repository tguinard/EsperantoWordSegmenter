#!/bin/sh
cd $(dirname $0)

mkdir -p sets
cp endings/*/*.txt sets
cp standalone/*.txt sets
cp normal/affix/*.txt sets
cp normal/generated/*.txt sets
