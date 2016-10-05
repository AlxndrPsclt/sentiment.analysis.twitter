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

class Medias {
  private var url = ""
  private var mediaURL =""
  def getMediaEntities_url = url

  def setMediaEntities_url_=(str: String): Unit = url = str
  def getMediaEntities_mediaurl = mediaURL

  def setMediaEntities_mediaurl_=(str: String): Unit = mediaURL = str


}
