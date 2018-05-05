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

import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NioEchoServerHandler
    implements Callable<Optional<Exception>>, Supplier<Optional<Exception>> {

  private static final Logger logger = LoggerFactory.getLogger(NioEchoServer.class);

  private final SelectionKey key;
  private final SelectableChannel selectableChannel;

  NioEchoServerHandler(SelectionKey key) {
    this.key = key;
    this.selectableChannel = key.channel();
  }

  private SocketChannel socketChannel() {
    return ((SocketChannel) selectableChannel);
  }

  private ServerSocketChannel serverSocketChannel() {
    return (ServerSocketChannel) selectableChannel;
  }

  @Override
  public Optional<Exception> get() {
    return call();
  }

  @Override
  public Optional<Exception> call() {
    try {
      handle();
      return Optional.empty();
    } catch (IOException e) {
      logger.warn("error", e);
      return Optional.of(e);
    }
  }

  private void handle() throws IOException {
    logger.info("handling key[{}]: {}", this, key);
    if (key.isAcceptable()) {
      try {
        final SocketChannel channel = serverSocketChannel().accept();
        channel.configureBlocking(false);

        final SocketAddress remoteAddress = channel.getRemoteAddress();
        logger.info("OP_ACCEPT[{}]: {}", this, remoteAddress);

        final ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        channel.register(key.selector(), SelectionKey.OP_READ, byteBuffer);
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else if (key.isReadable()) {
      final SocketChannel channel = socketChannel();

      final SocketAddress address = channel.getRemoteAddress();
      logger.info("OP_READ[{}]: {}", this, address);

      final ByteBuffer byteBuffer = (ByteBuffer) key.attachment();
      final int read = channel.read(byteBuffer);

      byteBuffer.flip();
      final byte[] bytes = new byte[read];
      byteBuffer.get(bytes);
      byteBuffer.compact();

      if (isCloseSignal(bytes)) {
        channel.close();
        return;
      }

      final String message = new String(bytes, StandardCharsets.UTF_8);
      final int size = 5 < bytes.length ? 5 : bytes.length;
      final String first3Bytes =
          IntStream.range(0, size)
              .mapToObj(i -> String.format("%02x", bytes[i]))
              .collect(joining());
      logger.info("message coming: {}", first3Bytes);
      logger.info("message[{}]: {}", this, message);
      byteBuffer.clear();

      final String returnValue = String.format("[%s]message: %s", LocalDateTime.now(), message);
      byteBuffer.put(returnValue.getBytes(StandardCharsets.UTF_8));

      key.interestOps(SelectionKey.OP_WRITE);
    } else if (key.isWritable()) {
      final SocketChannel channel = socketChannel();

      final SocketAddress address = channel.getRemoteAddress();
      logger.info("OP_WRITE[{}]: {}", this, address);

      final ByteBuffer byteBuffer = (ByteBuffer) key.attachment();
      byteBuffer.flip();
      channel.write(byteBuffer);
      byteBuffer.compact();
      if (byteBuffer.position() > 0) {
        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
      } else {
        key.interestOps(SelectionKey.OP_READ);
      }
    }
  }

  private static final byte[] CLOSE_SIGNAL = {
    (byte) 0xff, (byte) 0xf4, (byte) 0xff, (byte) 0xfd, (byte) 0x06
  };

  private static boolean isCloseSignal(final byte[] bytes) {
    return Arrays.equals(CLOSE_SIGNAL, bytes);
  }
}
