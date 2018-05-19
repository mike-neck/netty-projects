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
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AnotherNioEchoServer implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(AnotherNioEchoServer.class);

  public static void main(String[] args) throws ExecutionException, InterruptedException {
    final AnotherNioEchoServer anotherNioEchoServer = new AnotherNioEchoServer(8000);
    final ExecutorService executorService = Executors.newFixedThreadPool(2);
    final Future<?> subTask = executorService.submit(anotherNioEchoServer.subLoop);
    final Future<?> mainTsk = executorService.submit(anotherNioEchoServer);
    mainTsk.get();
  }

  private final ServerSocketChannel serverSocketChannel;
  private final int port;
  private final SubLoop subLoop;

  private AnotherNioEchoServer(int port) {
    try {
      this.port = port;
      this.serverSocketChannel = ServerSocketChannel.open();
      this.subLoop = new SubLoop();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void run() {
    try {
      final ServerSocket serverSocket = serverSocketChannel.socket();
      serverSocket.setReuseAddress(true);
      final InetSocketAddress address = new InetSocketAddress(port);
      serverSocketChannel.bind(address);
      serverSocketChannel.configureBlocking(false);
      final Selector mainSelector = Selector.open();
      serverSocketChannel.register(mainSelector, SelectionKey.OP_ACCEPT);
      logger.info("server started at port 8000");
      while (true) {
        mainSelector.select(50L);
        final Set<SelectionKey> selectionKeys = mainSelector.selectedKeys();
        for (SelectionKey key : selectionKeys) {
          selectionKeys.remove(key);
          if (!key.isAcceptable()) continue;
          logger.info("coming new connection.");
          final ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
          final SocketChannel channel = serverSocketChannel.accept();
          channel.configureBlocking(false);
          channel.register(subLoop.selector, SelectionKey.OP_READ, ByteBuffer.allocate(8192));
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static class SubLoop implements Runnable {
    final Selector selector;

    SubLoop() throws IOException {
      this.selector = Selector.open();
    }

    @Override
    public void run() {
      logger.info("start sub loop");
      while (true) {
        try {
          selector.select(50L);
          final Set<SelectionKey> selectionKeys = selector.selectedKeys();
          for (SelectionKey key : selectionKeys) {
            selectionKeys.remove(key);
            final ByteBuffer byteBuffer = (ByteBuffer) key.attachment();
            final SocketChannel channel = (SocketChannel) key.channel();
            if (key.isReadable()) {
              final int size = channel.read(byteBuffer);
              byteBuffer.flip();
              final byte[] bytes = new byte[size];
              byteBuffer.get(bytes);
              final String message = new String(bytes, StandardCharsets.UTF_8);
              logger.info("receive message: {}", message);
              byteBuffer.clear();
              final String result = String.format("[%s] %s", LocalDateTime.now(), message);
              byteBuffer.put(result.getBytes(StandardCharsets.UTF_8));
              key.interestOps(SelectionKey.OP_WRITE);
            } else if (key.isWritable()) {
              byteBuffer.flip();
              channel.write(byteBuffer);
              byteBuffer.compact();
              if (byteBuffer.position() > 0) {
                key.interestOps(SelectionKey.OP_WRITE);
              } else {
                byteBuffer.clear();
                key.interestOps(SelectionKey.OP_READ);
              }
            }
          }
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
    }
  }
}
