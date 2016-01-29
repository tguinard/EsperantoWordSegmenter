#!/usr/bin/python3
"""
Break tagged set into training and test set
Reads tagged set from stdin
"""

import sys
import random

train_percent = 0.75

words = []
for line in sys.stdin:
    line = line.strip()
    word = line.split()[0]
    morphemes = line.split()[1].split("'")
    while len(words) <= len(morphemes):
        words.append({})
    words[len(morphemes)].setdefault(word, [])
    words[len(morphemes)][word].append(line)

keys = [list(w.keys()) for w in words]
for keyset in keys:
    random.shuffle(keyset)

trainfile = open("train.txt", "w")
testfile = open("test.txt", "w")

for i, keyset in enumerate(keys):
    cutoff = int(train_percent * len(keyset))
    for key in keyset[:cutoff]:
        for line in words[i][key]:
            print(line, file=trainfile)
    for key in keyset[cutoff:]:
        for line in words[i][key]:
            print(line, file=testfile)
trainfile.close()
testfile.close()
