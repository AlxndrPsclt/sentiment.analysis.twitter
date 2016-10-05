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

class User {
  private var id = 0L
  private var name = ""
  private var createdAt = ""

  private var location = ""
  private var utcOffset = ""

  private var followersCount = 0L
  private var friendsCount = 0L

  private var lang = ""
  private var isContributorsEnabled = false

  private var isFollowRequestSent = false
  private var isProtected = false
  private var isVerified = false

  private var isGeoEnabled = false

  private var screenName = ""
  ///getters et setters
  def getId_user = id

  def setId_user_=(value: Long): Unit = id = value
  //name
  def getName = name

  def setName_=(x: String): Unit = name = x
  //location
  def getLocation = location

  def setLocation_=(str: String): Unit = location = str
  //lang 
   def getLang_user = lang

  def setLang_user_=(str: String): Unit = lang = str
  //screenName
  def getScreenName = screenName

  def setScreenName_=(str: String): Unit = screenName = str
  //followers count
  def getFollowers_count_user = followersCount

  def followersCountunt_user_=(value: Long): Unit = followersCount = value
  //friendsCount
   def getFriendsCount_user = friendsCount

  def setFriendsCount_user_=(value: Long): Unit = friendsCount = value
  //created AT
  def getCreatedAt_user = createdAt

  def setCreatedAt_user_=(str: String): Unit = createdAt = str
  //name
  def getUtc_offset = utcOffset

  def setUtc_offset_=(x: String): Unit = utcOffset = x
  //isGeoEnabled
  def getIsGeoEnabled = isGeoEnabled

  def setIsGeoEnabled_=(b: Boolean): Unit = isGeoEnabled = b
//isContributorsEnabled
  def getIsContributorsEnabled = isContributorsEnabled

  def setIsContributorsEnabled_=(b: Boolean): Unit = isContributorsEnabled = b
  
  //isFollowRequestSent
  def getIsFollowRequestSent = isFollowRequestSent

  def setIisFollowRequestSent_=(b: Boolean): Unit = isFollowRequestSent = b
  //isProtected
   def getIsProtected = isProtected

  def setIsProtected_=(b: Boolean): Unit = isProtected = b
  //isVerified
  
  def getIsVerified = isVerified

  def setIsVerified_=(b: Boolean): Unit = isVerified = b
}
