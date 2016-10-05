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

class UserMention {
  private var id = 0L
  private var name = ""
  private var name_screen = ""
  def getUserMention_id = id

  def setUserMention_id_=(value: Long): Unit = id = value
  def getUserMention_name = name

  def setUserMention_name_=(str: String): Unit = name = str
  def getUserMention_name_screen = name_screen

  def setUserMention_name_screen_=(str: String): Unit = name_screen = str
}
