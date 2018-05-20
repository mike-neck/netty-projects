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

import java.time.LocalDateTime;
import java.time.ZoneId;

public class LogMessage {

  private final LocalDateTime received;
  private final String original;

  public LogMessage(String original) {
    this.original = original;
    this.received = LocalDateTime.now(ZoneId.of("UTC"));
  }

  @Override
  public String toString() {
    @SuppressWarnings("StringBufferReplaceableByString")
    final StringBuilder sb = new StringBuilder("LogMessage{");
    sb.append("received=").append(received);
    sb.append(", original='").append(original).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
