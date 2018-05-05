/*
 * Copyright 2018 Shinya Mochida
 *
 * Licensed under the Apache License,Version2.0(the"License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,software
 * Distributed under the License is distributed on an"AS IS"BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example;

import com.example.common.AllAutoCloseable;
import io.netty.channel.EventLoopGroup;
import java.io.Closeable;
import java.io.IOException;

public class EventLoopGroups implements Closeable {

  private final EventLoopGroup parent;
  private final EventLoopGroup child;
  private final AllAutoCloseable gracefulShutdown;

  EventLoopGroups(EventLoopGroup parent, EventLoopGroup child) {
    this.parent = parent;
    this.child = child;
    final AllAutoCloseable shutdownChild = child::shutdownGracefully;
    this.gracefulShutdown = shutdownChild.andThen(parent::shutdownGracefully);
  }

  EventLoopGroup serverChannelLoop() {
    return parent;
  }

  EventLoopGroup childChannelLoop() {
    return child;
  }

  @Override
  public void close() throws IOException {
    gracefulShutdown.close();
  }
}
