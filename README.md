Automatically segments Esperanto words into their component morphemes

###Dependencies

Only Linux is guaranteed to be supported
scala (2.11 definitely works)
python3

###src/WordSegmenter.scala

Algorithm to segment words. Uses two basic steps:
1. Find all possible segmentations via trie traversal, apply rules unless otherwise specified
2. Find best segmentation using a Markov model or maximal match algorithm. 

Usage
    scala WordSegmenter.WordSegmenter trainingFile morphemesByTypeDirectory [-m|r|n|b|t]
See experiments/run\_tests.sh for example usage

Options
    Default: apply rules, use unigram Markov model
    -m: Use maximal morpheme matching instead of Markov model
    -r: Skip disambiguation (step 2)
    -n: Apply no rules in step 1
    -b: Use bigram Markov model
    -t: Use trigram Markov model
Concatenate options if using more than one. e.g. use "-mn", not "-m -n"
Note: unigram, bigram, trigram Markov models refer to 2-gram sequence, 3-gram sequence, 4-gram sequence respectively, as in https://en.wikipedia.org/wiki/N-gram#Examples

Build
run:
    src/build.sh

###morphemesByType/

Defines and classifies all valid morphemes.

Non content morphemes are predefined, following the Akademia Vortaro (http://www.akademio-de-esperanto.org/akademia\_vortaro/), with manual classification

morphemesByType/normal/generated is built using morphemesByType/normal/build/classify.py
Uses the dictionary "vortaro.xml" from Esperantilo: http://www.xdobry.de/esperantoedit/index\_en.html

To regenerate normal roots run:
    morphemesByType/normal/build/get\_not\_normal.sh
    morphemesByType/normal/build/classify.py

To remake morphemesByType/sets directory (what WordSegmenter.scala uses), run:
    morphemesByType/make\_sets.sh

###espsof/

Test set from ESPSOF. All presegmented words from the original source are in espsof.txt. testset\_espsof.txt contains all words with known morphemes (also occur in Esperantilo).
To remake test set (e.g. if the set of known morphemes changes), run:
    espsof/make\_testset.sh

###experiments/

Create a tagged training set and test set from the ESPSOF test set, and run WordSegmenter.scala. Analyze the segmentation accuracy.

To create tagged training/test sets, run:
    experiments/run\_tests.sh -f

To run WordSegmenter.scala with predefined options, run:
    experiments/run\_tests.sh -r

To create tagged sets and run WordSegmenter.scala, run:
    experiments/run\_tests.sh

To analyze the segmentation accuracy, run:
    experiments/analyze.py expectFile resultsFile [-r]
Use -r if and only if -r was used when running WordSegmenter.scala
