import scala.collection.mutable
import scala.io.Source
import java.io.File
import scala.math.Ordering.Implicits._

package WordSegmenter {

  /**
   * First pass morpheme types
   * For optional rule-based step
   */
  object RuleMorphemeType extends Enumeration {
    type RuleMorphemeType = Value
    val Standalone, Pronoun, Normal, WordEnd, Table, Article, TablePronounEnding = Value
    var ignoreRules = false

    def isValidEnding(value: RuleMorphemeType.Value): Boolean = {
      ignoreRules || (value != null && value != Normal)
    }

    def agreesWithPrevious(value: RuleMorphemeType.Value, prev: RuleMorphemeType.Value): Boolean = value match {
      case TablePronounEnding =>
        ignoreRules || (prev == Table || prev == Pronoun)
      case Article =>
        ignoreRules || prev == null
      case _ =>
        ignoreRules || prev != Article
    }

    def pleaseIgnoreRules() = {
      ignoreRules = true
    }
  }

  /**
   * Second pass morpheme types
   * For n-gram Markov disambiguation
   */
  object MarkovMorphemeType extends Enumeration {
    type MarkovMorphemeType = Value
    val AdjEnding, Adj, AdjSuffix, AdvEnding, Adverb, Adv, Article, Conjunction, Expression, MidEnding,
    NounEnding, NounHumanPrefix, NounHuman, NounHumanSuffix, NounPrefix, Noun, NounSuffix, Number, NumberSuffix,
    O, Preposition, PrepPrefix, Pronoun, TablePronounEnding, Table, TenseSuffix, VerbEnding, VerbPrefix, Verb,
    VerbSuffix, Start, End = Value

    val wordCounts: mutable.Map[MarkovMorphemeType.Value, Int] = mutable.Map[MarkovMorphemeType.Value, Int]()
    var total = 0

    def setWordCount(key: MarkovMorphemeType.Value, value: Int) = {
      wordCounts(key) = value
      total = 0
    }

    def totalMorphemes(): Int = {
      if (total == 0) {
        total = wordCounts.values.sum
      }
      total
    }

    def typeCount(value: MarkovMorphemeType.Value): Int = {
      if (wordCounts.contains(value)) {
        wordCounts(value)
      }
      else {
        totalMorphemes()
      }
    }

    def toRuleMorphemeType(value: MarkovMorphemeType.Value): RuleMorphemeType.Value = value match {
      case AdjEnding | AdvEnding | NounEnding | VerbEnding |
           MidEnding | O =>
        RuleMorphemeType.WordEnd

      case TablePronounEnding =>
        RuleMorphemeType.TablePronounEnding

      case Pronoun =>
        RuleMorphemeType.Pronoun

      case Article =>
        RuleMorphemeType.Article

      case Adj | Adv | NounHuman | Noun | Verb |
           AdjSuffix | NounHumanSuffix | NounSuffix | NumberSuffix | TenseSuffix | VerbSuffix |
           NounHumanPrefix | NounPrefix | PrepPrefix | VerbPrefix =>
        RuleMorphemeType.Normal

      case Adverb | Conjunction | Expression | Number | Preposition =>
        RuleMorphemeType.Standalone

      case Table =>
        RuleMorphemeType.Table

      case Start | End =>
        null
    }
  }

  /**
   * n-gram Markov model
   */
  class MarkovModel(filename: String, trieRoot: TrieNode, n: Int = 1) {
    val modelOrder = n
    val transitions = makeTransitions(filename)

    def makeTransitions(filename: String): Map[List[MarkovMorphemeType.Value], Map[MarkovMorphemeType.Value, Double]] = {
      val multiplier = 0.00001

      val individualTransitions = (for (line <- Source.fromFile(filename).getLines()) yield {
        val segmentation = line.split("\t")(2).split("'").toList.map(x => MarkovMorphemeType.withName(x.capitalize))
        val freq = line.split("\t")(3).toDouble

        val states = List.fill(modelOrder)(MarkovMorphemeType.Start) ++ segmentation ++ List(MarkovMorphemeType.End)

        for (i <- List.range(0, segmentation.length + 1))
        yield (states.slice(i, i + modelOrder), states(i + modelOrder), freq)
      }).flatten.toList

      val totalTransitions = mutable.Map[List[MarkovMorphemeType.Value], mutable.Map[MarkovMorphemeType.Value, Double]]()
      for (transition <- individualTransitions) {
        val key = transition._1
        val nextState = transition._2
        val freq = transition._3
        if (!totalTransitions.contains(key)) {
          totalTransitions.update(key, mutable.Map[MarkovMorphemeType.Value, Double]())
        }
        if (!totalTransitions(key).contains(nextState)) {
          totalTransitions(key).update(nextState, 0.0)
        }
        totalTransitions(key)(nextState) += freq
      }
      val totals = (for (kvp <- totalTransitions) yield (kvp._1, kvp._2.values.sum)).toMap
      val transitionProbabilities =
        mutable.Map[List[MarkovMorphemeType.Value], Map[MarkovMorphemeType.Value, Double]]()
      for (key <- totalTransitions.keySet) {
        for (kvp <- totalTransitions(key)) {
          totalTransitions(key)(kvp._1) = kvp._2 / totals(key) / MarkovMorphemeType.typeCount(kvp._1).toDouble *
            MarkovMorphemeType.totalMorphemes().toDouble * multiplier
        }
        transitionProbabilities.update(key, totalTransitions(key).toMap)
      }
      transitionProbabilities.toMap
    }

    def evaluateSegmentation(segmentation: List[MarkovMorphemeType.Value]): (Double, Int) = {
      var prevStates = List.fill(modelOrder)(MarkovMorphemeType.Start)

      var zeroPenalty = 0
      var score = 1.0
      for (morphType <- segmentation :+ MarkovMorphemeType.End) {
        if (transitions.contains(prevStates) && transitions(prevStates).contains(morphType)) {
          score *= transitions(prevStates)(morphType)
        }
        else {
          score = 0
          zeroPenalty -= 1
        }
        prevStates = prevStates.slice(1, prevStates.length) :+ morphType
      }
      (score, zeroPenalty)
    }
  }

  /**
   * Trie data structure for storing morphemes along with their types
   */
  class TrieNode(theLetter: Char, theRoot: TrieNode = null) {
    val root: TrieNode = if (theRoot == null) this else theRoot
    val letter: Char = theLetter

    val markovMorphemeTypes: mutable.Set[MarkovMorphemeType.Value] = mutable.Set[MarkovMorphemeType.Value]()
    val ruleMorphemeTypes: mutable.Set[RuleMorphemeType.Value] = mutable.Set[RuleMorphemeType.Value]()

    val children: mutable.Map[Char, TrieNode] = mutable.Map[Char, TrieNode]()

    /**
     * Add child if doesn't exist
     * Add mt and smt (if not null) to the child's types
     */
    def getOrAddChild(theLetter: Char, mt: MarkovMorphemeType.Value = null): TrieNode = {
      val child =
        if (children.contains(theLetter)) {
          children(theLetter)
        }
        else {
          val newNode = new TrieNode(theLetter, this.root)
          children(theLetter) = newNode
          newNode
        }
      if (mt != null) {
        child.markovMorphemeTypes.add(mt)
        child.ruleMorphemeTypes.add(MarkovMorphemeType.toRuleMorphemeType(mt))
      }
      child
    }

    /**
     * Define new word in trie
     */
    def addWord(mt: MarkovMorphemeType.Value, word: String, index: Int = 0): Unit = {
      if (word.length > index) {
        val letter = word(index)
        val newIndex = index + 1
        val currentMt = if (word.length == newIndex) mt else null
        val child = this.getOrAddChild(letter, currentMt)
        child.addWord(mt, word, newIndex)
      }
    }

    /**
     * First pass segmentation: look for all legal morphemes
     */
    def findMorphemes(word: String, startIndex: Int = 0, nextIndex: Int = 0, prev: RuleMorphemeType.Value = null)
    : mutable.Set[List[String]] = {
      val solutions = mutable.Set[List[String]]()
      val validMorphemes =
        this.ruleMorphemeTypes.filter(mt =>
          RuleMorphemeType.agreesWithPrevious(mt, prev)
        )

      //end of word
      if (nextIndex == word.length) {
        //makes valid morpheme
        if (validMorphemes.exists(mt => RuleMorphemeType.isValidEnding(mt))) {
          solutions.add(List[String](word.substring(startIndex, nextIndex)))
        }
      }

      //not the end of word
      else {
        //can start back at trie root
        validMorphemes.foreach(prevMorpheme =>
          this.root.findMorphemes(word, nextIndex, nextIndex, prevMorpheme).foreach(list =>
            solutions.add(word.substring(startIndex, nextIndex) +: list)
          )
        )

        //can go on with current path
        if (this.children.contains(word(nextIndex))) {
          this.children(word(nextIndex)).findMorphemes(word, startIndex, nextIndex + 1, prev).foreach(sol =>
            solutions.add(sol)
          )
        }
      }

      solutions
    }

    def getAllTags(solution: List[String]): List[List[MarkovMorphemeType.Value]] = {
      if (solution.isEmpty) {
        List(List())
      }
      else {
        val nextSolutions = getAllTags(solution.slice(1, solution.size))
        (for {value <- getIndexedNode(solution(0)).markovMorphemeTypes
              subSolution <- nextSolutions
        } yield value :: subSolution).toList
      }
    }

    /**
     * Find trie node representing word
     */
    def getIndexedNode(word: String, index: Int = 0): TrieNode = {
      if (index >= word.length) {
        this
      }
      else if (this.children.contains(word(index))) {
        children(word(index)).getIndexedNode(word, index + 1)
      }
      else {
        null
      }
    }
  }

  object WordSegmenter {

    /**
     * read files and build trie of morphemes
     */
    def buildTrie(morphemesByTypeDir: String): TrieNode = {

      val morphemeTypeFileNames = Map[MarkovMorphemeType.Value, String](
        (MarkovMorphemeType.AdjEnding, "adjEnding.txt"),
        (MarkovMorphemeType.Adj, "adj.txt"),
        (MarkovMorphemeType.AdjSuffix, "adjSuffix.txt"),
        (MarkovMorphemeType.AdvEnding, "advEnding.txt"),
        (MarkovMorphemeType.Adverb, "adverb.txt"),
        (MarkovMorphemeType.Adv, "adv.txt"),
        (MarkovMorphemeType.Article, "article.txt"),
        (MarkovMorphemeType.Conjunction, "conjunction.txt"),
        (MarkovMorphemeType.Expression, "expression.txt"),
        (MarkovMorphemeType.MidEnding, "midEnding.txt"),
        (MarkovMorphemeType.NounEnding, "nounEnding.txt"),
        (MarkovMorphemeType.NounHumanPrefix, "nounHumanPrefix.txt"),
        (MarkovMorphemeType.NounHuman, "nounHuman.txt"),
        (MarkovMorphemeType.NounHumanSuffix, "nounHumanSuffix.txt"),
        (MarkovMorphemeType.NounPrefix, "nounPrefix.txt"),
        (MarkovMorphemeType.Noun, "noun.txt"),
        (MarkovMorphemeType.NounSuffix, "nounSuffix.txt"),
        (MarkovMorphemeType.Number, "number.txt"),
        (MarkovMorphemeType.NumberSuffix, "numberSuffix.txt"),
        (MarkovMorphemeType.O, "o.txt"),
        (MarkovMorphemeType.Preposition, "preposition.txt"),
        (MarkovMorphemeType.PrepPrefix, "prepPrefix.txt"),
        (MarkovMorphemeType.Pronoun, "pronoun.txt"),
        (MarkovMorphemeType.TablePronounEnding, "tablePronounEnding.txt"),
        (MarkovMorphemeType.Table, "table.txt"),
        (MarkovMorphemeType.TenseSuffix, "tenseSuffix.txt"),
        (MarkovMorphemeType.VerbEnding, "verbEnding.txt"),
        (MarkovMorphemeType.VerbPrefix, "verbPrefix.txt"),
        (MarkovMorphemeType.Verb, "verb.txt"),
        (MarkovMorphemeType.VerbSuffix, "verbSuffix.txt")
      )

      val trieRoot = new TrieNode('^')

      morphemeTypeFileNames.foreach(keyVal => {
        val words = Source.fromFile(new File(morphemesByTypeDir, keyVal._2).getPath).getLines().toList
        words.foreach(word =>
          trieRoot.addWord(keyVal._1, word)
        )
        MarkovMorphemeType.setWordCount(keyVal._1, words.length)
      })

      trieRoot
    }

    /**
     * for printing solution
     */
    def solutionString(solutions: List[List[String]]): String = {
      (for (list <- solutions) yield list.mkString("'")).mkString("\t")
    }

    /**
     * convert to x notation ("sxajnas")
     */
    def xNotation(word: String): String = {
      val hatMap: Map[String, String] = Map(
        "ĉ" -> "cx",
        "ĝ" -> "gx",
        "ĥ" -> "hx",
        "ĵ" -> "jx",
        "ŝ" -> "sx",
        "ŭ" -> "ux"
      )

      var newWord = word
      hatMap.foreach(kvp =>
        newWord = newWord.replaceAllLiterally(kvp._1, kvp._2)
      )
      newWord
    }

    def maximalMatch(segmentations: Set[List[String]]): List[String] = {
      val scores = for (solution <- segmentations) yield (solution.map(_.length), solution)
      if (scores.isEmpty) {
        List()
      }
      else {
        val best = scores.max
        best._2
      }
    }

    def main(args: Array[String]) = {

      val arguments: List[Char] = if (args.length < 3) List() else args(2).toCharArray.toList

      val maxMatch = arguments.contains('m')
      val random = arguments.contains('r')
      val noRules = arguments.contains('n')
      val useBigram = arguments.contains('b')
      val useTrigram = arguments.contains('t')

      if (noRules) {
        RuleMorphemeType.pleaseIgnoreRules()
      }

      if (args.length == 2 || args.length == 3) {
        val trieRoot = buildTrie(args(1))
        val markovModelOrder = if (useTrigram) 3 else if (useBigram) 2 else 1
        val markovModel = new MarkovModel(args(0), trieRoot, markovModelOrder)

        for (line <- Source.stdin.getLines()) {
          val word = line.split("\t")(0)

          //find legal segmentations
          val solutions = trieRoot.findMorphemes(xNotation(word))

          if (random) {
            println(solutionString(solutions.toList))
          }

          else if (maxMatch) {
            println(word + "\t" + solutionString(List(maximalMatch(solutions.toSet))))
          }

          else {
            // Markov model
            val solutionScores = for (solution <- solutions; tag <- trieRoot.getAllTags(solution)) yield {
              (solution, tag, markovModel.evaluateSegmentation(tag))
            }

            var bestSolutions = mutable.MutableList[List[String]]()
            var bestSoFar = (-1.0, 0)
            for (solAndScore <- solutionScores) {
              if (bestSoFar <= solAndScore._3) {
                if (bestSoFar < solAndScore._3) {
                  bestSoFar = solAndScore._3
                  bestSolutions.clear()
                }
                bestSolutions += solAndScore._1
              }
            }
            println(word + "\t" + solutionString(List(maximalMatch(bestSolutions.toSet))))
          }
        }

      }
      else {
        println("Usage: scala WordSegmenter.WordSegmenter trainingFile morphemesByTypeDirectory [-m|r|n|b|t]")
        println(args.toList)
      }
    }
  }

}