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
 * Marker interface for the configuration class of an agent provider plugin.
 *
 * <p>The host never instantiates it: the configuration reaches the plugin as a JSON string through
 * {@link AgentProviderFactory#create(String)}. Implementing this interface only lets the plugin
 * loader identify the configuration class of a plugin.
 *
 * @author GraviteeSource Team
 */
public interface AgentProviderConfiguration {}
