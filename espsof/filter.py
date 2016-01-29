#!/usr/bin/python3

import sys

morphemes = set([line.strip() for line in open('all.txt').readlines()])
for line in sys.stdin:
    line = line.strip()
    dissection = line.split()[1].split("'")
    if False not in [d in morphemes for d in dissection]:
        print(line)
