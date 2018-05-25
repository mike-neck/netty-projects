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

import io.netty.channel.nio.NioEventLoopGroup;

import java.util.concurrent.CountDownLatch;

public class RunOnEventLoop {

  public static void main(String[] args) throws InterruptedException {
      final CountDownLatch latch = new CountDownLatch(1);
      final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);
    eventLoopGroup.submit(
        () -> {
          latch.countDown();
          System.out.println("done task");
        });
      latch.await();
      eventLoopGroup.shutdownGracefully().addListener(f -> System.out.println("finished"));
  }
}
