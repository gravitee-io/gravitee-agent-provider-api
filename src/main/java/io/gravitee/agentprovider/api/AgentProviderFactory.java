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

import static java.nio.charset.StandardCharsets.UTF_8;

import io.gravitee.node.api.cache.Cache;
import io.gravitee.node.api.cache.CacheConfiguration;
import io.gravitee.node.api.cache.CacheListener;
import io.gravitee.node.api.cache.CacheManager;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Entry point of an agent provider plugin: creates the {@link AgentProvider} bound to a given configuration.
 *
 * <p>The host creates the implementation as a bean of a Spring context dedicated to the plugin, whose parent is the host
 * context: it can inject host beans (such as Vert.x), and the plugin can declare its own {@code @Configuration} classes.
 * The implementation declares a single public constructor receiving the host {@link CacheManager}, so Spring injects it.
 *
 * <p>The factory reuses the provider of a configuration while it is in use, so that the resources it holds (HTTP
 * clients, access tokens, ...) are shared:
 * <ul>
 *   <li>the providers are kept in a local cache of the node, keyed by the SHA-256 checksum of the configuration, so the
 *   configuration is only deserialized on a cache miss and never kept as a key. An invalid configuration is never
 *   cached;</li>
 *   <li>a provider expires after an idle timeout without a call to {@link #create(String)}. The cache only expires them,
 *   it never evicts them on size;</li>
 *   <li>an expired provider is closed with {@link AutoCloseable#close()} once the operations running on it are
 *   complete. Calling it once closed fails with an {@link IllegalStateException}: the host calls
 *   {@link #create(String)} again to get a new one.</li>
 * </ul>
 *
 * <p>The plugin only implements {@link #instantiateProvider(String)}. Its provider implements {@link AutoCloseable} to
 * release the resources it holds (HTTP clients, access tokens, ...): its {@code close()} is called once, when the
 * provider is no longer cached and no operation is running on it anymore. It may run on a thread shared by every cache
 * of the node: it must not block.
 *
 * @param <P> the type of the providers created by the plugin
 * @author GraviteeSource Team
 */
public abstract class AgentProviderFactory<
  P extends AgentProvider & AutoCloseable
>
  implements AutoCloseable {

  /** The time a provider is kept without a call to {@link #create(String)}, unless the plugin sets another one. */
  public static final Duration DEFAULT_IDLE_TIMEOUT = Duration.ofMinutes(50);

  private final CacheManager cacheManager;
  private final Duration idleTimeout;
  // Guarded by this.
  private @Nullable Cache<String, CachedAgentProvider<P>> providers;
  private boolean closed;

  /**
   * Creates a factory whose providers expire after {@link #DEFAULT_IDLE_TIMEOUT}.
   *
   * @param cacheManager the cache manager of the host, in which the providers are cached.
   */
  protected AgentProviderFactory(CacheManager cacheManager) {
    this(cacheManager, DEFAULT_IDLE_TIMEOUT);
  }

  /**
   * Creates a factory whose providers expire after the given idle timeout.
   *
   * @param cacheManager the cache manager of the host, in which the providers are cached.
   * @param idleTimeout the time a provider is kept without a call to {@link #create(String)}.
   */
  protected AgentProviderFactory(
    CacheManager cacheManager,
    Duration idleTimeout
  ) {
    this.cacheManager = Objects.requireNonNull(cacheManager, "cacheManager");
    this.idleTimeout = Objects.requireNonNull(idleTimeout, "idleTimeout");
  }

  /**
   * Returns the provider bound to the given configuration, creating it if it is not cached.
   *
   * <p>The configuration is handed over as a raw JSON string and deserialized by the plugin itself, so the host never
   * has to load configuration classes coming from the plugin class loader.
   *
   * @param configuration the provider configuration as JSON, matching the plugin's configuration schema.
   * @return the provider.
   * @throws AgentProviderException if the configuration is missing or invalid.
   * @throws IllegalStateException if the factory has been closed.
   */
  public final AgentProvider create(@Nullable String configuration) {
    if (configuration == null || configuration.isBlank()) {
      throw new AgentProviderException(
        "Unable to create the agent provider: configuration is null or empty"
      );
    }
    String key = checksum(configuration);
    CachedAgentProvider<P> provider;
    do {
      // An exception thrown by the mapping function leaves no entry behind, so invalid configurations are never cached.
      // The node's cache reads the entry back after computing it, and gets nothing if it expired in between: the
      // provider is then retired by the cache listener, so another one is created.
      provider = providers().computeIfAbsent(key, k ->
        new CachedAgentProvider<>(
          Objects.requireNonNull(instantiateProvider(configuration), "provider")
        )
      );
    } while (provider == null);
    if (isClosed()) {
      // Closed while it was being created: the cache has been destroyed, nobody would close this provider.
      provider.retire();
      throw closedException();
    }
    return provider;
  }

  /**
   * Creates a provider bound to the given configuration. Called by {@link #create(String)} when no provider is cached
   * for this configuration.
   *
   * @param configuration the provider configuration as JSON, never blank.
   * @return the provider.
   * @throws AgentProviderException if the configuration is invalid.
   */
  protected abstract P instantiateProvider(String configuration);

  /**
   * Closes every cached provider and releases the cache. Called by the host when the plugin is undeployed; the factory
   * cannot create providers anymore.
   */
  @Override
  public void close() {
    Cache<String, CachedAgentProvider<P>> cache;
    synchronized (this) {
      if (closed) {
        return;
      }
      closed = true;
      cache = providers;
      providers = null;
    }
    if (cache != null) {
      List<CachedAgentProvider<P>> cached = List.copyOf(cache.values());
      cacheManager.destroy(cacheName());
      cached.forEach(CachedAgentProvider::retire);
    }
  }

  private synchronized Cache<String, CachedAgentProvider<P>> providers() {
    if (closed) {
      throw closedException();
    }
    if (providers == null) {
      // Local: a provider holds credentials and clients, it must never leave the node. No maximum size: an eviction on
      // size could hit a provider in use.
      this.providers = cacheManager.getOrCreateCache(
        cacheName(),
        CacheConfiguration.builder()
          .distributed(false)
          .timeToIdleInMs(idleTimeout.toMillis())
          .build()
      );
      this.providers.addCacheListener(
        new CacheListener<>() {
          @Override
          public void onEntryExpired(
            String key,
            CachedAgentProvider<P> provider
          ) {
            retire(provider);
          }

          @Override
          public void onEntryEvicted(
            String key,
            CachedAgentProvider<P> provider
          ) {
            retire(provider);
          }
        }
      );
    }
    return providers;
  }

  private synchronized boolean isClosed() {
    return closed;
  }

  private static IllegalStateException closedException() {
    return new IllegalStateException(
      "The agent provider factory has been closed"
    );
  }

  private String cacheName() {
    return "agent-provider-" + getClass().getName();
  }

  private static void retire(@Nullable CachedAgentProvider<?> provider) {
    if (provider != null) {
      provider.retire();
    }
  }

  private static String checksum(String configuration) {
    try {
      // MessageDigest is not thread-safe: one instance per call.
      return HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(
          configuration.getBytes(UTF_8)
        )
      );
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is not available", ex);
    }
  }
}
