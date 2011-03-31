/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
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
 */
package org.fusesource.stomp.scomp

import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.fusesource.hawtbuf.Buffer
import Buffer._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite}
import org.apache.activemq.apollo.broker.{BrokerFactory, Broker}
import org.apache.activemq.apollo.util.ServiceControl

@RunWith(classOf[JUnitRunner])
class StompTest extends FunSuite with ShouldMatchers with BeforeAndAfterAll {

  var broker: Broker = null
  var port = 61613


  override protected def beforeAll() = {
    try {
      broker = BrokerFactory.createBroker("xml:classpath:apollo-stomp.xml")
      ServiceControl.start(broker, "Starting broker")
    }
    catch {
      case e:Throwable => e.printStackTrace
    }
  }

  override protected def afterAll() = {
    broker.stop
  }

  test("Stomp queue send/receive") {
    val client = new StompClient
    client.connect("localhost", port)
    client.connected should be(true)
    client.sessionId.toString should not be (Stomp.DEFAULT_SESSION_ID.toString)

    val msgNum = 20;

    val sub = client.subscribe("/queue/test")


    for (i <- 1 to msgNum) {
      client.send("/queue/test", "test message", true)
    }


    for (i <- 1 to msgNum) {
      val message = sub.receive(1000)
      message should not be null
      message.content.utf8.toString should be ("test message")
    }

    client.disconnect
  }

  test("Stomp topic test two subs") {
    val client = new StompClient
    client.connect("localhost", port)
    client.connected should be(true)
    client.sessionId.toString should not be (Stomp.DEFAULT_SESSION_ID.toString)

    val sub1 = client.subscribe("/topic/test")
    val sub2 = client.subscribe("/topic/test")

    Thread.sleep(1000)

    client.send("/topic/test", "a message",  false)

    val msg1 = sub1.receive(1000)
    msg1 should not be null
    msg1.content.utf8.toString should be ("a message")

    val msg2 = sub2.receive(1000)
    msg2 should not be null
    msg2.content.utf8.toString should be ("a message")

    client.disconnect
  }

}


