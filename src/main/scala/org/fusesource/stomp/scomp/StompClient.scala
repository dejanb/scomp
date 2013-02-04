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

import java.util.UUID
import org.fusesource.hawtbuf.{Buffer, AsciiBuffer}
import Buffer._
import collection.mutable.ListBuffer
import org.apache.activemq.apollo.stomp.{Stomp, StompFrame, BufferContent}
import Stomp._
import concurrent.Future
import java.util.concurrent.TimeoutException
import collection.concurrent.TrieMap
import org.eintr.loglady.Logging

class StompClient extends FrameListener with Logging {
  val DEFAULT_SESSION_ID = ascii("-1")
  val bufferSize = 64 * 1204
  var open = false
  var connected = false
  var sessionId = DEFAULT_SESSION_ID
  var transport: StompTransport = _
  val subscriptions = new TrieMap[String, StompSubscription]()

  def connect(host: String, port: Int, user: String = null, password: String = null) {
    transport = new StompTransport(this)
    transport.init(host, port)
    transport.start()
    val headers = new ListBuffer[(AsciiBuffer, AsciiBuffer)]
    headers += ((ACCEPT_VERSION, V1_1))
    headers += ((HOST, ascii(host)))
    if (user != null) {
      headers += ((LOGIN, ascii(user)))
      headers += ((PASSCODE, ascii(password)))
    }
    val connectFrame = StompFrame(STOMP, headers.toList)
    send(connectFrame)
    val connect = receive()
    if (connect.action == CONNECTED) {
      val connectedVersion = connect.header(VERSION)
      if (connectedVersion == null || connectedVersion != V1_1) {
        disconnect()
        throw new Exception("Version " + connectedVersion + " " + VERSION + " of Stomp protocol is not supported")
      }
      connected = true
      sessionId = connect.header(SESSION)
    }
    else {
      reset()
      throw new Exception("Expected " + CONNECTED + " but received " + connect)
    }
  }

  def disconnect() {
    val disconnectFrame = StompFrame(DISCONNECT)
    send(disconnectFrame)
    // Thread.sleep(100)
    transport.stop()
    reset()
  }

  def send(frame: StompFrame) {
    transport.send(frame)
  }

  def send(destination: String, text: String, correlationId: Option[String] = None, persistent: Boolean = false) {
    val headers = new ListBuffer[(AsciiBuffer, AsciiBuffer)]
    headers += ((DESTINATION, ascii(destination)))
    if (persistent) {
      headers += ((PERSISTENT, ascii("true")))
    }
    correlationId match {
      case Some(id) => headers += ((ID, ascii(id)))
      case None =>
    }
    send(StompFrame(SEND, headers.toList, new BufferContent(ascii(text))))
  }

  def sendReceive(destination: String, text: String): Future[StompFrame] = {
    import scala.concurrent.promise
    val id = generateId()
    val replyTo = "/temp-queue/" + id
    //  val timeOut = 100
    val headers = List((DESTINATION, ascii(destination)), (REPLY_TO, ascii(replyTo)), (CORRELATION_ID, ascii(id)))
    val frame = StompFrame(SEND, headers, new BufferContent(ascii(text)))
    send(frame)
    val p = promise[StompFrame]()
    subscribe(destination = replyTo, id = id, listener = Some((response) => {
      if (response == null) {
        p.failure(new TimeoutException())
      }
      else {
        p.success(response)
      }
      unsubscribe(id)
    }))
    p.future
  }

  def unsubscribe(id: String) {
    send(StompFrame(UNSUBSCRIBE, List((ID, ascii(id)))))
    subscriptions -= (id)
  }


  def subscribe(destination: String, listener: Option[(StompFrame) => Unit] = None): StompSubscription = {
    subscribe(destination = destination, id = generateId(), listener = listener)
  }

  def subscribe(destination: String, id: String, listener: Option[(StompFrame) => Unit]): StompSubscription = {
    val frame = StompFrame(SUBSCRIBE, List((DESTINATION, ascii(destination)), (ID, ascii(id))))
    val subscription = new StompSubscription(id)
    if (listener.isDefined) {
      subscription.addListener(listener)
    }
    else {
    }
    subscriptions += (id -> subscription)
    send(frame)
    subscription
  }

  override def onStompFrame(frame: StompFrame) = {
    if (frame.action == MESSAGE) {
      val id = frame.header(SUBSCRIPTION).toString
      if (subscriptions.contains(id)) {
        subscriptions(id).onStompFrame(frame)
      }
      /*else {
        super.onStompFrame(frame)
      }*/
    }
    super.onStompFrame(frame)

  }

  def reset() {
    connected = false
    sessionId = DEFAULT_SESSION_ID
  }

  def generateId() = {
    UUID.randomUUID.toString
  }
}