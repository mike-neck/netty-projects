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
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogHandler extends ChannelInboundHandlerAdapter {

  private static final Logger logger = LoggerFactory.getLogger(LogHandler.class);

  private final String previous;
  private final String following;

  public LogHandler(String previous, String following) {
    this.previous = previous;
    this.following = following;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    final String debug =
        msg instanceof ByteBuf ? ByteBufUtil.hexDump((ByteBuf) msg) : msg.toString();
    logger.info("previous: {}/following: {}, object: {}", previous, following, debug);
    ctx.fireChannelRead(msg);
  }
}
