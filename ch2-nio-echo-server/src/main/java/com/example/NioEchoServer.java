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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class NioEchoServer implements Runnable, AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(NioEchoServer.class);

  public static void main(String[] args) {
    final ExecutorService executorService = Executors.newSingleThreadExecutor();
    try (final NioEchoServer server = new NioEchoServer(8000)) {
      final Future<?> future = executorService.submit(server);
      future.get();
    } catch (Exception e) {
      logger.info("error on executing service", e);
    } finally {
      executorService.shutdown();
    }
  }

  private final ExecutorService executorService = Executors.newFixedThreadPool(5);

  private final ServerSocketChannel serverSocketChannel;
  private final InetSocketAddress address;

  private NioEchoServer(final int port) throws IOException {
    this.serverSocketChannel = ServerSocketChannel.open();
    this.address = new InetSocketAddress(port);
    initialize();
  }

  private void initialize() throws IOException {
    final ServerSocket serverSocket = serverSocketChannel.socket();
    serverSocket.setReuseAddress(true);
    serverSocket.bind(address);
    serverSocketChannel.configureBlocking(false);
  }

  @Override
  public void run() {
    try {
      final Selector selector = Selector.open();
      serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
      logger.info("server start on {}", address);
      handle(selector);
    } catch (IOException e) {
      logger.warn("error on main loop", e);
    }
  }

  private void handle(final Selector selector) throws IOException {
    while (selector.select() > 0) {
      final Set<SelectionKey> keys = selector.selectedKeys();
      logger.info("key size: {}", keys.size());
      for (SelectionKey key : keys) {
        final NioEchoServerHandler handler =
            new NioEchoServerHandler(key);
        final Optional<Exception> result = handler.call();
        result.ifPresent(
            e -> logger.warn("error for key {} [{}]", key, e.getClass().getCanonicalName()));
        keys.remove(key); // op_accept と accept 以降のキーは別物かつ selectionKeys に残り続ける
      }
    }
  }

  @Override
  public void close() throws Exception {
    serverSocketChannel.close();
  }
}
