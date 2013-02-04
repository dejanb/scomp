/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.stomp.scomp

import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import org.apache.activemq.apollo.broker.Broker
import org.apache.activemq.apollo.util.ServiceControl
import org.apache.activemq.apollo.dto.AcceptingConnectorDTO
import org.apache.activemq.apollo.stomp.Stomp._
import scala.Some
import util.{Failure, Success}
import concurrent.Await

@RunWith(classOf[JUnitRunner])
class StompTest extends FunSuite with ShouldMatchers with BeforeAndAfterAll {
  var broker: Broker = null
  var port = 61613

  def createBroker = {
    broker = new Broker()
    broker.config.connectors.clear()
    val connector = new AcceptingConnectorDTO()
    connector.id = "tcp"
    connector.bind = "tcp://0.0.0.0:61613"
    broker.config.connectors.add(connector)
    broker
  }

  override protected def beforeAll() {
    try {
      broker = createBroker
      ServiceControl.start(broker, "Starting broker")
    }
    catch {
      case e: Throwable => e.printStackTrace()
    }
  }

  override protected def afterAll() {
    ServiceControl.stop(broker)
  }

  test("Stomp queue send/receive") {
    val client = new StompClient
    client.connect("localhost", port)
    client.connected should be(right = true)
    client.sessionId.toString should not be ("")
    val msgNum = 20
    val sub = client.subscribe("/queue/test")
    for (i <- 1 to msgNum) {
      client.send(destination = "/queue/test", text = "test message " + i, persistent = true)
    }
    for (i <- 1 to msgNum) {
      val message = sub.receive(1000)
      message should not be null
      message.content.utf8.toString should be("test message " + i)
    }
    client.disconnect()
  }

  test("Stomp topic test two subs") {
    val client = new StompClient
    client.connect("localhost", port)
    client.connected should be(right = true)
    client.sessionId.toString should not be ("")

    val sub1 = client.subscribe("/topic/test")
    val sub2 = client.subscribe("/topic/test")

    Thread.sleep(1000)

    val msgNum = 20

    for (i <- 1 to msgNum) {
      client.send(destination = "/topic/test", text = "test message " + i)
    }


    for (i <- 1 to msgNum) {
      val msg1 = sub1.receive(1000)
      msg1 should not be null
      msg1.content.utf8.toString should be("test message " + i)

      val msg2 = sub2.receive(1000)

      msg2 should not be null
      msg2.content.utf8.toString should be("test message " + i)
    }

    client.disconnect()
  }

  test("Stomp async receive") {
    val client = new StompClient
    client.connect("localhost", port)
    client.connected should be(right = true)
    client.sessionId.toString should not be ("")

    val msgNum = 20
    var received = 0

    client.subscribe("/queue/asyncTest", Some((frame) => {
      received += 1
    }))

    for (i <- 1 to msgNum) {
      client.sendReceive(destination = "/queue/asyncTest", text = "test message " + i)
    }

    Thread.sleep(3000)
    received should be(msgNum)
    client.disconnect()
  }

  test("Stomp req/response ") {
    import scala.concurrent.duration._
    import scala.concurrent.ExecutionContext.Implicits.global
    val timeOut: Duration = 100.milliseconds

    val client = new StompClient
    client.connect("localhost", port)
    client.connected should be(right = true)
    client.sessionId.toString should not be ("")

    val msgNum = 20
    var received = 0

    //test responder for the server side
    client.subscribe("/queue/bar", listener = Some((frame) => {
      val replyTo = frame.header(REPLY_TO)
      client.send(destination = replyTo.toString, text = "" + frame.content.utf8)
    }))

    // requester, awaits a future response
    for (i <- 1 to msgNum) {
      val future = client.sendReceive(destination = "/queue/bar", text = "test message" + 1)
      future onComplete {
        case Success(frame) => {
          if (frame != null) {
            received += 1
          }
        }
        case Failure(exception) =>
      }
      Await.result(future, timeOut)
    }
    Thread.sleep(3000)
    received should be(msgNum)
    client.disconnect()
  }
}