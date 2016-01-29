#!/bin/sh

cd $(dirname $0)

task="nothing"

if [ $# -eq 1 ]
then
    task=$1
fi

if [ $task != "-r" ]
then
    echo 'Creating tagged sets'
    ./tag.py | ./split_sets.py
    cat train.txt | awk '{print $1 "\t" $2}' | uniq > expect.train.txt
    cat test.txt | awk '{print $1 "\t" $2}' | uniq > expect.test.txt
    cat expect.train.txt expect.test.txt > expect.all.txt
fi

if [ $task != "-f" ]
then
    echo 'Running WordSegmenter'
    scala -cp ../bin/ WordSegmenter.WordSegmenter train.txt ../morphemesByType/sets/ -n < expect.train.txt > out.train.txt
    scala -cp ../bin/ WordSegmenter.WordSegmenter train.txt ../morphemesByType/sets/ -n < expect.test.txt > out.test.txt
    scala -cp ../bin/ WordSegmenter.WordSegmenter train.txt ../morphemesByType/sets/ -m < expect.all.txt > out.maxmatch.txt
    scala -cp ../bin/ WordSegmenter.WordSegmenter train.txt ../morphemesByType/sets/ -r < expect.all.txt > out.random.txt
    scala -cp ../bin/ WordSegmenter.WordSegmenter train.txt ../morphemesByType/sets/ -mn < expect.all.txt > out.norules.maxmatch.txt
    scala -cp ../bin/ WordSegmenter.WordSegmenter train.txt ../morphemesByType/sets/ -rn < expect.all.txt > out.norules.random.txt
    scala -cp ../bin/ WordSegmenter.WordSegmenter train.txt ../morphemesByType/sets/ -bn < expect.train.txt > out.bigram.train.txt
    scala -cp ../bin/ WordSegmenter.WordSegmenter train.txt ../morphemesByType/sets/ -bn < expect.test.txt > out.bigram.test.txt
    scala -cp ../bin/ WordSegmenter.WordSegmenter train.txt ../morphemesByType/sets/ -tn < expect.train.txt > out.trigram.train.txt
    scala -cp ../bin/ WordSegmenter.WordSegmenter train.txt ../morphemesByType/sets/ -tn < expect.test.txt > out.trigram.test.txt
    scala -cp ../bin/ WordSegmenter.WordSegmenter train.txt ../morphemesByType/sets/ < expect.train.txt > out.yesrules.train.txt
    scala -cp ../bin/ WordSegmenter.WordSegmenter train.txt ../morphemesByType/sets/ < expect.test.txt > out.yesrules.test.txt
    scala -cp ../bin/ WordSegmenter.WordSegmenter train.txt ../morphemesByType/sets/ -b < expect.train.txt > out.yesrules.bigram.train.txt
    scala -cp ../bin/ WordSegmenter.WordSegmenter train.txt ../morphemesByType/sets/ -b < expect.test.txt > out.yesrules.bigram.test.txt
    scala -cp ../bin/ WordSegmenter.WordSegmenter train.txt ../morphemesByType/sets/ -t < expect.train.txt > out.yesrules.trigram.train.txt
    scala -cp ../bin/ WordSegmenter.WordSegmenter train.txt ../morphemesByType/sets/ -t < expect.test.txt > out.yesrules.trigram.test.txt
fi
