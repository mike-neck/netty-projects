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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

public class AsyncNioEchoServer {

  private static final Logger logger = LoggerFactory.getLogger(AsyncNioEchoServer.class);

  public static void main(String[] args) throws IOException {
    final AsynchronousServerSocketChannel serverSocketChannel =
        AsynchronousServerSocketChannel.open();
    serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
    serverSocketChannel
        .bind(new InetSocketAddress(8000))
        .accept(
            new MyHandler(),
            new CompletionHandler<AsynchronousSocketChannel, MyHandler>() {
              @Override
              public void completed(AsynchronousSocketChannel result, MyHandler attachment) {
                final CompletableFuture<Integer> readFuture = new CompletableFuture<>();
                final ByteBuffer buffer = ByteBuffer.allocate(1024);
                result.read(
                    buffer, attachment, Handler.of(result, (i, m) -> readFuture.complete(i)));
                final CompletableFuture<byte[]> bytesFuture = readFuture.thenApply(byte[]::new);
                final CompletableFuture<byte[]> bsFuture =
                    bytesFuture.thenApply(
                        bs -> {
                          buffer.flip();
                          buffer.get(bs);
                          return bs;
                        });
                final CompletableFuture<String> messageFuture =
                    bsFuture.thenApply(bs -> new String(bs, StandardCharsets.UTF_8));
                messageFuture.thenAccept(msg -> logger.info("message {}", msg));
                final CompletableFuture<Integer> writeFuture =
                    messageFuture.thenComposeAsync(
                        msg -> {
                          final String response =
                              "["
                                  + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
                                  + "] "
                                  + ""
                                  + msg;
                          final byte[] bs = response.getBytes(StandardCharsets.UTF_8);
                          final CompletableFuture<Integer> future = new CompletableFuture<>();
                          result.write(
                              ByteBuffer.wrap(bs),
                              attachment,
                              Handler.of(result, (i, a) -> future.complete(i)));
                          return future;
                        });
                writeFuture.thenAccept(
                    i -> {
                      try {
                        result.close();
                      } catch (IOException e) {
                        throw new UncheckedIOException(e);
                      }
                    });
              }

              @Override
              public void failed(Throwable exc, MyHandler attachment) {
                  logger.warn("error on accept", exc);
              }
            });
  }

  private interface Handler<T, A> {
    void completed(T result, A attachment);

    static <T, A> CompletionHandler<T, A> of(
        AsynchronousSocketChannel channel, Handler<? super T, ? super A> handler) {
      return new CompletionHandler<T, A>() {
        @Override
        public void completed(T result, A attachment) {
          handler.completed(result, attachment);
        }

        @Override
        public void failed(Throwable exc, A attachment) {
          logger.warn("error", exc);
          try {
            channel.close();
          } catch (IOException e) {
            final UncheckedIOException ex = new UncheckedIOException(e);
            ex.addSuppressed(exc);
            throw ex;
          }
        }
      };
    }
  }

  private static class MyHandler {}
}
