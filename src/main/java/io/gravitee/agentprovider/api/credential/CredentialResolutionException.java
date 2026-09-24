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
package io.gravitee.agentprovider.api.credential;

import io.gravitee.agentprovider.api.AgentProviderException;

/**
 * Raised by a {@link CredentialResolver} when a credential cannot be handed to the plugin: the id is unknown, the
 * credential is not an OAuth2 client-credentials one, or no resolver is available in the current context.
 *
 * <p>It is an {@link AgentProviderException}, so a plugin reporting a probe can surface it like any other failure to
 * reach its upstream.
 *
 * @author GraviteeSource Team
 */
public class CredentialResolutionException extends AgentProviderException {

  public CredentialResolutionException(String message) {
    super(message);
  }

  public CredentialResolutionException(String message, Throwable cause) {
    super(message, cause);
  }
}
