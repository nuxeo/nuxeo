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

import java.nio.charset.StandardCharsets

import com.redis.RedisClientPool
import io.gatling.core.Predef._


object Redis {
  val namespace = System.getProperty("redisNamespace", "imp")

  lazy val pool = {
    val host = System.getProperty("redisHost", "localhost")
    val port = Integer.getInteger("redisPort", 6379)
    val db = Integer.getInteger("redisDb", 7)
    val ret = new RedisClientPool(host, port, database = db)
    ret.withClient(
      client => {
        // clean temp
        zpopSha = client.scriptLoad(zpopLua).get
        println("XXX redis " + host + ":" + port)
        println("XXX redis zpop sha: " + zpopSha)
      })
    ret
  }

  // Could be simpler once https://github.com/debasishg/scala-redis/pull/129 is fixed and integrated into gatling
  val zpopLua = """
       local val = redis.call('zrange', KEYS[1], 0, 0)[1]
       if val == nil then
           return 'EMPTY_MARKER'
       end
       redis.call('zremrangebyrank',KEYS[1], 0, 0)
       return val"""

  var zpopSha = ""


  def setupFolderFeed() = {
    pool.withClient(
      client => {
        // clean temp
        client.del(namespace + ":temp:folder:created")
        client.del(namespace + ":temp:folder:creating")
        client.del(namespace + ":temp:folder:toCreate")
        // copy zset to a temp ns
        client.zunionstore(namespace + ":temp:folder:toCreate", List(namespace + ":folder"))
        println("XXX number of folders: " + getNumberOfFolders())
      })

  }

  def setupDocFeed() = {
    pool.withClient(
      client => {
        // clean temp
        client.del(namespace + ":temp:doc:created")
        client.del(namespace + ":temp:doc:creating")
        client.del(namespace + ":temp:doc:toCreate")
        // copy zset to a temp ns
        client.sunionstore(namespace + ":temp:doc:toCreate", namespace + ":doc")
        println("XXX number of documents: " + getNumberOfDocuments())
      })

  }

  def getNumberOfDocuments(): Int = {
    pool.withClient(
      client => {
        val ret = client.scard(namespace + ":temp:doc:toCreate").get.toInt
        ret
      })
  }

  def getNumberOfFolders(): Int = {
    pool.withClient(
      client => {
        val ret = client.zcard(namespace + ":temp:folder:toCreate").get.toInt
        ret
      })
  }

  def getNextDoc() = {
    var map = Map[String, String]()
    pool.withClient(
      client => {
        val docKey = client.spop[String](namespace + ":temp:doc:toCreate").getOrElse(Constants.EMPTY_MARKER)
        if (Constants.EMPTY_MARKER == docKey) {
          map = Map(Constants.END_OF_FEED -> "true")
          println("XXX getNextDoc END OF FEED")
        } else {
          client.sadd(namespace + ":temp:doc:creating", docKey)
          map = client.hgetall1[String, String](namespace + ":data:" + docKey).get
          map = map + ("key" -> docKey)
          // println("XXX getNextDoc " + docKey + " returns " + map)
        }
      })
    map
  }

  def getNextFolder() = {
    var map = Map[String, String]()
    pool.withClient(
      client => {
        // damn scala
        val docKeyOpt: Option[Any] = client.evalSHA(zpopSha, List(namespace + ":temp:folder:toCreate"), List())
        val docKey = new String(docKeyOpt.get.asInstanceOf[Array[Byte]], StandardCharsets.UTF_8)
        if (Constants.EMPTY_MARKER == docKey) {
          map = Map(Constants.END_OF_FEED -> "true")
          println("XXX getNextFolder END OF FEED")
        } else {
          // println("XXX get folder zpop " + docKey)
          client.sadd(namespace + ":temp:folder:creating", docKey)
          map = client.hgetall1[String, String](namespace + ":data:" + docKey).get
          // println("XXX getNextFolder " + docKey + " return " + map)
        }
      })
    map
  }

  def markFolderCreated(session: Session) = {
    if (session("status").as[Int] == 201) {
      val docKey = session("key").as[String]
      pool.withClient(
        client => {
          client.srem(namespace + ":temp:folder:creating", docKey)
          client.sadd(namespace + ":temp:folder:created", docKey)
        }
      )
    }
    false
  }

  def markDocumentCreated(session: Session) = {
    if (session("status").as[Int] == 201) {
      val docKey = session("key").as[String]
      pool.withClient(
        client => {
          client.srem(namespace + ":temp:doc:creating", docKey)
          client.sadd(namespace + ":temp:doc:created", docKey)
        }
      )
    }
    false
  }

  def getRandomDoc() = {
    var map = Map[String, String]()
    pool.withClient(
      client => {
        val docKey = client.srandmember[String](namespace + ":temp:doc:created").get
        map = client.hgetall1[String, String](namespace + ":data:" + docKey).get
        // println("XXX getRandomDoc " + docKey + " returns " + map)
      })
    map
  }

  def getRandomFolder() = {
    var map = Map[String, String]()
    pool.withClient(
      client => {
        val docKey = client.srandmember[String](namespace + ":temp:folder:created").get
        map = client.hgetall1[String, String](namespace + ":data:" + docKey).get
        // println("XXX getRandomFolder " + docKey + " returns " + map)
      })
    map
  }

}
