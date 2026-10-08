/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.agentprovider.api.model;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A page of agents returned by discovery.
 *
 * @param items The agents of this page. Never {@code null}, empty if none
 * @param next The cursor to pass to get the next page, or {@code null} if this is the last page
 * @author GraviteeSource Team
 */
public record DiscoveryPage(List<AgentRef> items, @Nullable String next) {
  public DiscoveryPage {
    items = items == null ? List.of() : items;
  }
}
