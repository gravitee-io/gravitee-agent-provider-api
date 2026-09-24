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

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The material of an OAuth2 client-credentials grant, as resolved by the host from one of its stored credentials.
 *
 * <p>{@code scopes} may be empty: the plugin then applies whatever default scope its upstream requires.
 *
 * <p>The secret is masked by {@link #toString()} so that an instance reaching a log line or an exception message does
 * not print it.
 *
 * @param tokenUrl the token endpoint to post the grant to.
 * @param clientId the client id.
 * @param clientSecret the client secret.
 * @param scopes the scopes to request, possibly empty.
 * @author GraviteeSource Team
 */
public record OAuth2ClientCredentials(
  String tokenUrl,
  String clientId,
  String clientSecret,
  List<String> scopes
) {
  public OAuth2ClientCredentials(
    String tokenUrl,
    String clientId,
    String clientSecret,
    @Nullable List<String> scopes
  ) {
    this.tokenUrl = Objects.requireNonNull(tokenUrl, "tokenUrl");
    this.clientId = Objects.requireNonNull(clientId, "clientId");
    this.clientSecret = Objects.requireNonNull(clientSecret, "clientSecret");
    this.scopes = scopes == null ? List.of() : List.copyOf(scopes);
  }

  @Override
  public String toString() {
    return (
      "OAuth2ClientCredentials[tokenUrl=" +
      tokenUrl +
      ", clientId=" +
      clientId +
      ", clientSecret=****, scopes=" +
      scopes +
      "]"
    );
  }
}
