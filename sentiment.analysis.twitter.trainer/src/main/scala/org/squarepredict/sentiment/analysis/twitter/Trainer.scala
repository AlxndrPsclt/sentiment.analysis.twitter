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


package org.squarepredict.sentiment.analysis.twitter

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import it.nerdammer.spark.hbase.HBaseReaderBuilder
import it.nerdammer.spark.hbase._
import org.squarepredict.sentiment.analysis.preprocessor.Preprocessor
import org.apache.spark.mllib.classification.NaiveBayes

object Trainer {
  val NB_ITERATIONS = 100


  def readLexiconFromHBase(sc: SparkContext, table: String, columnFamily: String): HBaseReaderBuilder[(String, String, String, String, String, 
 String)] = {
    val hBaseRDD = sc.hbaseTable[(String, String, String, String, String, String)](table)
        .select("word", "pos", "neg", "neutre", "total", "value")
            .inColumnFamily(columnFamily)
    return hBaseRDD
  }

  def readListFromHBase(sc: SparkContext, table: String, columnFamily: String): List[String] = {
    val hBaseRDD = sc.hbaseTable[String](table)
        .select("word")
            .inColumnFamily(columnFamily)
    return hBaseRDD.collect().toList
  }

  def readLexiconMapAndListFromHBase(sc: SparkContext, table: String, columnFamily: String): (List[scala.collection.immutable.Map[String,String]], List[String])= {
    val rddLexicon = readLexiconFromHBase(sc, table, columnFamily)

    val rddLexiconMapList = rddLexicon.map(x => Map(("word", x._1), ("pos", x._2), ("neg", x._3), ("neutre", x._4), ("total", x._5), ("value", x._6)))
    val lexiconMapList = rddLexiconMapList.collect().toList

    val lexiconList = rddLexicon.map(x => x._1).collect().toList

    println(lexiconMapList)
    return (lexiconMapList, lexiconList)
  }

  def readFullTweetsFromHBase(sc: SparkContext, table: String, columnFamily: String): HBaseReaderBuilder[(String, String, String, String, String, String, String, String, String, String, String, String, String, String)] = {
    val hBaseRDD = sc.hbaseTable[(String, String, String, String, String, String, String, String, String, String, String, String, String, String)](table)
        .select( "user_id", "boundingBoxCoordinates", "hashtags", "source", "lang", "retweetCount", "boundingBoxType", "urls", "userMention", "user_createdAt", "createdAt", "media", "user_name", "text")
            .inColumnFamily(columnFamily)
    return hBaseRDD
  }

  def readTextTweetsFromHBase(sc: SparkContext, table: String, columnFamily: String): HBaseReaderBuilder[(String)] = {
    val hBaseRDD = sc.hbaseTable[(String)](table)
        .select("text")
            .inColumnFamily(columnFamily)
    return hBaseRDD
  }


  def main(args: Array[String]) = {

    ///////////////////////
    //Unpacking arguments//
    ///////////////////////

    val (master, inputtable, familColumn, outputfile) =
      if (args.length == 4)
        (args(0), args(1), args(2), args(3))
      else
        (null, null, null, null)

    println(master)

    val sparkConf = new SparkConf().setAppName("SentimentAnalysisTraining")
    sparkConf.setMaster(args(0)); //Local[x], yarn-client….
    //sparkConf.set("spark.hbase.host", quorum).set("spark.hbase.port", clientPort)

    val sc = new SparkContext(sparkConf);


    val (emojis_lexicon, emojis_list) = readLexiconMapAndListFromHBase(sc, "sentiment.analysis.lexicons",  "emojis")
    val (smileys_lexicon, smileys_list) = readLexiconMapAndListFromHBase(sc, "sentiment.analysis.lexicons", "smileys")
    val stopwords_list = readListFromHBase(sc, "sentiment.analysis.lexicons", "stopwords_fr")
    val prefixs_list = readListFromHBase(sc, "sentiment.analysis.lexicons", "prefixs")

    val preprocessor = new Preprocessor(emojis_lexicon, emojis_list,  smileys_lexicon, smileys_list, stopwords_list, prefixs_list)


    val tweets = readTextTweetsFromHBase(sc, inputtable, familColumn)

    
    val processed_tweets = tweets.map(tweet => preprocessor.preprocess(tweet))
    

    val model = NaiveBayes.train(processed_tweets, lambda = 1.0, modelType = "multinomial")

    val unique_model_name = outputfile+(System.currentTimeMillis / 1000).toString

    println("################## SAVING THE MODEL ###################")
    model.save(sc, unique_model_name)
  }

}
