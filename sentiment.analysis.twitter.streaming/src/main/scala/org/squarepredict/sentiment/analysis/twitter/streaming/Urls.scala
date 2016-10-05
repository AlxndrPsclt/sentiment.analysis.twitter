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

class Urls {
  private var url = ""
  private var display_url = ""
  //text
  def getUrlEntities_url = url

  def setUrlEntities_url_=(str: String): Unit = url = str
  def getUrlEntities_urlDisplay = display_url

  def setUrlEntities_urlDisplay_=(str: String): Unit = display_url = str
}
