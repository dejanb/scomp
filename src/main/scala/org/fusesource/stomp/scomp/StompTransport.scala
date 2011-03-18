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


import org.fusesource.hawtdispatch._
import java.nio.channels.{SelectionKey, SocketChannel}
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.io.IOException
import java.util.concurrent.TimeUnit
import org.fusesource.hawtbuf.{ByteArrayOutputStream => BAOS}
import org.fusesource.hawtbuf.Buffer
import Buffer._


class StompTransport(client: StompClient) extends Logging {

  protected var channel: SocketChannel = _
  protected var queue = createQueue("Stomp Transport")

  protected val readBuffer = ByteBuffer.allocate(64 * 1024)
  protected var readSource: DispatchSource = _
  protected var connected = false

  def init(host: String, port: Int) {
    channel = SocketChannel.open
    channel.configureBlocking(false)
    val source: DispatchSource = createSource(channel, SelectionKey.OP_CONNECT, queue)

    def finishConnect = {
      if (channel != null && !channel.isConnected) {
        if (channel.finishConnect) {
            info("Successfully connected to " + host + ":" + port)
          source.release
          readSource = createSource(channel, SelectionKey.OP_READ, queue)
          readSource.setEventHandler(^{
            readFrames
          })
          readSource.resume
          connected = true
        } else {
          throw new IOException("Connect timeout!")
        }
      }
    }

    source.setEventHandler(^{
      finishConnect
    })
    source.resume

    queue.after(5, TimeUnit.SECONDS) {
      finishConnect
    }

    channel.connect(new InetSocketAddress(host, port))

  }

  def readFrames: Unit = {
    var frameStart = true
    val buffer = new BAOS()
    val bytesRead = channel.read(readBuffer);
    while (bytesRead != -1) {
      readBuffer.flip
      while (readBuffer.hasRemaining) {
        val c = readBuffer.get
        if (c == 0) {
          debug("Received:\n" +  ascii(buffer.toBuffer))
          dispatch(StompCodec.decode(buffer.toBuffer))
          readBuffer.compact
          return
        }
        if (!frameStart || c != Stomp.NEWLINE) {
          frameStart = false
          buffer.write(c)
        }
      }
      readBuffer.clear
    }
  }

  def start() = {
    checkConnected
  }

  def stop() = {
    if (readSource != null) {
       readSource.release
       readSource = null
    }

    if (channel != null) {
      channel.close
      channel = null
    }
  }

  def send(frame: StompFrame): Unit = {
     val buffer = StompCodec.encode(frame)
     debug("Sending:\n" +  ascii(buffer))
     channel.write(ByteBuffer.wrap(buffer.toByteArray))
  }

  def request(frame: StompFrame): StompFrame = {
    //TODO not yet implemented
    return null
  }

  def dispatch(frame: StompFrame) = {
    client.onStompFrame(frame)
  }

  def checkConnected() = {
    val expire = System.currentTimeMillis() + 5000
    while (!connected) {
      if (System.currentTimeMillis() > expire) {
        throw new IOException("Not connected")
      }
      Thread.sleep(500)
    }
  }

}