/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.stomp.scomp

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.fusesource.hawtbuf.Buffer
import Buffer._
import Stomp._


@RunWith(classOf[JUnitRunner])
class StompTest extends FunSuite with ShouldMatchers {

  test("Stomp codec") {
    val frame = new StompFrame(ascii("SEND"), List((ascii("destination"), ascii("/queue/test"))), new BufferContent(ascii("Test")))
    println(ascii(StompCodec.encode(frame)))
  }

  test("Stomp connect") {
    val client = new StompClient
    client.connect("localhost", 61613)
    println(client.connected + " " + client.sessionId)
  }

}


