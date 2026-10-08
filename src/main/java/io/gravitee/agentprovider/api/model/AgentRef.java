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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Lightweight description of an agent returned by discovery.
 *
 * @param id The identifier of the agent in the provider
 * @param name The name of the agent
 * @param description The description of the agent, if any
 * @param platform The platform hosting the agent (e.g. Bedrock, Vertex AI), if known
 * @param framework The framework the agent is built with (e.g. LangGraph, ADK), if known
 * @param workload The workload running the agent, if exposed by the provider
 * @param models The names of the models used by the agent. Never {@code null}, empty if unknown
 * @param tools The names of the tools available to the agent. Never {@code null}, empty if none
 * @param lastActivity The last time the agent was active, if exposed by the provider
 * @param metadata Additional provider-specific information about the agent. Never {@code null}, empty if none
 *
 * @author GraviteeSource Team
 */
public record AgentRef(
  String id,
  String name,
  @Nullable String description,
  @Nullable String platform,
  @Nullable String framework,
  @Nullable String workload,
  List<String> models,
  List<String> tools,
  @Nullable Instant lastActivity,
  Map<String, String> metadata
) {
  public AgentRef {
    models = models == null ? List.of() : models;
    tools = tools == null ? List.of() : tools;
    metadata = metadata == null ? Map.of() : metadata;
  }

  /**
   * Creates a reference carrying only the identifier and the name of the agent.
   *
   * @param id The identifier of the agent in the provider
   * @param name The name of the agent
   */
  public AgentRef(String id, String name) {
    this(id, name, null, null, null, null, null, null, null, null);
  }
}
