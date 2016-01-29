#!/bin/sh
cd $(dirname $0)
mkdir -p ../bin
scalac -d ../bin WordSegmenter.scala
