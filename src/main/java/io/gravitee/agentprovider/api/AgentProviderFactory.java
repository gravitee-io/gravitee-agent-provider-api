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

/**
 * Entry point of an agent provider plugin: creates an {@link AgentProviderApi} bound to a given configuration.
 *
 * <p>The host creates the implementation as a bean of a Spring context dedicated to the plugin, whose parent is the host
 * context: it can inject host beans (such as Vert.x), and the plugin can declare its own {@code @Configuration} classes.
 *
 * @author GraviteeSource Team
 */
public interface AgentProviderFactory {
  /**
   * Creates a provider bound to the given configuration.
   *
   * <p>The configuration is handed over as a raw JSON string and deserialized by the plugin itself, so the host never
   * has to load configuration classes coming from the plugin class loader.
   *
   * <p>The context carries the host services the provider may rely on, such as the resolver of credentials the
   * configuration references by id. It is scoped by the host (see {@link AgentProviderContext}): a plugin caching the
   * providers it creates must key them on the context as well as on the configuration.
   *
   * @param configuration the provider configuration as JSON, matching the plugin's configuration schema.
   * @param context the host services available to the provider.
   * @return the provider.
   * @throws AgentProviderException if the configuration is missing or invalid.
   */
  AgentProviderApi create(String configuration, AgentProviderContext context);
}
