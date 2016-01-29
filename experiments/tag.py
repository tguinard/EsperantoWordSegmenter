#!/usr/bin/python3
"""
Ad-hoc part of speech tagging
Needed for training set in order to define Markov model
If not sure about POS tag, print all possibilities with equal probability
"""

import os
from itertools import product

endEnd = {'nounEnding', 'verbEnding', 'adjEnding', 'advEnding'}
suffixes = {'adjSuffix', 'nounHumanSuffix', 'nounSuffix', 'numberSuffix', 'tenseSuffix', 'verbSuffix'}
midEnd = {'o', 'midEnding'}
standalone = {'adverb', 'article', 'conjunction', 'expression', 'number', 'preposition', 'pronoun', 'table'}
normal = {'adj', 'adv', 'nounHuman', 'noun', 'verb'}

# Distinguish between endEnd and midEnd
def positional(isEnding, isSecondLast, morphTypeSet):
    if isSecondLast or (isEnding and len(morphTypeSet) > 1):
        morphTypeSet = morphTypeSet - midEnd
    if (not isEnding) and len(morphTypeSet) > 1:
        morphTypeSet = morphTypeSet - endEnd
    return morphTypeSet

# Match part of speech with word ending/suffix if possible
def directPartOfSpeech(morphTypeSet, nextTypeSet):
    newTypeSet = set()
    if len(nextTypeSet & (endEnd | suffixes)) == 1:
        searchKey = list(nextTypeSet & (endEnd | suffixes))[0].split("Ending")[0].split('Suffix')[0]
        for mtype in morphTypeSet:
            if searchKey in mtype:
                newTypeSet.add(mtype)
    if 'tenseSuffix' in nextTypeSet and 'verb' in morphTypeSet:
        newTypeSet = {'verb'}
    if len(newTypeSet) > 0:
        return newTypeSet
    return morphTypeSet

# Get rid of standalone types if possible
# Use when word has more than one morpheme
def noStandalone(morphTypeSet):
    newTypeSet = morphTypeSet - standalone
    if len(newTypeSet) > 0:
        return newTypeSet
    return morphTypeSet

morphDir = "../morphemesByType/sets/"
files = os.listdir(morphDir)
morphTypes = {}
for filename in files:
    mtype = filename.split(".txt")[0]
    members = [line.strip() for line in open(morphDir + filename).readlines()]
    for morpheme in members:
        morphTypes.setdefault(morpheme, set())
        morphTypes[morpheme].add(mtype)

cases = [line.strip() for line in open("../espsof/testset_espsof.txt").readlines()]
for line in cases:
    morphemes = line.split()[1].split("'")
    nextTypeSet = set()
    typeSets = []
    for i, morpheme in list(enumerate(morphemes))[::-1]:
        isLast = i == len(morphemes) - 1
        morphTypeSet = morphTypes[morpheme]
        morphTypeSet = directPartOfSpeech(morphTypeSet, nextTypeSet)

        #must be standalone if only one morpheme
        if len(morphemes) == 1:
            morphTypeSet = morphTypeSet & standalone

        #en is in a lot of classes
        if isLast and morpheme == 'en':
            morphTypeSet = {'advEnding'}
        if i != 0 and morpheme == 'en' and (morphemes[i-1] in {'supr', 'flank', 'dekstr', 'hejm'}):
            morphTypeSet = {'midEnding'}

        if len(morphemes) > 1:
            morphTypeSet = noStandalone(morphTypeSet)
        
        nextTypeSet = morphTypeSet
        isSecondLast = i == len(morphemes) - 2
        morphTypeSet = positional(isLast, isSecondLast, morphTypeSet)
        if morpheme == 'en' and 'prepPrefix' in morphTypeSet:
            morphTypeSet = {'prepPrefix'}
        
        typeSets.append(morphTypeSet)
    typeSets = typeSets[::-1]
    prod = list(product(*typeSets))
    for combo in prod:
        print(line, "'".join(combo), 1/len(prod), sep="\t")
