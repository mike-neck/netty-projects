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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyEchoServer implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(NettyEchoServer.class);

  private final int port;
  private final TransportProvider transportProvider;

  NettyEchoServer(int port, TransportProvider transportProvider) {
    this.port = port;
    this.transportProvider = transportProvider;
  }

  @Override
  public void run() {
    final ReceptionHandler receptionHandler = new ReceptionHandler();
    final MessageHandler messageHandler = new MessageHandler();

    try (final EventLoopGroups groups = transportProvider.eventLoopGroups()) {
      final ServerBootstrap serverBootstrap =
          new ServerBootstrap()
              .group(groups.serverChannelLoop(), groups.childChannelLoop())
              .channel(transportProvider.serverSocketChannel())
              .localAddress(port)
              .childHandler(
                  new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                      ch.pipeline()
                          .addLast("reception", receptionHandler)
                          .addLast("message", messageHandler);
                    }
                  });
      final ChannelFuture channelFuture =
          serverBootstrap
              .bind()
              .addListener(future -> logger.info("server started on port {}", port))
              .sync();
      channelFuture.channel().closeFuture().sync();
    } catch (IOException | InterruptedException e) {
      logger.warn("error on sync", e);
    }
  }
}
