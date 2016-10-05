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

package org.squarepredict.sentiment.analysis.twitter.batch

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.sql.SQLContext
import java.io._
import org.squarepredict.sentiment.analysis.preprocessor._
import org.apache.spark.mllib.classification.{SVMModel, SVMWithSGD}
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.mllib.classification.{NaiveBayes, NaiveBayesModel}
import org.apache.spark.mllib.regression.LabeledPoint
import it.nerdammer.spark.hbase.HBaseReaderBuilder
import it.nerdammer.spark.hbase._
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import scala.collection.mutable.ArrayBuffer
//import org.apache.spark.mllib.evaluation.MulticlassMetrics




object BatchJob {
  val NB_ITERATIONS = 100

  def evaluate(metrics: MulticlassMetrics) = {
    // Precision by thresholds
    val fMeasure = metrics.fMeasure
    val precision = metrics.precision
    val pp = metrics.recall
    println("fMeasure =")
    // Summary stats
    println(s"Recall = ${metrics.recall}")
    println(s"Precision = ${metrics.precision}")
    println(s"F1 measure = ${metrics.fMeasure}")
    //println(s"Accuracy = ${metrics.accuracy}")
    
    // Individual label stats
    metrics.labels.foreach(label =>
      println(s"Class $label precision = ${metrics.precision(label)}"))
      metrics.labels.foreach(label => println(s"Class $label recall = ${metrics.recall(label)}"))
      metrics.labels.foreach(label => println(s"Class $label F1-score = ${metrics.fMeasure(label)}"))

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

  def list_models(sc: SparkContext, apath: String): Array[String] = {
    //Listing available models
    val fs = FileSystem.get(sc.hadoopConfiguration)
    val list_models = fs.listLocatedStatus(new Path(apath))

    var list_model_names = ArrayBuffer.empty[String]
    
    while (list_models.hasNext()) {
      val p = list_models.next().getPath()
      // returns the filename or directory name if directory
      val nameOfFile = p.getName() 
      list_model_names+=apath+nameOfFile
    }

    return list_model_names.toArray
  }

  def compareTuples(tup1: (String, Double), tup2: (String, Double)) = {
    if (tup1._2 > tup2._2) tup1 else tup2
  }


  def SavePerformanceInHbase(tableName: String, familyColumn: String, bestModel: String, f1_score:Double) {
    System.out.println("DEBUG ___________SAVING_PERFORMANCES")
    outputRDD.map(
      (bestModel, bestF1Score)
    ).toHBaseTable(tableName).inColumnFamily(familyColumn)
      .toColumns("bestModel", "F1-Score")
          .save()
  }


  def main(args: Array[String]) = {

    ///////////////////////
    //Unpacking arguments//
    ///////////////////////

    if (args.length < 8) {
          System.err.println("Usage: BatchJob master appName kafkaBrokers kafkaTopicInitial kafkaTopicWithSentiment zookeeper_quorum zookeeper_port inputTableName inputFamilyColumn")
          System.exit(1)
        }

    val Array(master, appName) = args.take(2) //Spark conf parameters
    val Array(kafkaBrokers, kafkaTopic) = args.slice(2,4)
    val Array(zookeeper_quorum, zookeeper_port) = args.slice(4,6)
    val Array(inputTableName, inputFamilyColumn) = args.slice(6,8)
    val Array(bestModelTableName, bestModelFamilyColumn) = args.slice(8,10)


    println("Master:")
    println(master)
    println("inputTableName:")
    println(inputTableName)
    println("Kafka broker:")
    println(kafkaBrokers)




    //////////////
    //Spark Conf//
    //////////////

    val sparkConf = new SparkConf().setAppName(appName)
    sparkConf.setMaster(master); //Local[x], yarn-client….
    sparkConf.set("spark.hbase.host", zookeeper_quorum).set("spark.hbase.port", zookeeper_port)
    val sc = new SparkContext(sparkConf);
    
    var list_model_names = list_models(sc, "/user/share/jobs-data/sentiment.analysis.twitter.models/")
    println("######################")
    println(list_model_names.mkString(" "))

    val (emojis_lexicon, emojis_list) = readLexiconMapAndListFromHBase(sc, "sentiment.analysis.lexicons",  "emojis")
    val (smileys_lexicon, smileys_list) = readLexiconMapAndListFromHBase(sc, "sentiment.analysis.lexicons", "smileys")
    val stopwords_list = readListFromHBase(sc, "sentiment.analysis.lexicons", "stopwords_fr")
    val prefixs_list = readListFromHBase(sc, "sentiment.analysis.lexicons", "prefixs")

    val preprocessor = new Preprocessor(emojis_lexicon, emojis_list,  smileys_lexicon, smileys_list, stopwords_list, prefixs_list)



    val tweets = readTextTweetsFromHBase(sc, inputTableName, inputFamilyColumn)
    val processed_tweets = tweets.map(tweet => preprocessor.preprocess(tweet))



    processed_tweets.take(4).foreach(println)
    //tweets.show()

    println("-----------Start of data loading-----------")

    val array_models = list_model_names.map(model_path => (model_path, NaiveBayesModel.load(sc, model_path)))
    
    //val model = array_models(0)

    val models = sc.parallelize(list_model_names)


    val allPredictionsAndLabels = array_models.map{ case (model_path, model) => (model_path, processed_tweets.map { processed_tweet => (model.predict(processed_tweet.features), processed_tweet.label) } ) }




    val metrics = allPredictionsAndLabels.map{ case (model_path, predictionsAndLabels) => (model_path, new MulticlassMetrics(predictionsAndLabels)) }

    val f1_scores = metrics.map{ case (model_path, metric) => (model_path, metric.fMeasure) }

    //val bestF1Score = f1_scores.max()
    //
    //val bestF1Score = f1_scores.reduce()
    
    val bestF1Score = f1_scores.reduce(compareTuples)

    println(bestF1Score)
    SavePerformanceInHbase(bestF1Score._1, bestF1Score._2)
    


    println("################## BATCH FINISHED RUNNING ###################")


    sc.stop();
  }
}
