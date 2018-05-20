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
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class NettyLimitedEchoServer {

  private static final Logger logger = LoggerFactory.getLogger(NettyLimitedEchoServer.class);

  public static void main(String[] args) {
    final NioEventLoopGroup parent = new NioEventLoopGroup();
    final NioEventLoopGroup child = new NioEventLoopGroup(1);
    final ServerBootstrap bootstrap = new ServerBootstrap();
    try (final AllAutoCloseable ignored =
        AllAutoCloseable.of(child::shutdownGracefully, parent::shutdownGracefully)) {
      bootstrap
          .group(parent, child)
          .channel(NioServerSocketChannel.class)
          .localAddress(8000)
          .childOption(ChannelOption.SO_RCVBUF, 128)
          .childHandler(
              new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                  ch.pipeline()
                      .addLast(new ReadHandler(), new LogMessageHandler(), new LoggingHandler());
                }
              });
      final ChannelFuture channelFuture =
          bootstrap.bind().sync().addListener(v -> logger.info("server started on 8000"));
      channelFuture.channel().closeFuture().sync();
    } catch (InterruptedException | IOException e) {
      logger.warn("error on shutting down the server", e);
    }
  }
}
