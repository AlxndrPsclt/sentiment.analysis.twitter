//This program is free software; you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation; either version 2 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//For the complete terms of the GNU General Public License, please see this URL:
//http://www.gnu.org/licenses/gpl-2.0.html


package org.squarepredict.sentiment.analysis.preprocessor.test

import org.scalatest.junit.AssertionsForJUnit
import org.junit.Test
import org.squarepredict.sentiment.analysis.preprocessor.Preprocessor
import org.junit._
import org.junit.Assert._

class PreprocessorTest extends AssertionsForJUnit {
  var preprocessor:Preprocessor = _

  @Before def preparePreprocessor {
    //val WORDS_LEXICON_PATH = "conf/test/resources/data/lexicons/final_lexicon.json"
    //val EMOJIS_LEXICON_PATH = "conf/test/resources/data/lexicons/emojis_lexicon.json"
    //val SMILEYS_LEXICON_PATH = "conf/test/resources/data/lexicons/smileys_lexicon.json"

    val WORDS_LEXICON_PATH = "conf/test/resources/data/lexicons/final_lexicon.csv"
    val EMOJIS_LEXICON_PATH = "conf/test/resources/data/lexicons/emojis_lexicon.csv"
    val SMILEYS_LEXICON_PATH = "conf/test/resources/data/lexicons/smileys_lexicon.csv"

    val STOPWORDS_PATH="conf/test/resources/data/wordlists/fr.json"
    val PREFIXS_PATH="conf/test/resources/data/wordlists/prefixs.json"

    val EMOJIS_LEXICON_LIST_PATH = "conf/test/resources/data/lexicons/emojis_list.txt"
    val SMILEYS_LEXICON_LIST_PATH = "conf/test/resources/data/lexicons/smileys_list.txt"

    val STOPWORDS_LIST_PATH="conf/test/resources/data/wordlists/fr_list.txt"
    val PREFIXS_LIST_PATH="conf/test/resources/data/wordlists/prefixs_list.txt"

    //val emojis_lexicon_json = scala.io.Source.fromFile(EMOJIS_LEXICON_PATH).mkString
    val emojis_lexicon_file= scala.io.Source.fromFile(EMOJIS_LEXICON_PATH).getLines.map(line => line.split(","))
    val emojis_lexicon= emojis_lexicon_file.map(elt => Map("word" -> elt(0), "pos" -> elt(1), "neg" -> elt(2), "total" -> elt(3), "neutre" -> elt(4), "value" -> elt(5))).toList

    val smileys_lexicon_file= scala.io.Source.fromFile(SMILEYS_LEXICON_PATH).getLines.map(line => line.split(","))
    val smileys_lexicon= smileys_lexicon_file.map(elt => Map("word" -> elt(0), "pos" -> elt(1), "neg" -> elt(2), "total" -> elt(3), "neutre" -> elt(4), "value" -> elt(5))).toList


    val emojis_list = scala.io.Source.fromFile(EMOJIS_LEXICON_LIST_PATH).getLines.toList
    val smileys_list = scala.io.Source.fromFile(SMILEYS_LEXICON_LIST_PATH).getLines.toList
    val stopwords_list = scala.io.Source.fromFile(STOPWORDS_LIST_PATH).getLines.toList
    val prefixs_list = scala.io.Source.fromFile(PREFIXS_LIST_PATH).getLines.toList

    
    println("Emojis lexicon")
    println(emojis_lexicon)
    println("Smileys lexicon")
    println(smileys_lexicon)

//    preprocessor = new Preprocessor(emojis_lexicon_json, smileys_lexicon_json, "{}", "{}")
    preprocessor = new Preprocessor(emojis_lexicon, emojis_list,  smileys_lexicon, smileys_list, stopwords_list, prefixs_list)
    //preprocessor = new Preprocessor()

    preprocessor.attachEmojisList(emojis_list) 
    preprocessor.attachSmileysList(smileys_list) 
    preprocessor.attachStopwordsList(stopwords_list) 
    preprocessor.attachPrefixsList(prefixs_list) 
  }

  def assertForMe(operation: String => String, text: String, original: String, destination: String) {
    val modified = operation(original)
    assertEquals(text, destination, modified)
  }

  @Test def testRemoveWords {
    assertForMe(tweet => preprocessor.removeWords(List("mot", "phrase"), tweet),
      "Remplacing entire words",
      "Ceci est un mot à remplacer",
      "Ceci est un   à remplacer")

    assertForMe(tweet => preprocessor.removeWords(List("mot", "phrase"), tweet),
      "Remplacing entire words",
      "Ceci est un motif à ne pas remplacer",
      "Ceci est un motif à ne pas remplacer")
  }

  @Test def testRemoveUrls {
  assertForMe(preprocessor.removeUrls,
    "Remove URLs",
     "Un tweet avec https://t.co/BL4QKXdkXj and stuff.",
     "Un tweet avec url and stuff.")
  }

  @Test def testRemoveUsernames {
  assertForMe(preprocessor.removeUsernames,
    "Remove Usernames",
     "A tweet with @someuser to remove.",
     "A tweet with username to remove.")
  }

  @Test def testRemoveAccentsAndSpecialChars {
  assertForMe(preprocessor.removeAccentsAndSpecialChars,
    "Remove Accents",
     "A tweet with accent$ and spécial Chars to remove.",
     "A tweet with accent  and special Chars to remove ")
  }

  @Test def testExtractAndRemoveEmojis {
    val tweet = "👏Merci pour la 📷"
    val (tweet_emojis_list, clean_tweet) = preprocessor.extractListElementsFromStringAndClean(preprocessor.emojis_list, tweet)
    assertForMe(atweet => atweet,
    "Remove Emojis",
    clean_tweet,
     " Merci pour la  ")
    assertEquals(tweet_emojis_list, List("👏","📷"))
  }

  @Test def testExtractAndRemoveSmileys {
    val tweet = "Ceci est :) un tweets pour tester:( comment;) ça marche."
    val (tweet_smileys_list, clean_tweet) = preprocessor.extractListElementsFromStringAndClean(preprocessor.smileys_list, tweet)
    assertForMe(atweet => atweet,
    "Remove Smileys",
    clean_tweet,
    "Ceci est   un tweets pour tester  comment;) ça marche.")
    assertEquals(tweet_smileys_list, List(":)",":("))
  }

//  @Test def testLangDetection = {
//    val is_french = preprocessor.detectFrench("Essayons de detecter ce tweet en langue européenne.")
//    assertTrue(is_french)
//    val is_not_french = preprocessor.detectFrench("Some other thext, this time in a diffrent language.")
//    assertFalse(is_not_french)
//  }

}
