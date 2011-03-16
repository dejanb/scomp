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

import java.io.IOException
import Stomp._
import org.fusesource.hawtbuf.{AsciiBuffer, Buffer, ByteArrayOutputStream}
import collection.mutable.ListBuffer

object StompCodec {
  val READ_BUFFFER_SIZE = 1024 * 64;
  val MAX_COMMAND_LENGTH = 1024;
  val MAX_HEADER_LENGTH = 1024 * 10;
  val MAX_HEADERS = 1000;
  val MAX_DATA_LENGTH = 1024 * 1024 * 100;
  val TRIM = true
  val SIZE_CHECK = false


  def encode(frame: StompFrame): Buffer = {

    val os = new ByteArrayOutputStream(frame.size)

    frame.action.writeTo(os)
    os.write(NEWLINE)

    for ((key, value) <- frame.headers) {
      key.writeTo(os)
      os.write(COLON)
      value.writeTo(os)
      os.write(NEWLINE)
    }
    os.write(NEWLINE)
    frame.content.writeTo(os)
    os.write(NULL)
    os.write(NEWLINE)

    os.toBuffer
  }

  def decode(buffer: Buffer): StompFrame = {

    def read_line = {
      val pos = buffer.indexOf('\n'.toByte)
      if (pos < 0) {
        throw new IOException("expected a new line")
      } else {
        val rc = buffer.slice(0, pos).ascii
        buffer.offset += (pos + 1)
        buffer.length -= (pos + 1)
        rc
      }
    }


    val action = if (TRIM) {
      read_line.trim().ascii
    } else {
      read_line
    }

    var headers = new ListBuffer[(AsciiBuffer, AsciiBuffer)]

    var line = read_line
    while (line.length() > 0) {
      try {
        val seperatorIndex = line.indexOf(COLON)
        if (seperatorIndex < 0) {
          throw new IOException("Header line missing seperator.")
        }
        var name = line.slice(0, seperatorIndex).ascii
        if (TRIM) {
          name = name.trim().ascii
        }
        var value = line.slice(seperatorIndex + 1, line.length()).ascii
        if (TRIM) {
          value = value.trim().ascii
        }
        val entry = (name, value)
        headers += entry
      } catch {
        case e: Exception =>
          e.printStackTrace
          throw new IOException("Unable to parser header line [" + line + "]")
      }
      line = read_line
    }

    new StompFrame(action, headers.toList, BufferContent(buffer))
  }

}