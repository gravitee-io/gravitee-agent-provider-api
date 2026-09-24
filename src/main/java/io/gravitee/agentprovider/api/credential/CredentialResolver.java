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

/**
 * Resolves a credential stored by the host from its id, on behalf of an agent provider.
 *
 * <p>The host hands an implementation to the plugin through {@link io.gravitee.agentprovider.api.AgentProviderContext}.
 * A plugin must call it <strong>when it needs the credential</strong> (typically each time it requests a token), not
 * once when the provider is created: this is how a credential rotated or revoked on the host side reaches the plugin
 * without any redeployment.
 *
 * <p>Besides {@link CredentialResolutionException}, the host may throw its own runtime exceptions, for instance to
 * refuse a credential its policy forbids. A plugin must let them propagate untouched rather than translating or
 * swallowing them: they carry the host's own diagnostic and error mapping.
 *
 * @author GraviteeSource Team
 */
@FunctionalInterface
public interface CredentialResolver {
  /**
   * Resolves an OAuth2 client-credentials credential.
   *
   * @param credentialId the id of the credential as it appears in the provider configuration.
   * @return the decrypted credential material.
   * @throws CredentialResolutionException if there is no such credential, or it is not an OAuth2 client-credentials
   *     one.
   */
  OAuth2ClientCredentials oauth2ClientCredentials(String credentialId);
}
