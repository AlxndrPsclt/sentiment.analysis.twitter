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

import org.squarepredict.sentiment.analysis.twitter.streaming.Coordinates

class Place {
  private var name = ""
  private var countryCode = ""
  private var id = ""
  private var country = ""
  private var placeType = ""
  private var url = ""
  private var fullName = ""
  private var boundingBoxType = ""
  private var boundingBoxCoordinates = Array[Array[Coordinates]]()
  //place name
  def getPlace_name = name
  def setPlace_name_=(x: String): Unit = name = x
  //countryCode
  def getCountryCode = countryCode
  def setCountryCode_=(x: String): Unit = countryCode = x
  
  //id
  def getPLace_id = id
  def setPLace_id_=(x: String): Unit = id = x
  //country
   def getCounty = country
  def setCountry_=(x: String): Unit = country = x
  //placeType
   def getPlaceType = placeType
  def setPlaceType_=(x: String): Unit = placeType = x
  //url
   def getPLace_url = url
  def setPLace_url_=(x: String): Unit = url = x
  //fullName
  def getPlace_fullName = fullName
  def setPlace_fullName_=(x: String): Unit = fullName = x
  //coordinates
  def getBoundingBoxCoordinates = boundingBoxCoordinates
  def setBoundingBoxCoordinates_=(entite: Array[Array[Coordinates]]): Unit = boundingBoxCoordinates = entite
  //BoundingBoxType
  def getBoundingBoxType = boundingBoxType
  def setBoundingBoxType_=(entite: String): Unit = boundingBoxType = entite
  //
}
