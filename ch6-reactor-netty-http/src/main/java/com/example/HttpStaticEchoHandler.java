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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import java.nio.charset.StandardCharsets;

public class HttpStaticEchoHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
    if (!req.decoderResult().isSuccess()) {
      sendError(ctx, HttpResponseStatus.BAD_REQUEST);
      return;
    }
    if (!req.method().equals(HttpMethod.GET)) {
      sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
      return;
    }
    final String uri = req.uri();
    final Req r = new Req(uri);
    final ByteBuf buffer = Unpooled.buffer();
    final int size =
        buffer.writeCharSequence(objectMapper.writeValueAsString(r), StandardCharsets.UTF_8);
    final DefaultFullHttpResponse response =
        new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
    final HttpHeaders headers =
        response
            .headers()
            .set("Content-Type", "application/json; charset=UTF-8")
            .set("Content-Size", size);
    final ChannelFutureListener listener;
    if (HttpUtil.isKeepAlive(req)) {
      headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
      listener = f -> {};
    } else {
      listener = ChannelFutureListener.CLOSE;
    }
    ctx.writeAndFlush(response).addListener(listener);
  }

  private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
    final DefaultFullHttpResponse response =
        new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            status,
            Unpooled.copiedBuffer("Failure: " + status + "\r\n", StandardCharsets.UTF_8));
    response.headers().set("Content-Type", "text/plain; charset=UTF-8");
    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
  }

  public static class Req {
    private final String requestUri;

    public Req(String requestUri) {
      this.requestUri = requestUri;
    }
  }
}
