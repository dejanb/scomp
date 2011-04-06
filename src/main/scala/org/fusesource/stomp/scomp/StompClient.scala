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

import Stomp._
import java.util.UUID
import java.util.concurrent.{TimeUnit, LinkedBlockingQueue}
import org.fusesource.hawtbuf.{Buffer, AsciiBuffer}
import Buffer._
import collection.mutable.ListBuffer

class StompClient extends FrameListener with Logging {
  val bufferSize = 64 * 1204

  var open = false
  var connected = false
  var sessionId = DEFAULT_SESSION_ID
  var transport: StompTransport = _

  var subscriptions = Map[String, StompSubscription]()

  def connect(host: String, port: Int, user: String = null, password: String = null) = {
    transport = new StompTransport(this)
    transport.init(host, port)
    transport.start

    // send CONNECT frame
    var headers = new ListBuffer[(AsciiBuffer, AsciiBuffer)]
    headers += ((ACCEPT_VERSION, V1_1))
    headers += ((HOST, ascii(host)))
    if (user != null) {
      headers += ((LOGIN, ascii(user)))
      headers += ((PASSCODE, ascii(password)))
    }

    val connectFrame = new StompFrame(STOMP, headers.toList)

    send(connectFrame)
    val connect = receive()
    if (connect.action == CONNECTED) {
       val connectedVersion = connect.getHeader(VERSION).orNull
       if (connectedVersion == null || connectedVersion != V1_1) {
         disconnect
         throw new Exception("Version " + connectedVersion + " " + VERSION + " of Stomp protocol is not supported")
       }
       connected = true
       sessionId = connect.getHeader(SESSION).getOrElse(DEFAULT_SESSION_ID)
      info("Successfully connected to " + connect.getHeader(SERVER).getOrElse(ascii(" unknown ")) + " at " + host + ":" + port)
    } else {
      reset
      throw new Exception("Expected " + CONNECTED + " but received " + connect);
    }

  }

  def disconnect = {
    val disconnectFrame = new StompFrame(DISCONNECT)
    send(disconnectFrame)
    transport.stop
    reset
  }

  // send methods

  def send(frame: StompFrame): Unit = {
    transport.send(frame)
  }

  def send(destination: String, text: String, persistent: Boolean = false): Unit = {
    val frame = new StompFrame(SEND, List((ascii(DESTINATION), ascii(destination))), new BufferContent(ascii(text)));
    if (persistent) {
      frame.headers ::= (PERSISTENT, ascii("true"));
    }
    send(frame)
  }

  // api

  def subscribe(destination: String) = {
    val id = generateId
    val frame = new StompFrame(SUBSCRIBE,
        List((ascii("destination"), ascii(destination)),
             (ascii("id"), ascii(id))
        )
    );
    send(frame)
    val sub = new StompSubscription(id)
    subscriptions += id -> sub
    sub
  }

  override def onStompFrame(frame: StompFrame) = {
    if (frame.action == MESSAGE) {
      val id = frame.getHeader(SUBSCRIPTION).get.toString
      if (subscriptions.contains(id)) {
        subscriptions(id).onStompFrame(frame)
      } else {
        super.onStompFrame(frame)
      }
    }
    super.onStompFrame(frame)
  }

  def reset() = {
    connected = false
    sessionId = DEFAULT_SESSION_ID
  }

  def generateId() = {
    UUID.randomUUID.toString;
  }

}