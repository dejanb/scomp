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

import org.fusesource.hawtbuf.Buffer
import Buffer._
import Stomp._
import java.util.UUID
import java.util.concurrent.{TimeUnit, LinkedBlockingQueue}

class StompClient extends FrameListener {
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
    val connectFrame = new StompFrame(Stomp.CONNECT)
    //TODO add headers; username/pass and stuff
    send(connectFrame)
    val connect = receive()
    if (connect.action == CONNECTED) {
       connected = true
       sessionId = connect.getHeader(SESSION).getOrElse(DEFAULT_SESSION_ID)
    } else {
      reset
      throw new Exception("expected " + CONNECTED + " but received " + connect);
    }

  }

  def disconnect = {
    val disconnectFrame = new StompFrame(Stomp.DISCONNECT)
    send(disconnectFrame)
    transport.stop
    reset
  }

  // send methods

  def send(frame: StompFrame): Unit = {
    transport.send(frame)
  }

  def send(destination: String, text: String, persistent: Boolean = false): Unit = {
    val frame = new StompFrame(Stomp.SEND, List((ascii("destination"), ascii(destination))), new BufferContent(ascii(text)));
    if (persistent) {
      frame.headers ::= (Stomp.PERSISTENT, ascii("true"));
    }
    send(frame)
  }

  // api

  def subscribe(destination: String) = {
    val id = generateId
    val frame = new StompFrame(Stomp.SUBSCRIBE,
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
    if (frame.action == Stomp.MESSAGE) {
      val id = frame.getHeader(Stomp.SUBSCRIPTION).get.toString
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