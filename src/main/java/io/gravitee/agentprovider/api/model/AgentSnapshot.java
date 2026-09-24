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
 * Full description of an agent as exposed by a provider.
 *
 * @author GraviteeSource Team
 */
public record AgentSnapshot(
  String id,
  String name,
  @Nullable String description,
  @Nullable String version,
  @Nullable String state,
  @Nullable String publicationState,
  @Nullable String instructions,
  @Nullable List<Capability> capabilities,
  List<AgentTool> tools,
  @Nullable AgentModel model,
  List<AgentEntrypoint> entrypoints,
  @Nullable String a2aCard
) {
  public AgentSnapshot {
    tools = tools == null ? List.of() : List.copyOf(tools);
    entrypoints = entrypoints == null ? List.of() : List.copyOf(entrypoints);
  }
}
