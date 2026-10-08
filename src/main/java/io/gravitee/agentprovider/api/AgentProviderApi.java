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

import io.gravitee.agentprovider.api.model.AgentSnapshot;
import io.gravitee.agentprovider.api.model.Capability;
import io.gravitee.agentprovider.api.model.DiscoveryPage;
import io.gravitee.agentprovider.api.model.Probe;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Contract implemented by an agent provider to expose the AI agents hosted by an external provider
 * (Bedrock, Vertex AI, LangGraph, ...) so they can be added to the Gravitee catalog.
 *
 * <p>Lifecycle operations ({@link #start(String)}, {@link #stop(String)}) are optional: a provider implements them only
 * if its platform supports them, and advertises it per agent through {@link AgentSnapshot#capabilities()}.
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
   * List the agents available on the provider, one page at a time.
   *
   * <p>The first page is requested with a {@code null} cursor. The following pages are requested with the
   * {@link DiscoveryPage#next()} cursor of the previous page, until it is {@code null}.
   *
   * @param cursor the cursor returned by the previous page, or {@code null} to get the first page.
   * @param pageSize the maximum number of agents to return. The provider may return fewer.
   * @return a page of references to the discovered agents.
   * @throws AgentProviderException if the provider cannot be reached or the cursor is invalid.
   */
  DiscoveryPage discover(@Nullable String cursor, int pageSize);

  /**
   * Fetch the full description of the given agents.
   *
   * @param ids the identifiers of the agents to fetch (as returned by {@link #discover(String, int)}). Must not be
   *     {@code null} nor empty.
   * @return the snapshots of the requested agents.
   * @throws IllegalArgumentException if {@code ids} is {@code null} or empty.
   * @throws AgentProviderException if the provider cannot be reached or an id is unknown.
   */
  List<AgentSnapshot> fetch(String... ids);

  /**
   * Start the given agent on the provider.
   *
   * <p>Only called when the snapshot of the agent advertises {@link Capability#START}. Providers that do not support it
   * keep the default implementation.
   *
   * @param id the identifier of the agent to start (as returned by {@link #discover(String, int)}).
   * @throws UnsupportedOperationException if the provider does not support starting agents.
   * @throws AgentProviderException if the provider cannot be reached or the id is unknown.
   */
  default void start(String id) {
    throw new UnsupportedOperationException(
      "Starting an agent is not supported by this provider"
    );
  }

  /**
   * Stop the given agent on the provider.
   *
   * <p>Only called when the snapshot of the agent advertises {@link Capability#STOP}. Providers that do not support it
   * keep the default implementation.
   *
   * @param id the identifier of the agent to stop (as returned by {@link #discover(String, int)}).
   * @throws UnsupportedOperationException if the provider does not support stopping agents.
   * @throws AgentProviderException if the provider cannot be reached or the id is unknown.
   */
  default void stop(String id) {
    throw new UnsupportedOperationException(
      "Stopping an agent is not supported by this provider"
    );
  }
}
