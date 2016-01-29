#!/usr/bin/python3
"""
Try combining some classes, see if classification can be simplified
"""

import sys

def mapOutput(mymap):
    for line in sys.stdin:
        linefields = line.split()
        fields = linefields[2].split("'")
        newFields = [mymap.get(field, field) for field in fields]
        linefields[2] = "'".join(newFields)
        print("\t".join(linefields))

humanMap = {
    'nounHuman': 'noun',
    'nounHumanPrefix': 'nounPrefix',
    'nounHumanSuffix': 'nounSuffix',
}

standaloneMap = {
    'adverb': 'conjunction',
    'conjunction': 'conjunction',
    'expression': 'conjunction',
    'preposition': 'conjunction',
}

posMap = {
    'adj': 'noun',
    'adv': 'noun',
    'nounHuman': 'noun',
    'verb': 'noun',
    'adjSuffix': 'nounSuffix',
    'nounSuffix': 'nounSuffix',
    'numberSuffix': 'nounSuffix',
    'tenseSuffix': 'nounSuffix',
    'verbSuffix': 'nounSuffix',
    'nounHumanSuffix': 'nounSuffix',
    'nounHumanPrefix': 'nounPrefix',
    'nounPrefix': 'nounPrefix',
    'prepPrefix': 'nounPrefix',
    'verbPrefix': 'nounPrefix',
    'adjEnding': 'nounEnding',
    'advEnding': 'nounEnding',
    'nounEnding': 'nounEnding',
    'verbEnding': 'nounEnding',
}

endPosMap = {
    'adjEnding': 'nounEnding',
    'advEnding': 'nounEnding',
    'nounEnding': 'nounEnding',
    'verbEnding': 'nounEnding',
}

superEndPosMap = {
    'adjEnding': 'nounEnding',
    'advEnding': 'nounEnding',
    'nounEnding': 'nounEnding',
    'verbEnding': 'nounEnding',
    'midEnding': 'o',
    'o': 'o',
}

midEndPosMap = {
    'midEnding': 'o',
    'o': 'o',
}

arguments = {
    '-h': humanMap,
    '-s': standaloneMap,
    '-p': posMap,
    '-e': endPosMap,
    '-u': superEndPosMap,
    '-m': midEndPosMap,
}
argumentNames = {
    '-h': 'humanMap',
    '-s': 'standaloneMap',
    '-p': 'posMap',
    '-e': 'endPosMap',
    '-u': 'superEndPosMap',
    '-m': 'midEndPosMap',
}

try:
    mapOutput(arguments[sys.argv[1]])
except (IndexError, KeyError):
    print('Usage: %s opt' % sys.argv[0])
    print("Options:")
    for k,v in argumentNames.items():
        print("\t%s: %s" % (k, v))
