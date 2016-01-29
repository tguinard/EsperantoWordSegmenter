#!/usr/bin/python3

import sys

def add_to_pos(root, pos):
    if pos in noun_suffix:
        nouns.add(root)
    if pos in verb_suffix:
        verbs.add(root)
    if pos in adj_suffix:
        adjs.add(root)
    if pos in adv_suffix:
        advs.add(root)


def xify(root):
    hatmap = {
        'ĉ': 'cx',
        'ĝ': 'gx',
        'ĥ': 'hx',
        'ĵ': 'jx',
        'ŝ': 'sx',
        'ŭ': 'ux',
        'Ĉ': 'cx',
        'Ĝ': 'gx',
        'Ĥ': 'hx',
        'Ĵ': 'jx',
        'Ŝ': 'sx',
        'Ŭ': 'ux',

    }
    for k,v in hatmap.items():
        root = root.replace(k, v)
    return root.lower()

def filewrite(filename, roots):
    outfile = open(filename, 'w')
    for root in sorted(list(roots)):
        print(root, file=outfile)
    outfile.close()


noun_suffix = {'o', 'oj', 'on', 'ojn'}
verb_suffix = {'i', 'is', 'as', 'os', 'us', 'u'}
adj_suffix = {'a', 'an', 'aj', 'ajn'}
adv_suffix = {'e', 'en'}

nouns = set()
verbs = set()
adjs = set()
advs = set()
humans = set()

not_normal = set()
try:
    not_normal = set([line.strip() for line in open('not_normal.txt').readlines()])
except IOError:
    print("Failed: Run ./get_not_normal.sh first")
    sys.exit(1)

root = None
pos = None
for line in open('vortaro.xml'):
    line = line.strip()
    if line.startswith('<rdk'):
        root = xify(line.split(" v='")[1].split("'")[0])
        pos = None
        if " a='" in line and root not in not_normal:
            pos = line.split(" a='")[1].split("'")[0]
            add_to_pos(root, pos)
    elif line.startswith('<drv') and root is not None:
        onesuffix = line.count('=') == 1 and " suf='" in line
        grmr = line.count('=') == 2 and " suf='" in line and " grm='" in line
        if onesuffix or grmr:
            suf = line.split(" suf='")[1].split("'")[0]
            if pos is None:
                add_to_pos(root, suf)
            if suf == 'ino':
                humans.add(root)


humans = humans & nouns
nouns = nouns - humans

filewrite('../generated/noun.txt', nouns)
filewrite('../generated/verb.txt', verbs)
filewrite('../generated/adj.txt', adjs)
filewrite('../generated/adv.txt', advs)
filewrite('../generated/nounHuman.txt', humans)
