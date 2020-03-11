/*
 * (C) Copyright 2015-2019 Nuxeo (http://nuxeo.com/) and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Delbosc Benoit
 */
package org.nuxeo.cap.bench

import io.gatling.core.Predef._

object Feeders {

  // csv feeds
  val users = csv("data/users.csv").circular
  val admins = csv("data/admins.csv").circular
  val fulltextSearch = csv("data/fulltext.csv").circular

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
