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

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;
import java.util.concurrent.CountDownLatch;

public class HttpHandlerInitializer extends ChannelInitializer<Channel> {

  private final SslContext context;
  private final boolean startTls;
  private final CountDownLatch countDownLatch;

  HttpHandlerInitializer(SslContext context, boolean startTls, CountDownLatch countDownLatch) {
    this.context = context;
    this.startTls = startTls;
    this.countDownLatch = countDownLatch;
  }

  private SslHandler sslEngine(final Channel channel) {
    final SSLEngine sslEngine = context.newEngine(channel.alloc());
    return new SslHandler(sslEngine, startTls);
  }

  @Override
  protected void initChannel(Channel ch) {
    final SslHandler sslHandler = sslEngine(ch);
    ch.pipeline()
        .addFirst("ssl", sslHandler)
        .addLast("ssl to http-codec", new LogHandler("ssl", "http-codec"))
        .addLast("http-codec", new HttpClientCodec())
        .addLast("http-codec to decompress", new LogHandler("http-codec", "decompress"))
        .addLast("decompress", new HttpContentDecompressor())
        .addLast("decompress to aggregate", new LogHandler("decompress", "aggregate"))
        .addLast("aggregate", new HttpObjectAggregator(1048576))
        .addLast("aggregate to client", new LogHandler("aggregate", "client"))
        .addLast("client", new ClientHandler.ForResponse(countDownLatch))
        .addLast("client to req", new LogHandler("client", "req"))
        .addLast("req", new ClientHandler.ForRequest());
  }
}
