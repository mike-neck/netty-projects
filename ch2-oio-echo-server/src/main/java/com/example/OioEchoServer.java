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

import com.example.common.AllAutoCloseable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OioEchoServer implements Runnable, AutoCloseable {

  public static void main(String[] args) throws IOException {
    final ExecutorService executors = Executors.newSingleThreadExecutor();
    final ServerSocket serverSocket = new ServerSocket(8008);
    final OioEchoServer oioEchoServer = new OioEchoServer(serverSocket, 3);
    logger.info("starting server on port 8008");
    final CompletableFuture<Void> future = CompletableFuture.runAsync(oioEchoServer, executors);
    future
        .whenCompleteAsync(
            (v, error) -> {
              if (error == null) {
                logger.info("finished server");
              } else {
                logger.info("server finished unexpectedly", error);
              }
            })
        .join();
  }

  private static final Logger logger = LoggerFactory.getLogger(OioEchoServer.class);

  private final ServerSocket serverSocket;
  private final ExecutorService executor;

  private OioEchoServer(final ServerSocket serverSocket, final int thread) {
    this.serverSocket = serverSocket;
    this.executor = Executors.newFixedThreadPool(thread);
  }

  @Override
  public void run() {
    final AtomicInteger semaphore = new AtomicInteger(3);
    while (semaphore.get() > 0) {
      try {
        handle(semaphore);
      } catch (IOException e) {
        logger.warn("error in main loop", e);
        throw new UncheckedIOException(e);
      }
    }
  }

  private void handle(final AtomicInteger semaphore) throws IOException {
    final Socket clientSocket = serverSocket.accept();
    final InetAddress client = clientSocket.getInetAddress();
    final int clientPort = clientSocket.getPort();
    logger.info("new connection: {}[{}]", client, clientPort);
    final OioSocketServerHandler handler = new OioSocketServerHandler(clientSocket);
    final CompletableFuture<Void> future = CompletableFuture.runAsync(handler, executor);
    future.whenCompleteAsync(
        (v, error) -> {
          if (error == null) {
            logger.info("successfully handle: {}[{}]", client, clientPort);
          } else {
            logger.warn(
                "[{}]failure in handling: {}[{}]", semaphore.decrementAndGet(), client, clientPort);
          }
        });
  }

  @Override
  public void close() throws Exception {
    final AllAutoCloseable all = serverSocket::close;
    final AllAutoCloseable closeable = all.andThen(executor::shutdown);
    closeable.close();
  }
}
