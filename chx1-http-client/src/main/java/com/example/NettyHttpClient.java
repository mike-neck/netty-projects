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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

public class NettyHttpClient {

  private static final Logger logger = LoggerFactory.getLogger(NettyHttpClient.class);

  public static void main(String[] args) throws SSLException, InterruptedException {
    final SslContext context = SslContextBuilder.forClient().startTls(false).build();

    final CountDownLatch countDownLatch = new CountDownLatch(1);

    final EventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);
    final Bootstrap bootstrap = new Bootstrap();
    bootstrap
        .group(eventLoopGroup)
        .channel(NioSocketChannel.class)
        .handler(new HttpHandlerInitializer(context, false, countDownLatch));
    final String hostname = "api.github.com";
    final ChannelFuture channelFuture = bootstrap.connect(new InetSocketAddress(hostname, 443));
    channelFuture.addListener(
        (ChannelFutureListener)
            future -> {
              logger.info("connected remote peer.");
              final Channel channel = channelFuture.channel();
              final DefaultHttpHeaders httpHeaders = new DefaultHttpHeaders();
              httpHeaders.add("Host", hostname);
              httpHeaders.add(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
              httpHeaders.add(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
              httpHeaders.add("User-Agent", "Netty");
              httpHeaders.add("Accept", "application/json");
              final DefaultHttpRequest request =
                  new DefaultHttpRequest(
                      HttpVersion.HTTP_1_1,
                      HttpMethod.GET,
                      "/search/repositories?q=netty&sort=stars&order=desc&per_page=3",
                      httpHeaders);
              channel.writeAndFlush(request).addListener(future1 -> logger.info("request sent."));
            });
    countDownLatch.await();
    channelFuture
        .channel()
        .closeFuture()
        .addListener(
            f -> {
              logger.info("closed");
              eventLoopGroup.shutdownGracefully().addListener(gf -> logger.info("finish"));
            });
  }
}
