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
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class ReadHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ReadHandler.class);

    private CompositeByteBuf messages = Unpooled.compositeBuffer();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof ByteBuf) {
            final ByteBuf byteBuf = (ByteBuf) msg;
            logger.info("read: size = {}", ((ByteBuf) msg).readableBytes());
            messages.addComponent(true, Unpooled.copiedBuffer(byteBuf));
        } else {
            logger.info("read: type = {}", msg.getClass().getSimpleName());
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        logger.info("read complete: size = {}", messages.readableBytes());
        final String message = messages.toString(StandardCharsets.UTF_8).trim();
        messages = Unpooled.compositeBuffer();
        ctx.fireChannelRead(message);
        ctx.fireChannelReadComplete();
    }
}
