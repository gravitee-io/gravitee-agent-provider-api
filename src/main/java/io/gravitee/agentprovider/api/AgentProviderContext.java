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

import io.gravitee.agentprovider.api.credential.CredentialResolver;
import java.util.Objects;

/**
 * Host services an agent provider may use, handed over when the provider is created.
 *
 * <p>A context is scoped by the host: in AI Management it is bound to the environment owning the source, so the
 * credentials it resolves are the ones of that environment only. A plugin caching providers must therefore treat two
 * distinct context instances as two distinct scopes, even for an identical configuration, and never reuse a provider
 * created with one context for a call made with another. Hosts, in return, hand the same instance for the same scope.
 *
 * @author GraviteeSource Team
 */
public interface AgentProviderContext {
  /**
   * @return the resolver of credentials stored by the host.
   */
  CredentialResolver credentials();

  /**
   * A context exposing only the given credential resolver.
   *
   * @param credentials the resolver.
   * @return the context.
   */
  static AgentProviderContext of(CredentialResolver credentials) {
    Objects.requireNonNull(credentials, "credentials");
    return () -> credentials;
  }
}
