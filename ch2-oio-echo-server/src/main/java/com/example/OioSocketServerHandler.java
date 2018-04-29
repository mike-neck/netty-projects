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
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OioSocketServerHandler implements Runnable, AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(OioSocketServerHandler.class);

  private final Socket clientSocket;

  OioSocketServerHandler(final Socket clientSocket) {
    this.clientSocket = clientSocket;
  }

  @Override
  public void run() {
    try {
      handle();
    } catch (IOException e) {
      logger.warn("error: ", e);
    }
  }

  private void handle() throws IOException {
    final BufferedReader input =
        new BufferedReader(
            new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
    final PrintWriter output =
        new PrintWriter(
            new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);

    final InetAddress address = clientSocket.getInetAddress();
    final int port = clientSocket.getPort();

    try (final AllAutoCloseable ignored = AllAutoCloseable.of(input, output)) {
      String read;
      while ((read = input.readLine()) != null && !read.isEmpty()) {
        logger.info("receive from {}[{}]: {}", address, port, read);
        output.println(String.format("[%s] %s", LocalDateTime.now(), read));
      }
    }
  }

  @Override
  public void close() throws Exception {
    clientSocket.close();
  }
}
