#!/bin/sh
cd $(dirname $0)

find ../../endings | grep txt$ | xargs cat > end.tmp
find ../../standalone | grep txt$ | xargs cat > stand.tmp
find ../affix | grep txt$ | xargs cat > affix.tmp
cat end.tmp stand.tmp affix.tmp | sort | uniq > not_normal.txt
rm *.tmp
