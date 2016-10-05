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


package org.squarepredict.sentiment.analysis.twitter.streaming

import scala.collection.mutable.ArrayBuffer
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.client.HBaseAdmin
import org.apache.hadoop.hbase.HTableDescriptor
import org.apache.hadoop.hbase.HColumnDescriptor
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.client.HTable
import org.apache.hadoop.hbase.client.Put
import org.apache.spark.storage._
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.mllib.classification.NaiveBayesModel
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.storage._
import org.apache.spark.streaming._
import org.apache.spark.streaming.StreamingContext._
import org.apache.spark.streaming.twitter._
import org.apache.spark.streaming.twitter.TwitterUtils
import org.squarepredict.sentiment.analysis.preprocessor._
import com.google.gson.GsonBuilder
import it.nerdammer.spark.hbase.HBaseSparkConf
import org.apache.spark.mllib.feature.HashingTF
import it.nerdammer.spark.hbase._
import java.util.HashMap
import org.apache.spark.streaming.kafka._
import org.apache.spark.streaming.dstream._
import org.apache.kafka.clients.producer.{ProducerConfig, KafkaProducer, ProducerRecord}

object StreamingJob {
  val FEATURE_COUNT=100

  var numTweets_1 = 0L // point d'arret du streaming SaveInHbase_1
  val gson = new GsonBuilder().create()
  var numTweetsCollected = 0L //point  d' arret du streaming SaveInFile

  val hasher = new HashingTF(FEATURE_COUNT)

  def SaveInHbase_1(rdd: org.apache.spark.rdd.RDD[String], tableName: String, familyColumn: String) {
    val count = rdd.count()

    if (count > 0) {
      System.out.println("DEBUG __________________________COUNT = " + count)
      numTweets_1 += count
      val outputRDD = rdd.repartition(1)
      outputRDD.map(jsonStr => {
        val i = 0
        var champ = new ArrayBuffer[String]

        val tweet = gson.fromJson(jsonStr, classOf[TweetsInformations])

        champ = ArrayBuffer(tweet.getId.toString(), tweet.getCreatedAt, tweet.getSource, tweet.getLang, tweet.getText,
          tweet.getRetweetCount.toString(), tweet.getUser.getId_user.toString(), tweet.getUser.getName,
          tweet.getUser.getCreatedAt_user, "", "", "", "", tweet.getPlace.getBoundingBoxType, "")
        for (i <- 0 until champ.length) {
          if (champ(i).isEmpty()) {
            champ(i) = "NAN"
          }
        }

        if (!tweet.getHashtags.isEmpty) {
          val j = 0
          champ(9) = tweet.getHashtags(j).getHashtagText
          for (j <- 1 until tweet.getHashtags.length) {
            champ(9) = champ(9).concat(";").concat(tweet.getHashtags(j).getHashtagText)
          }
        }

        if (!tweet.getUrls.isEmpty) {
          val j = 0
          champ(10) = tweet.getUrls(j).getUrlEntities_url
          for (j <- 01 until tweet.getUrls.length) {
            champ(10) = champ(10).concat(";").concat(tweet.getUrls(j).getUrlEntities_url)
          }
        }

        if (!tweet.getMedias.isEmpty) {
          val j = 0
          champ(11) = tweet.getMedias(j).getMediaEntities_mediaurl
          for (j <- 1 until tweet.getMedias.length) {
            champ(11) = champ(11).concat(";").concat(tweet.getMedias(j).getMediaEntities_mediaurl)
          }
        }

        if (!tweet.getUserMention.isEmpty) {
          val j = 0
          champ(12) = tweet.getUserMention(j).getUserMention_name
          for (j <- 1 until tweet.getUserMention.length) {
            champ(12) = champ(12).concat(";").concat(tweet.getUserMention(j).getUserMention_name)
          }
        }

        if (!tweet.getPlace.getBoundingBoxCoordinates.isEmpty) {
          //
          val j = 0
          val i = 0
          champ(14) = ("Latitude_" + i).concat(" :").concat(tweet.getPlace.getBoundingBoxCoordinates(j)(i).getLatitude.toString()).concat(",").
            concat("Longitude_" + i).concat(" :").concat(tweet.getPlace.getBoundingBoxCoordinates(j)(i).getLongitude.toString())
          for (j <- 0 until tweet.getPlace.getBoundingBoxCoordinates.length) {
            for (i <- 1 until tweet.getPlace.getBoundingBoxCoordinates(j).length) {
              champ(14) = champ(14).concat(";").concat("Latitude_" + i).concat(" :").concat(tweet.getPlace.getBoundingBoxCoordinates(j)(i).getLatitude.toString()).concat(",").
                concat("Longitude_" + i).concat(" :").concat(tweet.getPlace.getBoundingBoxCoordinates(j)(i).getLongitude.toString())
            }
          }
        }

        (champ(0), champ(1), champ(2), champ(3), champ(4), champ(5), champ(6), champ(7), champ(8), champ(9), champ(10), champ(11), champ(12), champ(13), champ(14))

      }).toHBaseTable(tableName).inColumnFamily(familyColumn)
        .toColumns("createdAt", "source", "lang", "text", "retweetCount", "user_id", "user_name", "user_createdAt", "hashtags", "urls", "media", "userMention",
          "boundingBoxType", "boundingBoxCoordinates")
        .save()

    }

  }






  def isFrench(model: NaiveBayesModel, tweet: String) : Boolean = {
    val hashed = StreamingJob.hasher.transform(tweet.split("\\s").filter(x=>x!=""))
    val lang = model.predict(hashed)
    return lang==1.0
  }

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


  def main(args: Array[String]) = {

    ///////////////////////
    //Unpacking arguments//
    ///////////////////////
    if (args.length < 14) {
          System.err.println("Usage: StreamingJob master appName consumerKey consumerSecret accessToken accessTokenSecret kafkaBrokers kafkaTopicInitial kafkaTopicWithSentiment zookeeper_quorum zookeeper_port sentiment_model_path language_model_path subject tableName familyColumn filters*")
          System.exit(1)
        }

    val Array(master, appName) = args.take(2) //Spark conf parameters
    val Array(consumerKey, consumerSecret, accessToken, accessTokenSecret) = args.slice(2,6)     //Twitter credentials
    val Array(kafkaBrokers, kafkaTopicInitial, kafkaTopicWithSentiment) = args.slice(6,9)        //Kafka conf
    val Array(zookeeper_quorum, zookeeper_port) = args.slice(9,11)
    val Array(sentiment_model_path, language_model_path) = args.slice(11,13)                      //Models
    val Array(subject) = args.slice(13,14)                                                              //Will be added to the json representing the tweet()
    val Array(tableName, familyColumn) = args.slice(14,16)                                       //Htable conf
    var filters = args.takeRight(args.length - 16)


    println("Master:")
    println(master)
    println("tableName:")
    println(tableName)
    println("Kafka broker:")
    println(kafkaBrokers)
    println("Filters:")
    println(filters(0))



    






//////////////////////////////////
//Setting twitter authentication//
//////////////////////////////////
      

    
    System.setProperty("twitter4j.oauth.consumerKey", consumerKey)
    System.setProperty("twitter4j.oauth.consumerSecret", consumerSecret)
    System.setProperty("twitter4j.oauth.accessToken", accessToken)
    System.setProperty("twitter4j.oauth.accessTokenSecret", accessTokenSecret)
    




///////////////////////////////
//Spark and streaming context//
///////////////////////////////
    val sparkConf = new SparkConf().setMaster(master).setAppName(appName)
    sparkConf.set("spark.hbase.host", zookeeper_quorum).set("spark.hbase.port", zookeeper_port)
    //sparkConf.set("spark.hbase.host", quorum).set("spark.hbase.port", clientPort)
    val sc = new SparkContext(sparkConf)
    val ssc = new StreamingContext(sc, Seconds(10))

    val tweetStream = TwitterUtils.createStream(ssc, None, filters).map(gson.toJson(_))

////////////////
//HBase config//
////////////////

///////////////////////////////////////////
//Setting preprocessor and loading models//
///////////////////////////////////////////



    val (emojis_lexicon, emojis_list) = readLexiconMapAndListFromHBase(sc, "sentiment.analysis.lexicons",  "emojis")
    val (smileys_lexicon, smileys_list) = readLexiconMapAndListFromHBase(sc, "sentiment.analysis.lexicons", "smileys")
    val stopwords_list = readListFromHBase(sc, "sentiment.analysis.lexicons", "stopwords_fr")
    val prefixs_list = readListFromHBase(sc, "sentiment.analysis.lexicons", "prefixs")





    val preprocessor = new Preprocessor(emojis_lexicon, emojis_list,  smileys_lexicon, smileys_list, stopwords_list, prefixs_list)

    val language_detector_model  = NaiveBayesModel.load(sc, language_model_path)

    val sentiment_analysis_model = NaiveBayesModel.load(sc, sentiment_model_path)



////////////////////////
//Save tweets in hbase//
////////////////////////

    tweetStream.foreachRDD(SaveInHbase_1(_ , tableName, familyColumn))

////////////////////
//Action on tweets//
////////////////////

    tweetStream.foreachRDD( rdd => {
      System.out.println("# events = " + rdd.count())

      rdd.foreachPartition( partition => {
        // Print statements in this section are shown in the executor's stdout logs
        //val kafkaOpTopic = "tweets" 
        val props = new HashMap[String, Object]()
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers)
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
          "org.apache.kafka.common.serialization.StringSerializer")
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
          "org.apache.kafka.common.serialization.StringSerializer")

        val producer = new KafkaProducer[String, String](props)
        partition.foreach( tweet => {
          val tweets_and_subject = tweet.dropRight(1)+", \"subject\": \""+subject+"\"}"
          // As as debugging technique, users can write to DBFS to verify that records are being written out 
          // dbutils.fs.put("/tmp/test_kafka_output",data,true)
          val message = new ProducerRecord[String, String](kafkaTopicInitial, null, tweets_and_subject)
          producer.send(message)
        } )
      producer.close()
      }
    )})

    val tweets_with_text = tweetStream.map(tw => (tw, gson.fromJson(tw, classOf[TweetsInformations]).getText))
    val tweets = tweets_with_text.filter{ case (_, textTw) => textTw >= " " }


    val french_tweets = tweets.filter{ case(_, textTw) => isFrench(language_detector_model, textTw) }

    val cleaned_tweets = french_tweets.map{ case (jsonTw, textTw) => (jsonTw, preprocessor.preprocess(textTw)) }

    val predictions = cleaned_tweets.map { case(jsonTw, clean_tweet) => (jsonTw, sentiment_analysis_model.predict(clean_tweet.features))}


    predictions.foreachRDD( rdd => {
      System.out.println("# events = " + rdd.count())

      rdd.foreachPartition( partition => {
        // Print statements in this section are shown in the executor's stdout logs
        //val kafkaOpTopic = "sentiment" 
        val props = new HashMap[String, Object]()
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers)
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
          "org.apache.kafka.common.serialization.StringSerializer")
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
          "org.apache.kafka.common.serialization.StringSerializer")

        val producer = new KafkaProducer[String, String](props)
        partition.foreach{  case (jsonTw, prediction) => {
          // As as debugging technique, users can write to DBFS to verify that records are being written out 
          // dbutils.fs.put("/tmp/test_kafka_output",data,true)
        val tweet_subject_prediction = jsonTw.dropRight(1)+", \"subject\": \""+subject+"\", \"sentiment\": "+prediction.toString()+"}"

        val message = new ProducerRecord[String, String](kafkaTopicWithSentiment, null, tweet_subject_prediction)
        producer.send(message)
      }  }

      producer.close()
      }
    )})

    ssc.start()
    ssc.awaitTermination()



  }
}
