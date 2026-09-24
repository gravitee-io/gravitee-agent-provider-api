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
package io.gravitee.agentprovider.api;

import io.gravitee.agentprovider.api.model.AgentRef;
import io.gravitee.agentprovider.api.model.AgentSnapshot;
import io.gravitee.agentprovider.api.model.Probe;
import java.util.List;

/**
 * Contract implemented by an agent provider to expose the AI agents hosted by an external provider
 * (Bedrock, Vertex AI, LangGraph, ...) so they can be added to the Gravitee catalog.
 *
 * @author GraviteeSource Team
 */
public interface AgentProviderApi {
  /**
   * Check the connectivity with the provider (credentials, network, ...).
   *
   * @return the result of the probe.
   */
  Probe test();

  /**
   * List the agents available on the provider.
   *
   * @return references (id and name) of the discovered agents.
   * @throws AgentProviderException if the provider cannot be reached.
   */
  List<AgentRef> discover();

  /**
   * Fetch the full description of the given agents.
   *
   * @param ids the identifiers of the agents to fetch (as returned by {@link #discover()}).
   * @return the snapshots of the requested agents.
   * @throws AgentProviderException if the provider cannot be reached or an id is unknown.
   */
  List<AgentSnapshot> fetch(String... ids);
}
