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

package org.squarepredict.sentiment.analysis.preprocessor

import java.text.Normalizer

import scala.io.Source

import org.apache.spark.mllib.feature.HashingTF
import org.apache.spark.mllib.regression.LabeledPoint
import org.json4s.JDouble
import org.json4s.JField
import org.json4s.JObject
import org.json4s.JString
import org.json4s.JValue
import org.json4s.native.JsonMethods.parse

import com.cybozu.labs.langdetect.DetectorFactory

object Preprocessor {
  type Lexicon = List[scala.collection.immutable.Map[String,String]]
  val WORDS_LEXICON_PATH = "conf/test/resources/data/lexicons/final_lexicon.json"
  val EMOJIS_LEXICON_PATH = "conf/test/resources/data/lexicons/emojis_lexicon.json"
  val SMILEYS_LEXICON_PATH = "conf/test/resources/data/lexicons/smileys_lexicon.json"

  val STOPWORDS_PATH="conf/test/resources/data/wordlists/fr.json"
  val PREFIXS_PATH="conf/test/resources/data/wordlists/prefixs.json"

  val SPECIAL_CHARS_REGEX="""[^A-Za-z #]"""
  val TWITTER_URL_REGEX="""https://t.co/(\w)*"""
  val TWITTER_USERNAME_REGEX="""@(\w)*"""

  val FEATURE_COUNT=250
  val NEGATIVE_TRESHOLD=0.25
  val POSITIVE_TRESHOLD=0.3

  val LANG_DETECT_PROFILE_PATH="conf/test/resources/profiles"
  //

  val hasher = new HashingTF(FEATURE_COUNT)

  //val languageProfiles = new LanguageProfileReader().readAllBuiltIn()

  //val languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard()).withProfiles(languageProfiles).build()
  
  System.out.println("Welcome, we are executed here:");
  System.out.println("Working Directory = " + System.getProperty("user.dir"));

  //DetectorFactory.loadProfile(LANG_DETECT_PROFILE_PATH);


  def loadLexicon(filename: String) : org.json4s.JsonAST.JValue = {
    val loaded_file = Source.fromFile(filename).mkString
    println(loaded_file)
    val lexicon: JValue = parse(loaded_file)
    return lexicon
  }

  def loadLexiconFromJsonString(json_lexicon: String) : org.json4s.JsonAST.JValue = {
    val lexicon: JValue = parse(json_lexicon)
    return lexicon
  }

  def loadListFromLexicon(json: org.json4s.JsonAST.JValue, keyword: String = "word") =
    for {
      JObject(child) <- json
      JField("word", JString(word)) <- child    ///Passing the keyword as an argument doesn't work, for some obscure reason. Instead we code it in hard.
    } yield word

  def loadList(filename: String): List[String]= {
    return this.loadListFromLexicon(loadLexicon(filename), "word")
  }


  def loadListFromJsonString(json_lexicon: String): List[String]= {
    return this.loadListFromLexicon(loadLexiconFromJsonString(json_lexicon), "word")
  }

  def getResourcePath(resource: String) : String = {
    println("Path for the resource files:")
    val fpath = getClass.getResource(resource)
    println(fpath)
    return fpath.getPath()
  }
}

class Preprocessor(emojis_lexicon_path:String,
      smileys_lexicon_path:String,
      stopwords_path:String,
      prefixs_path:String,
      feature_count:Int,
      negative_treshold:Double,
      positive_treshold:Double) extends Serializable {

///////////////////////////////////
//Constructors and initialisation//
///////////////////////////////////
  type Lexicon = List[scala.collection.immutable.Map[String,String]]

  var emojis_lexicon: Lexicon=_
  var smileys_lexicon: Lexicon=_
  var words_lexicon: Lexicon=_
  var emojis_list: List[String]=_
  var smileys_list: List[String]=_
  var words_list: List[String]=_
  var stopwords_list: List[String]=_
  var prefixs_list: List[String]=_

//  def this() {
//    this(Preprocessor.EMOJIS_LEXICON_PATH,
//      Preprocessor.SMILEYS_LEXICON_PATH,
//      Preprocessor.STOPWORDS_PATH,
//      Preprocessor.PREFIXS_PATH,
//      Preprocessor.FEATURE_COUNT,
//      Preprocessor.NEGATIVE_TRESHOLD,
//      Preprocessor.POSITIVE_TRESHOLD)
//
//      println("Instanciated with short constructor.")
//      loadRessources()
//  }

//  def this(emojis_lexicon_path:String, smileys_lexicon_path:String, stopwords_path:String, prefixs_path:String) = {
//  
//    this(emojis_lexicon_path, smileys_lexicon_path, stopwords_path, prefixs_path,
//      Preprocessor.FEATURE_COUNT,
//      Preprocessor.NEGATIVE_TRESHOLD,
//      Preprocessor.POSITIVE_TRESHOLD)
//
//      println("Path for the resource files:")
//      var fpath = getClass.getResource("/emojis_lexicon.json")
//      //var cLoader = cls.getClassLoader()
//      //val fpath = cLoader.getResourceAsStream("emojis_lexicon.json");
//      println(fpath)
//    
//      println("Instanciated with special path constructor.")
//      loadRessources()
//  }


//  def this(emojis_lexicon_json:String, smileys_lexicon_json:String, stopwords_json:String, prefixs_json:String) = {
//    //CONSTRUCTOR
//  
//    this("loaded_from_json_string", "loaded_from_json_string", "loaded_from_json_string", "loaded_from_json_string",
//      Preprocessor.FEATURE_COUNT,
//      Preprocessor.NEGATIVE_TRESHOLD,
//      Preprocessor.POSITIVE_TRESHOLD)
//
//    loadRessourcesFromJsonStrings(emojis_lexicon_json, smileys_lexicon_json, stopwords_json, prefixs_json)
//  }

  def this(emojis_lexicon_map_list: List[scala.collection.immutable.Map[String,String]], emojis_list: List[String],
    smileys_lexicon_map_list: List[scala.collection.immutable.Map[String,String]], smileys_list: List[String], 
    stopwords_list: List[String], prefixs_list: List[String]) = {
    this("loaded_using_map_lists","loaded_using_map_lists","loaded_using_map_lists","loaded_using_map_lists",
      Preprocessor.FEATURE_COUNT,
      Preprocessor.NEGATIVE_TRESHOLD,
      Preprocessor.POSITIVE_TRESHOLD)

      this.attachEmojisLexicon(emojis_lexicon_map_list)
      this.attachEmojisList(emojis_list)
      this.attachSmileysLexicon(smileys_lexicon_map_list)
      this.attachSmileysList(smileys_list)
      this.attachStopwordsList(stopwords_list)
      this.attachPrefixsList(prefixs_list)
  }


//  def loadRessources() = {
//    emojis_lexicon = Preprocessor.loadLexicon(Preprocessor.getResourcePath(emojis_lexicon_path))
//    emojis_list = Preprocessor.loadListFromLexicon(emojis_lexicon)
//
//    smileys_lexicon = Preprocessor.loadLexicon(Preprocessor.getResourcePath(smileys_lexicon_path))
//    smileys_list = Preprocessor.loadListFromLexicon(smileys_lexicon)
//
//    stopwords_list = Preprocessor.loadList(Preprocessor.getResourcePath(stopwords_path))
//    prefixs_list = Preprocessor.loadList(Preprocessor.getResourcePath(prefixs_path))
//    println("Assets loaded.")
//  }

  def attachEmojisLexicon(emojis_lexicon: Lexicon) = {
    this.emojis_lexicon = emojis_lexicon
  }

  def attachSmileysLexicon(smileys_lexicon: Lexicon) = {
    this.smileys_lexicon = smileys_lexicon
  }

  def attachEmojisList(emojis_list: List[String]) = {
    this.emojis_list = emojis_list
  }

  def attachSmileysList(smileys_list: List[String]) = {
    this.smileys_list = smileys_list
  }

  def attachStopwordsList(stopwords_list: List[String]) = {
    this.stopwords_list = stopwords_list
  }

  def attachPrefixsList(prefixs_list: List[String]) = {
    this.prefixs_list = prefixs_list
  }

//  def loadRessourcesFromJsonStrings(emojis_lexicon_json:String, smileys_lexicon_json:String, stopwords_json:String, prefixs_json:String) = {
//    println("Loading some resource")
//    println("Loading emojis...")
//    emojis_lexicon = Preprocessor.loadLexiconFromJsonString(emojis_lexicon_json)
//    //emojis_list = Preprocessor.loadListFromLexicon(emojis_lexicon)
//
//    println("Loading smileys...")
//    smileys_lexicon = Preprocessor.loadLexiconFromJsonString(smileys_lexicon_json)
//    println("Smileys loaded.")
    //smileys_list = Preprocessor.loadListFromLexicon(smileys_lexicon)

//    println("Loading stopwords...")
//    stopwords_list = Preprocessor.loadListFromJsonString(stopwords_json)
//    println("Loading prefixes...")
//    prefixs_list = Preprocessor.loadListFromJsonString(prefixs_json)
//    println("Assets loaded via Json Strings.")
//  }

////////////////////////////
//Pre treatement functions//
////////////////////////////
  def extractListElementsFromStringAndClean(aList: List[String], aString: String) : (List[String], String)  = {
    val modified = this.removeText(aList, aString)
    val rList = aList.filter( elt => aString contains elt)
    return (rList, modified)
  }

  def removeWords(wordlist: List[String], aString: String): String = {
    var modified = aString
    wordlist.foreach { toReplace => modified= modified.replaceAll("\\b"+toReplace+"\\b", " ") }
    return modified
  }

  def removeText(wordlist: List[String], aString: String): String = {
    var modified = aString
    wordlist.foreach { toReplace => modified= modified.replace(toReplace, " ") }
    return modified
  }

  def removeUrls(tweet: String): String = {
    return tweet.replaceAll(Preprocessor.TWITTER_URL_REGEX, "url")
  }

  def removeUsernames(tweet: String): String = {
    return tweet.replaceAll(Preprocessor.TWITTER_USERNAME_REGEX, "username")
  }

  def removeSpecialChars(tweet: String): String = {
    return tweet.replaceAll(Preprocessor.SPECIAL_CHARS_REGEX, " ")
  }

  def removeAccents(tweet: String): String= {
    return Normalizer.normalize(tweet, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
  }

  def removeAccentsAndSpecialChars(tweet: String): String = {
    return removeSpecialChars(removeAccents(tweet))
  }

  def extractScoresFromJson(wordlist: List[String], lexicon: org.json4s.JsonAST.JValue) = {
    for {
      JObject(child) <- lexicon
      JField("word", JString(word)) <- child    ///Passing the string keyword as an argument doesn't work, for some obscure reason (?!!). Instead we code it in hard.
      JField("pos", JDouble(pos)) <- child
      JField("neg", JDouble(neg)) <- child
      if wordlist.contains(word)
    } yield pos + neg
  }

  def extractScores(wordlist: List[String], lexicon: Lexicon): List[Float] = {
    val listeValues = for (lexicon_entry <- lexicon if wordlist.contains(lexicon_entry("word")))
      yield (lexicon_entry("pos").toString.toFloat + lexicon_entry("neg").toString.toFloat)
    return listeValues
  }

  def computeScore(wordlist: List[String], lexicon: Lexicon): Double = {
    wordlist match {
      case Nil => 0
      case _ => extractScores(wordlist, lexicon).foldLeft(0.0)(_+_)
    }
  }

  def getLabelValue(score: Double): Double = {
    score match {
      case s if (s<Preprocessor.NEGATIVE_TRESHOLD) => 0.0
      case s if (s>Preprocessor.POSITIVE_TRESHOLD) => 2.0
      case _ => 1.0
    }
  }

//  def detectFrenchOld(tweet: String): Boolean = {
//    val lang = Preprocessor.languageDetector.detect(tweet)
//    var langtxt = "Nothing"
//    if (lang.isPresent()) {
//        langtxt = lang.get().toString()
//    }
//    langtxt match {
//      case "fr" => return true
//      case _ => return false
//    }
//  }
//
//This function is temporarily removed because we are using a Naive Bayes model to check the language.
//  def detectFrench(tweet: String): Boolean =  {
//    var detector = DetectorFactory.create();
//    detector.append(tweet);
//    var langtxt = detector.detect();
//    langtxt match {
//      case "fr" => return true
//      case _ => return false
//    }
//  }


  def clean(tweet: String): (String, List[String], List[String]) = {
    val (tweet_emojis_list, tweet_without_emojis) = this.extractListElementsFromStringAndClean(this.emojis_list, tweet)
    val (tweet_smileys_list, tweet_without_smileys) = this.extractListElementsFromStringAndClean(this.smileys_list, tweet_without_emojis)

    var text_tweet = tweet_without_smileys.toLowerCase()
    text_tweet = this.removeUrls(text_tweet)
    text_tweet = this.removeUsernames(text_tweet)
    text_tweet = this.removeAccents(text_tweet)
    text_tweet = this.removeWords(this.stopwords_list, text_tweet)
    text_tweet = this.removeWords(this.prefixs_list, text_tweet)
    text_tweet = this.removeSpecialChars(text_tweet)

    return (text_tweet, tweet_emojis_list, tweet_smileys_list)
  }

  def preprocess(tweet: String): LabeledPoint = {
    val (cleaned_tweet, tweet_emojis_list, tweet_smileys_list)= clean(tweet)
    val smiley_score = this.computeScore(tweet_smileys_list, this.smileys_lexicon)
    val emoji_score = this.computeScore(tweet_emojis_list, this.emojis_lexicon)

    val score = smiley_score + emoji_score
    val label = this.getLabelValue(score)

    val hashed = Preprocessor.hasher.transform(cleaned_tweet.split("\\s").filter(x=>x!=""))

    return LabeledPoint(label, hashed)
  }
}
