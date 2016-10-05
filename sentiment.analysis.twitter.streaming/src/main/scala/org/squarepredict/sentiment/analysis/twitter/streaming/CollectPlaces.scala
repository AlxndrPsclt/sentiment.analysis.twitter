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

import java.io.File
import org.apache.spark.sql._
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{ Seconds, StreamingContext }
import org.apache.spark.{ SparkConf, SparkContext }
import com.google.gson.Gson
import org.apache.spark._
import it.nerdammer.spark.hbase._
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.client.HBaseAdmin
import org.apache.hadoop.hbase.HTableDescriptor
import org.apache.hadoop.hbase.HColumnDescriptor
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.client.HTable
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.mapreduce.TableInputFormat
import org.apache.spark.storage._

import org.squarepredict.sentiment.analysis.twitter.streaming.TweetsInformations
import com.google.gson.GsonBuilder
import scala.collection.mutable.ArrayBuffer
import twitter4j.conf.ConfigurationBuilder

object CollectPLace {
  var tableName = "Place"
  var familColumn = "place"
  var gson = new GsonBuilder().create();
  var numTweets = 0L
  var numTweetsCollected = 0L
  def main(args: Array[String]) {

    // Process program arguments and set properties
    if (args.length < 4) {
      System.err.println("Usage: TwitterPopularTags <consumer key> <consumer secret> " +
        "<access token> <access token secret> [<filters>]")
      System.exit(1)
    }

    val Array(consumerKey, consumerSecret, accessToken, accessTokenSecret) = args.take(4)

    //\"#orage\", \"#Tempete\", \"#Orage\", \"#inondation\", \"Innondation\", \"#tempête\",
    val filters = Array("#Paris", "#orage")

    // can use them to generat OAuth credentials
    System.setProperty("twitter4j.oauth.consumerKey", consumerKey)
    System.setProperty("twitter4j.oauth.consumerSecret", consumerSecret)
    System.setProperty("twitter4j.oauth.accessToken", accessToken)
    System.setProperty("twitter4j.oauth.accessTokenSecret", accessTokenSecret)

    //Spark
    //val conf = new SparkConf().setAppName(this.getClass().getName()).setMaster(args(4))
    val sc = new SparkContext(args(4), this.getClass().getName())
    val ssc = new StreamingContext(sc, Seconds(2))

    //HBASE
    val confHbase = HBaseConfiguration.create()
    confHbase.set("hbase.master", args(4))
    confHbase.set("hbase.zookeeper.quorum", sys.env.getOrElse("ZOOKEEPER_QUORUM", "localhost"))
    confHbase.set("hbase.zookeeper.property.clientPort", sys.env.getOrElse("ZOOKEPPER_CLIENTPORT", "2181"))
    confHbase.setLong("hbase.rpc.timeout", 3000000L)

    val admin = new HBaseAdmin(confHbase)

    if (admin.isTableAvailable(tableName)) {
      admin.disableTable(tableName)
      admin.deleteTable(tableName)
    }

    val tableDesc = new HTableDescriptor(tableName)
    tableDesc.addFamily(new HColumnDescriptor(familColumn.getBytes()))
    admin.createTable(tableDesc)
    val tweetStream = TwitterUtils.createStream(ssc, None, filters).map(gson.toJson(_))

    tweetStream.foreachRDD((rdd, time) => {
      val count = rdd.count()
      // var ligne = new Array[String](3)

      if (count > 0) {
        numTweets += count
        val outputRDD = rdd.repartition(1)
        outputRDD.map(jsonStr => {
          val i = 0

          var champ = new ArrayBuffer[String]
          val json = "{\"id\":603151064788017152, \"place\":{\"name\":\"Nilüfer\",\"countryCode\":\"TR\",\"id\":\"3166615088e5d058\",\"country\":\"Türkiye\",\"placeType\":\"city\",\"url\":\"https://api.twitter.com/1.1/geo/id/3166615088e5d058.json\", \"fullName\":\"Nilüfer, Bursa\",\"boundingBoxType\":\"Polygon\",\"boundingBoxCoordinates\":[[{\"latitude\":40.1769659,\"longitude\":28.8959199}, {\"latitude\":40.2754603,\"longitude\":28.8959199},{\"latitude\":40.2754603,\"longitude\":29.0127621}, {\"latitude\":40.1769659,\"longitude\":29.0127621}]]}}"
          val tweet = gson.fromJson(json, classOf[TweetsInformations])

          champ = ArrayBuffer(tweet.getId.toString(), "", "")
          for (i <- 0 until champ.length) {
            if (champ(i).isEmpty()) {
              champ(i) = "NAN"

            }
          }

       
            
          if (!tweet.getPlace.getBoundingBoxCoordinates.isEmpty) {
            val j = 0
            val i = 0
            champ(2) = ("Latitude_"+i).concat(" :").concat(tweet.getPlace.getBoundingBoxCoordinates(j)(i).getLatitude.toString()).concat(",").
                 concat("Longitude_"+i) .concat(" :").concat(tweet.getPlace.getBoundingBoxCoordinates(j)(i).getLongitude.toString())
            for (j <- 0 until tweet.getPlace.getBoundingBoxCoordinates.length) {
              for (i <- 1 until tweet.getPlace.getBoundingBoxCoordinates(j).length) {
                champ(2) = champ(2).concat(";").concat("Latitude_"+i).concat(" :").concat(tweet.getPlace.getBoundingBoxCoordinates(j)(i).getLatitude.toString()).concat(",").
                 concat("Longitude_"+i) .concat(" :").concat(tweet.getPlace.getBoundingBoxCoordinates(j)(i).getLongitude.toString())
              }
            }
           
          }
          System.out.println("______champs(2)_______\n" + champ(2))
          (champ(0), champ(1), champ(2))

        }).toHBaseTable(tableName).inColumnFamily(familColumn)
          .toColumns("boundingBoxType", "boundingBoxCoordinates")
          .save()

        if (numTweets > 2) {
          System.exit(0)
        }
      }

    })

  
    ssc.start()

    ssc.awaitTermination()
    ssc.stop(true, true)
  }
}
