package org.nuxeo.cap.bench
/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Delbosc Benoit
 */

import io.gatling.core.Predef._

object Feeders {

  // csv feeds
  val users = csv("users.csv").circular
  val admins = csv("admins.csv").circular
  val fulltextSearch = csv("fulltext.csv").circular

  // redis feeds
  def createDocFeeder() = {
    Redis.setupDocFeed()
    Iterator.continually(Redis.getNextDoc())
  }

  def createFolderFeeder() = {
    Redis.setupFolderFeed()
    Iterator.continually(Redis.getNextFolder())
    // bounded version
    // Iterator.tabulate(getNumberOfFolder())(n => getNextFolder())
  }

  def createRandomDocFeeder() = {
    Iterator.continually(Redis.getRandomDoc())
  }

  // Tell if a redis feeder is not empty
  def notEmpty(session:Session) = {
    session(Constants.END_OF_FEED).asOption[String] == None
  }


  // val doc = Iterator.tabulate(getNumberOfDoc("doc")) (n => getDocToCreate("doc"))
  // val doc2 = Iterator.continually(getDocToCreate("f1"))
  // val f1 = Iterator.tabulate(getNumberOfDoc("f1")) (n => getDocToCreate("f1"))
  // val f2 = Iterator.tabulate(getNumberOfDoc("f2")) (n => getDocToCreate("f2"))
  // val f3 = Iterator.tabulate(getNumberOfDoc("f3")) (n => getDocToCreate("f3"))
  // val f1 = Iterator.continually(getDocToCreate("f1"))
  // val f2 = Iterator.continually(getDocToCreate("f2"))
  // val f3 = Iterator.continually(getDocToCreate("f2"))
}
