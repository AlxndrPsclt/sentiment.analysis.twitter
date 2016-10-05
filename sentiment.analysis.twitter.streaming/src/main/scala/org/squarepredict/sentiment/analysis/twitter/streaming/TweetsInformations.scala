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
import org.squarepredict.sentiment.analysis.twitter.streaming.User
import scala.collection.mutable.ArrayBuffer

import org.squarepredict.sentiment.analysis.twitter.streaming.Hashtags
class TweetsInformations /*(id: Int, created_at: String, source: String, retweeted: Int, text: String, user: Array[String]) */ {
  private var id = 0L
  private var createdAt = ""
  private var source = ""
  private var retweetCount = 0L
  private var text = ""
  private var lang = ""
 
  private var user = new User()

  private var hashtagEntities = Array[Hashtags]()

  private var urlEntities = Array[Urls]()
  private var mediaEntities = Array[Medias]()
  private var userMentionEntities = Array[UserMention]()
  private var place = new Place()
   private var inReplyToStatusId = 0L
  private var inReplyToUserId = 0L
  private var isFavorited = false
  private var isPossiblySensitive = false
  private var isTruncated = false
  //id

  def getId = id

  def setId_=(value: Long): Unit = id = value

  //createdAT
  def getSource = source

  def setSource_=(x: String): Unit = source = x
  //source
  def getCreatedAt = createdAt

  def setCreatedAt_=(x: String): Unit = createdAt = x
  //text
  def getText = text

  def setText_=(str: String): Unit = text = str
  //lang
  def getLang = lang

  def setLang_=(str: String): Unit = lang = str

  //getuser
  def getUser = user
  def setUser_=(usr: User): Unit = user = usr
  //retweetedCount
  def getRetweetCount = retweetCount

  def setRetweetCount_=(value: Long): Unit = retweetCount = value
  //reply tweet
  def getInReplyToStatusId = inReplyToStatusId

  def setInReplyToStatusId_=(value: Long): Unit = inReplyToStatusId = value

  def getInReplyToUserId = inReplyToUserId

  def setInReplyToUserId_=(value: Long): Unit = inReplyToUserId = value
  //isFavorited
  def getIsFavorited = isFavorited

  def setIsFavorited_=(value: Boolean): Unit = isFavorited = value

  //isPossiblySensitive
  def getIsPossiblySensitive = isPossiblySensitive

  def setIsPossiblySensitive_=(value: Boolean): Unit = isPossiblySensitive = value

  //isTruncated
  def getIsTruncated = isTruncated

  def setIsTruncated_=(value: Boolean): Unit = isTruncated = value

  //hashtags
  def getHashtags = hashtagEntities
  def setHashtags_=(entite: Array[Hashtags]): Unit = hashtagEntities = entite
  //urls
  def getUrls = urlEntities
  def setUrls_=(entite: Array[Urls]): Unit = urlEntities = entite

  //medias
  def getMedias = mediaEntities
  def setMedias_=(entite: Array[Medias]): Unit = mediaEntities = entite

  //User Mention

  def getUserMention = userMentionEntities
  def setuserMention_=(entite: Array[UserMention]): Unit = userMentionEntities = entite
//place
  def getPlace = place
  def setPlace_=(plce: Place): Unit = place = plce
}
