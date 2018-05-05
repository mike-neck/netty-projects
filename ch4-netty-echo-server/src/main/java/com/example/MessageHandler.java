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
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public class MessageHandler extends ChannelInboundHandlerAdapter {

  private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    final String message = (String) msg;
    logger.info("MessageHandler receive message {}", message);

    final String returnMessage = String.format("[%s] %s", LocalDateTime.now(), message);

    final ByteBuf buffer = ctx.alloc().buffer();
    buffer.writeCharSequence(returnMessage, StandardCharsets.UTF_8);

    ctx.writeAndFlush(buffer)
        .addListener(
            future -> {
              if (future.isSuccess()) {
                logger.info("successfully returns message: {}", returnMessage);
              } else if (future.isCancelled()) {
                logger.info("returning message is cancelled");
              } else {
                logger.info("fail to return message", future.cause());
              }
            });
  }

  @Override
  public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
    logger.info("channelRegistered");
    super.channelRegistered(ctx);
  }

  @Override
  public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
    logger.info("channelUnregistered");
    super.channelUnregistered(ctx);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    logger.info("channelActive");
    super.channelActive(ctx);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    logger.info("channelInactive");
    super.channelInactive(ctx);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    logger.info("channelReadComplete");
    super.channelReadComplete(ctx);
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    logger.info("handlerAdded");
    super.handlerAdded(ctx);
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    logger.info("handlerRemoved");
    super.handlerRemoved(ctx);
  }
}
