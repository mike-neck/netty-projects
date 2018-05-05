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
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class NettyEchoServer implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(NettyEchoServer.class);

  private final int port;

  public NettyEchoServer(int port) {
    this.port = port;
  }

  public static void main(String[] args) {
    new NettyEchoServer(8000).run();
  }

  @Override
  public void run() {
    final NioEventLoopGroup serverChannelLoop = new NioEventLoopGroup();
    final NioEventLoopGroup channelHandlerLoop = new NioEventLoopGroup();

    final ReceptionHandler receptionHandler = new ReceptionHandler();
    final MessageHandler messageHandler = new MessageHandler();

    try (final AllAutoCloseable ignored =
        ((AllAutoCloseable) (channelHandlerLoop::shutdownGracefully))
            .andThen(serverChannelLoop::shutdownGracefully)) {
      final ServerBootstrap serverBootstrap =
          new ServerBootstrap()
              .group(serverChannelLoop, channelHandlerLoop)
              .channel(NioServerSocketChannel.class)
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
