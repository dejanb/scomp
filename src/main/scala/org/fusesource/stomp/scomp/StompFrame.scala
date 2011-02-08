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

import org.fusesource.hawtbuf.{UTF8Buffer, AsciiBuffer, Buffer}
import java.io.OutputStream
import Stomp._
import Buffer._


case class StompFrame(action:AsciiBuffer, headers:HeaderMap=Nil, content:StompContent=NilContent) {

  def size:Int = {

    var headerSize = 0
    for ((key, value) <- headers) {
        headerSize += key.length + value.length
    }

    action.length + 1 + headerSize + 1 + content.length
  }

  def send(out:OutputStream) {
     println("sending " + ascii(StompCodec.encode(this)))
     StompCodec.encode(this).writeTo(out)
     out.flush
  }

  def getHeader(name:AsciiBuffer):Option[AsciiBuffer] = {
    val i = headers.iterator
    while( i.hasNext ) {
      val entry = i.next
      if( entry._1 == name ) {
        return Some(entry._2)
      }
    }
    None
  }

}

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
trait StompContent {
  def length:Int

  def isEmpty = length == 0

  def writeTo(os:OutputStream)

  def utf8:UTF8Buffer

  def retain = {}
  def release = {}
}

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object NilContent extends StompContent {
  def length = 0
  def writeTo(os:OutputStream) = {}
  val utf8 = new UTF8Buffer("")
}

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
case class BufferContent(content:Buffer) extends StompContent {
  def length = content.length
  def writeTo(os:OutputStream) = content.writeTo(os)
  def utf8:UTF8Buffer = content.utf8
}

object Stomp {

  ///////////////////////////////////////////////////////////////////
  // Framing
  ///////////////////////////////////////////////////////////////////

  val EMPTY_BUFFER = new Buffer(0)
  val NULL: Byte = 0
  val NULL_BUFFER = new Buffer(Array(NULL))
  val NEWLINE: Byte = '\n'
  val COMMA: Byte = ','
  val NEWLINE_BUFFER = new Buffer(Array(NEWLINE))
  val END_OF_FRAME_BUFFER = new Buffer(Array(NULL, NEWLINE))
  val COLON: Byte = ':'
  val COLON_BUFFER = new Buffer(Array(COLON))
  val ESCAPE:Byte = '\\'

  val ESCAPE_ESCAPE_SEQ = ascii("""\\""")
  val COLON_ESCAPE_SEQ = ascii("""\c""")
  val NEWLINE_ESCAPE_SEQ = ascii("""\n""")

  type HeaderMap = List[(AsciiBuffer, AsciiBuffer)]


  ///////////////////////////////////////////////////////////////////
  // Frame Commands
  ///////////////////////////////////////////////////////////////////
  val STOMP = ascii("STOMP")
  val CONNECT = ascii("CONNECT")
  val SEND = ascii("SEND")
  val DISCONNECT = ascii("DISCONNECT")
  val SUBSCRIBE = ascii("SUBSCRIBE")
  val UNSUBSCRIBE = ascii("UNSUBSCRIBE")

  val BEGIN_TRANSACTION = ascii("BEGIN")
  val COMMIT_TRANSACTION = ascii("COMMIT")
  val ABORT_TRANSACTION = ascii("ABORT")
  val BEGIN = ascii("BEGIN")
  val COMMIT = ascii("COMMIT")
  val ABORT = ascii("ABORT")
  val ACK = ascii("ACK")
  val NACK = ascii("NACK")

  ///////////////////////////////////////////////////////////////////
  // Frame Responses
  ///////////////////////////////////////////////////////////////////
  val CONNECTED = ascii("CONNECTED")
  val ERROR = ascii("ERROR")
  val MESSAGE = ascii("MESSAGE")
  val RECEIPT = ascii("RECEIPT")

  ///////////////////////////////////////////////////////////////////
  // Frame Headers
  ///////////////////////////////////////////////////////////////////
  val RECEIPT_REQUESTED = ascii("receipt")
  val TRANSACTION = ascii("transaction")
  val CONTENT_LENGTH = ascii("content-length")
  val CONTENT_TYPE = ascii("content-type")
  val TRANSFORMATION = ascii("transformation")
  val TRANSFORMATION_ERROR = ascii("transformation-error")

  val RECEIPT_ID = ascii("receipt-id")

  val DESTINATION = ascii("destination")
  val CORRELATION_ID = ascii("correlation-id")
  val REPLY_TO = ascii("reply-to")
  val EXPIRATION_TIME = ascii("expires")
  val PRIORITY = ascii("priority")
  val TYPE = ascii("type")
  val PERSISTENT = ascii("persistent")

  val MESSAGE_ID = ascii("message-id")
  val PRORITY = ascii("priority")
  val REDELIVERED = ascii("redelivered")
  val TIMESTAMP = ascii("timestamp")
  val SUBSCRIPTION = ascii("subscription")

  val ACK_MODE = ascii("ack")
  val ID = ascii("id")
  val SELECTOR = ascii("selector")

  val LOGIN = ascii("login")
  val PASSCODE = ascii("passcode")
  val CLIENT_ID = ascii("client-id")
  val REQUEST_ID = ascii("request-id")
  val ACCEPT_VERSION = ascii("accept-version")
  val HOST = ascii("host")
  val HEART_BEAT = ascii("heart-beat")

  val MESSAGE_HEADER = ascii("message")
  val VERSION = ascii("version")
  val SESSION = ascii("session")
  val RESPONSE_ID = ascii("response-id")

  val BROWSER = ascii("browser")
  val EXCLUSIVE = ascii("exclusive")
  val USER_ID = ascii("user-id")

  ///////////////////////////////////////////////////////////////////
  // Common Values
  ///////////////////////////////////////////////////////////////////
  val TRUE = ascii("true")
  val FALSE = ascii("false")
  val END = ascii("end")

  val ACK_MODE_AUTO = ascii("auto")
  val ACK_MODE_NONE = ascii("none")

  val ACK_MODE_CLIENT = ascii("client")
  val ACK_MODE_SESSION = ascii("session")

  val ACK_MODE_MESSAGE = ascii("message")

  val V1_0 = ascii("1.0")
  val V1_1 = ascii("1.1")
  val DEFAULT_HEART_BEAT = ascii("0,0")
  val DEFAULT_SESSION_ID = ascii("-1")

  val SUPPORTED_PROTOCOL_VERSIONS = List(V1_1, V1_0)

  val TEXT_PLAIN = ascii("text/plain")

}