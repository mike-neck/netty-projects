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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

class ClientHandler {

    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    static class ForResponse extends ChannelInboundHandlerAdapter {

        private final CountDownLatch countDownLatch;

        ForResponse(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            logger.info("response returned.");
            if (msg instanceof FullHttpResponse) {
                final FullHttpResponse httpResponse = (FullHttpResponse) msg;
                logger.info("response type: {}", httpResponse.getClass().getCanonicalName());
                logger.info("http object: {}", httpResponse);
                final String response = httpResponse.content().toString(StandardCharsets.UTF_8);
                logger.info("response body: {}", response);
                countDownLatch.countDown();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.warn("exception caught", cause);
            final Channel channel = ctx.channel();
            if (channel.isOpen()) {
                channel.closeFuture().addListener(f -> {
                    logger.info("closed");
                    countDownLatch.countDown();
                });
            } else {
                logger.info("already closed");
                countDownLatch.countDown();
            }
        }
    }

    static class ForRequest extends ChannelOutboundHandlerAdapter {}
}
