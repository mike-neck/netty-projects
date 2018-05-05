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
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public class ReceptionHandler extends ChannelInboundHandlerAdapter {

  private static final Logger logger = LoggerFactory.getLogger(ReceptionHandler.class);

  private static final byte[] CLOSE_SIGNAL = {
    (byte) 0xff, (byte) 0xf4, (byte) 0xff, (byte) 0xfd, (byte) 0x06
  };

  private static final ByteBuf CLOSE_MESSAGE = Unpooled.buffer();

  static {
    CLOSE_MESSAGE.writeCharSequence("Byte.", StandardCharsets.UTF_8);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    logger.info("channelRead");
    final ByteBuf byteBuf = (ByteBuf) msg;
    final int size = byteBuf.readableBytes();
    final byte[] bytes = new byte[size];
    byteBuf.readBytes(bytes);
    if (Arrays.equals(CLOSE_SIGNAL, bytes)) {
      ctx.close();
    } else {
      final String message = new String(bytes, StandardCharsets.UTF_8);
      logger.info("message: {}", message);
      ctx.fireChannelRead(message);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    logger.warn("error on handling inbound message", cause);
    super.exceptionCaught(ctx, cause);
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
