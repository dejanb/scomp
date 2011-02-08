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

import java.net.{InetSocketAddress, Socket}
import java.io._
import org.fusesource.hawtbuf.Buffer
import _root_.org.fusesource.hawtbuf.{ByteArrayOutputStream => BAOS}
import Buffer._
import Stomp._

class StompClient {
  var socket: Socket = new Socket
  var out: OutputStream = null
  var in: InputStream = null
  val bufferSize = 64 * 1204

  var open = false
  var connected = false
  var sessionId = DEFAULT_SESSION_ID

  def connect(host: String, port: Int, user: String = null, password: String = null) {
    // open socket
    socket = new Socket
    socket.connect(new InetSocketAddress(host, port))
    socket.setSoLinger(true, 0)
    out = new BufferedOutputStream(socket.getOutputStream, bufferSize)
    in = new BufferedInputStream(socket.getInputStream, bufferSize)

    // send CONNECT frame
    val connectFrame = new StompFrame(Stomp.CONNECT)
    //TODO add headers; username/pass and stuff
    connectFrame.send(out)
    val connect = receive
    if (connect.action == CONNECTED) {
       connected = true
       sessionId = connect.getHeader(SESSION).getOrElse(DEFAULT_SESSION_ID)
    } else {
      reset
      throw new Exception("expected " + CONNECTED + " but received " + connect);
    }

  }

  def reset() = {
    connected = false
    sessionId = DEFAULT_SESSION_ID
  }

  def receive(): StompFrame = {
    var start = true;
    val buffer = new BAOS()
    var c = in.read
    while (c >= 0) {
      if (c == 0) {
        println("received " + ascii(buffer.toBuffer))
        return StompCodec.decode(buffer.toBuffer)
      }
      if (!start || c != Stomp.NEWLINE) {
        start = false
        buffer.write(c)
      }
      c = in.read()
    }
    throw new EOFException()
  }

}