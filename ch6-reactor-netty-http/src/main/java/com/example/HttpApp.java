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

import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.server.HttpServer;
import reactor.ipc.netty.tcp.BlockingNettyContext;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Scanner;
import java.util.StringJoiner;

public class HttpApp {

  private static final Logger logger = LoggerFactory.getLogger(HttpApp.class);

  public static void main(String[] args) {
    final HttpServer httpServer = HttpServer.create(8100);
    final BlockingNettyContext blockingNettyContext =
        httpServer.startRouter(
            httpServerRoutes ->
                httpServerRoutes.get(
                    "/hello",
                    (req, res) -> {
                      final String headers =
                          Lists.immutable
                              .ofAll(req.requestHeaders().entries())
                              .collect(Tuples::pairFrom)
                              .collect(p -> String.format("%s : %s", p.getOne(), p.getTwo()))
                              .injectInto(new StringJoiner("\n"), StringJoiner::add)
                              .toString();
                      logger.info("Header\n{}", headers);
                      final String message = req.param("message");
                      logger.info("[/hello]message: {}", message);
                      final Message msg = new Message(message);
                      return res.header("Content-Type", "text/plain")
                          .compression(true)
                          .sendString(
                              Mono.just(msg).map(Message::toString), StandardCharsets.UTF_8);
                    }));
    logger.info("Server started on port 8100");
    new Scanner(System.in).nextLine();
    blockingNettyContext.shutdown();
  }

  static class Message {
    final String message;
    final OffsetDateTime time;

    Message(String message) {
      this.message = message;
      this.time = OffsetDateTime.now();
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("Message{");
      sb.append("message='").append(message).append('\'');
      sb.append(", time=").append(time);
      sb.append('}');
      return sb.toString();
    }
  }
}
