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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class LogMessageHandler extends ChannelOutboundHandlerAdapter {

  private static final Logger logger = LoggerFactory.getLogger(LogMessageHandler.class);

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
    if (!(msg instanceof LogMessage)) ctx.write(msg, promise);
    @SuppressWarnings("ConstantConditions")
    final LogMessage logMessage = (LogMessage) msg;
    final byte[] bytes = (logMessage.toString() + "\n").getBytes(StandardCharsets.UTF_8);
    final ByteBuf byteBuf = ctx.alloc().buffer(bytes.length);
    byteBuf.writeBytes(bytes);
    ctx.write(byteBuf, promise);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    logger.info("error handling message", cause);
    ctx.writeAndFlush(Unpooled.directBuffer().setCharSequence(0, "error", StandardCharsets.UTF_8))
        .addListener(v -> ctx.close());
  }
}
