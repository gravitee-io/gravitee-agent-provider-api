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
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Full description of an agent as exposed by a provider.
 *
 * @param id The identifier of the agent in the provider
 * @param name The name of the agent
 * @param description The description of the agent, if any
 * @param version The version of the agent if the provider supports versioning
 * @param state The state of the agent normalized to Gravitee vocabulary, or {@code AgentState#UNKNOWN} if the provider does not expose it
 * @param rawState The state of the agent if such info exists for the provider. Follow the provider vocabulary
 * @param rawPublicationState The publication state if such info exists for the provider. Follow the provider vocabulary
 * @param instructions The instructions (system prompt) given to the agent, if exposed by the provider
 * @param capabilities The lifecycle operations the provider supports for this agent (e.g. start, stop). Never {@code null}, empty if unknown
 * @param tools The tools available to the agent. Never {@code null}, empty if none
 * @param model The model used by the agent, if exposed by the provider
 * @param entrypoints The entrypoints through which the agent can be invoked. Never {@code null}, empty if none
 * @param a2aCard The raw A2A agent card (JSON) if the agent exposes one
 * @param metadata Additional provider-specific information about the agent. Never {@code null}, empty if none
 * @author GraviteeSource Team
 */
public record AgentSnapshot(
  String id,
  String name,
  @Nullable String description,
  @Nullable String version,
  @Nullable AgentState state,
  @Nullable String rawState,
  @Nullable String rawPublicationState,
  @Nullable String instructions,
  List<Capability> capabilities,
  List<AgentTool> tools,
  @Nullable AgentModel model,
  List<AgentEntrypoint> entrypoints,
  @Nullable String a2aCard,
  Map<String, String> metadata
) {
  public AgentSnapshot {
    state = state == null ? AgentState.UNKNOWN : state;
    tools = tools == null ? List.of() : tools;
    entrypoints = entrypoints == null ? List.of() : entrypoints;
    capabilities = capabilities == null ? List.of() : capabilities;
    metadata = metadata == null ? Map.of() : metadata;
  }
}
