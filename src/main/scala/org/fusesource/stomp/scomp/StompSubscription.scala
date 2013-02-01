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

import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}
import org.apache.activemq.apollo.stomp.StompFrame
import org.eintr.loglady.Logging
import scala.Option

trait FrameListener extends Logging {
  val queue = new LinkedBlockingQueue[StompFrame]()
  var listener: Option[(StompFrame) => Unit] = None

  def addListener(l: Option[(StompFrame) => Unit]) {
    listener = l
  }

  def onStompFrame(frame: StompFrame) = {
    if (listener.isDefined) {
      listener.get.apply(frame)
    }
    else {
      queue.offer(frame)
    }
  }

  def receive(timeout: Int = -1): StompFrame = {
    if (timeout < 0) {
      queue.take
    } else {
      queue.poll(timeout, TimeUnit.MILLISECONDS)
    }
  }
}

class StompSubscription(val id: String) extends FrameListener

