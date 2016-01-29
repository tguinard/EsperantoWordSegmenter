#!/bin/sh
cd $(dirname $0)

cat sets/*.txt | sort | uniq > all.txt
