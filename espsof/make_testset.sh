#!/bin/sh
cd $(dirname $0)

sh ../morphemesByType/get_all.sh
mv ../morphemesByType/all.txt .
cat espsof.txt standalone.txt | awk 'NF == 1 {print $1 "\t" $1} NF != 1 {print}' | ./filter.py > testset_espsof.txt
rm all.txt
