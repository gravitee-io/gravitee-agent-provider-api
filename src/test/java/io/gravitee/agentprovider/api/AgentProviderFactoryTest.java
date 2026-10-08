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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import io.gravitee.agentprovider.api.model.AgentSnapshot;
import io.gravitee.agentprovider.api.model.DiscoveryPage;
import io.gravitee.agentprovider.api.model.Probe;
import io.gravitee.node.api.cache.CacheManager;
import io.gravitee.node.plugin.cache.standalone.StandaloneCacheManager;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AgentProviderFactoryTest {

  private static final String CONFIGURATION = "{\"name\":\"first\"}";
  private static final String OTHER_CONFIGURATION = "{\"name\":\"second\"}";
  private static final String INVALID_CONFIGURATION = "{\"invalid\":true}";

  @Test
  void should_create_the_provider_of_the_configuration() {
    FakeFactory factory = new FakeFactory(new StandaloneCacheManager());

    factory.create(CONFIGURATION).test();

    assertThat(factory.created).hasSize(1);
    assertThat(factory.created.getFirst().configuration).isEqualTo(
      CONFIGURATION
    );
    assertThat(factory.created.getFirst().tests).hasValue(1);
  }

  @Test
  void should_reuse_the_provider_of_a_configuration() {
    FakeFactory factory = new FakeFactory(new StandaloneCacheManager());

    assertThat(factory.create(CONFIGURATION)).isSameAs(
      factory.create(CONFIGURATION)
    );
    assertThat(factory.created).hasSize(1);
  }

  @Test
  void should_create_one_provider_per_configuration() {
    FakeFactory factory = new FakeFactory(new StandaloneCacheManager());

    assertThat(factory.create(CONFIGURATION)).isNotSameAs(
      factory.create(OTHER_CONFIGURATION)
    );
    assertThat(factory.created).hasSize(2);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void should_fail_when_the_configuration_is_missing(String configuration) {
    FakeFactory factory = new FakeFactory(new StandaloneCacheManager());

    assertThatThrownBy(() -> factory.create(configuration))
      .isInstanceOf(AgentProviderException.class)
      .hasMessage(
        "Unable to create the agent provider: configuration is null or empty"
      );
    assertThat(factory.created).isEmpty();
  }

  @Test
  void should_not_cache_an_invalid_configuration() {
    FakeFactory factory = new FakeFactory(new StandaloneCacheManager());

    assertThatThrownBy(() ->
      factory.create(INVALID_CONFIGURATION)
    ).isInstanceOf(AgentProviderException.class);
    assertThatThrownBy(() ->
      factory.create(INVALID_CONFIGURATION)
    ).isInstanceOf(AgentProviderException.class);
    assertThat(factory.attempts).hasValue(2);
  }

  @Test
  void should_close_every_provider_when_closed() {
    FakeFactory factory = new FakeFactory(new StandaloneCacheManager());
    factory.create(CONFIGURATION);
    factory.create(OTHER_CONFIGURATION);

    factory.close();

    assertThat(factory.created).allMatch(FakeProvider::isClosed);
  }

  @Test
  void should_close_a_factory_that_never_created_a_provider() {
    FakeFactory factory = new FakeFactory(new StandaloneCacheManager());

    factory.close();

    assertThat(factory.created).isEmpty();
  }

  @Test
  void should_close_a_factory_only_once() {
    FakeFactory factory = new FakeFactory(new StandaloneCacheManager());
    factory.create(CONFIGURATION);
    factory.close();

    factory.close();

    assertThat(factory.created).singleElement().matches(FakeProvider::isClosed);
  }

  @Test
  void should_not_create_providers_once_closed() {
    FakeFactory factory = new FakeFactory(new StandaloneCacheManager());
    factory.close();

    assertThatThrownBy(() -> factory.create(CONFIGURATION))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("The agent provider factory has been closed");
  }

  @Test
  void should_let_the_plugin_reject_an_unsupported_operation() {
    FakeFactory factory = new FakeFactory(new StandaloneCacheManager());

    assertThatThrownBy(() ->
      factory.create(CONFIGURATION).start("agent")
    ).isInstanceOf(UnsupportedOperationException.class);
  }

  @Nested
  class Expiry {

    private static final Duration IDLE_TIMEOUT = Duration.ofMillis(200);

    @Test
    void should_keep_the_provider_while_it_is_used() {
      FakeFactory factory = new FakeFactory(
        new StandaloneCacheManager(),
        IDLE_TIMEOUT
      );
      AgentProvider provider = factory.create(CONFIGURATION);

      for (int i = 0; i < 5; i++) {
        sleep(IDLE_TIMEOUT.dividedBy(2));
        assertThat(factory.create(CONFIGURATION)).isSameAs(provider);
      }
      assertThat(factory.created.getFirst().isClosed()).isFalse();
    }

    @Test
    void should_replace_and_close_an_expired_provider() {
      FakeFactory factory = new FakeFactory(
        new StandaloneCacheManager(),
        IDLE_TIMEOUT
      );
      AgentProvider expired = factory.create(CONFIGURATION);

      sleep(IDLE_TIMEOUT.multipliedBy(2));
      AgentProvider replacement = factory.create(CONFIGURATION);

      assertThat(replacement).isNotSameAs(expired);
      await()
        .atMost(Duration.ofSeconds(5))
        .until(factory.created.getFirst()::isClosed);
      assertThat(factory.created.get(1).isClosed()).isFalse();
      assertThatThrownBy(expired::test)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(
          "The agent provider has been closed: create a new one from the factory"
        );
    }

    @Test
    void should_close_an_expired_provider_once_its_operations_are_complete() {
      FakeFactory factory = new FakeFactory(
        new StandaloneCacheManager(),
        IDLE_TIMEOUT
      );
      AgentProvider expired = factory.create(CONFIGURATION);
      FakeProvider delegate = factory.created.getFirst();
      delegate.release = new CountDownLatch(1);
      CompletableFuture<Probe> running = CompletableFuture.supplyAsync(
        expired::test
      );
      await()
        .atMost(Duration.ofSeconds(5))
        .until(() -> delegate.tests.get() == 1);

      sleep(IDLE_TIMEOUT.multipliedBy(2));
      factory.create(CONFIGURATION);
      await()
        .atMost(Duration.ofSeconds(5))
        .until(() -> factory.created.size() == 2);
      sleep(IDLE_TIMEOUT);

      // Expired while the operation is running: still open.
      assertThat(delegate.isClosed()).isFalse();
      delegate.release.countDown();
      assertThat(running.join()).isEqualTo(Probe.ok());
      await().atMost(Duration.ofSeconds(5)).until(delegate::isClosed);
    }

    private static void sleep(Duration duration) {
      try {
        Thread.sleep(duration.toMillis());
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(ex);
      }
    }
  }

  private static class FakeFactory extends AgentProviderFactory<FakeProvider> {

    private final List<FakeProvider> created = new CopyOnWriteArrayList<>();
    private final AtomicInteger attempts = new AtomicInteger();

    FakeFactory(CacheManager cacheManager) {
      super(cacheManager);
    }

    FakeFactory(CacheManager cacheManager, Duration idleTimeout) {
      super(cacheManager, idleTimeout);
    }

    @Override
    protected FakeProvider instantiateProvider(String configuration) {
      attempts.incrementAndGet();
      if (INVALID_CONFIGURATION.equals(configuration)) {
        throw new AgentProviderException("Invalid configuration");
      }
      FakeProvider provider = new FakeProvider(configuration);
      created.add(provider);
      return provider;
    }
  }

  private static class FakeProvider implements AgentProvider, AutoCloseable {

    private final String configuration;
    private final AtomicInteger tests = new AtomicInteger();
    // Holds the operations until it is counted down: open by default.
    private volatile CountDownLatch release = new CountDownLatch(0);
    private volatile boolean closed;

    FakeProvider(String configuration) {
      this.configuration = configuration;
    }

    @Override
    public Probe test() {
      tests.incrementAndGet();
      try {
        release.await();
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
      return Probe.ok();
    }

    @Override
    public DiscoveryPage discover(@Nullable String cursor, int pageSize) {
      return new DiscoveryPage(List.of(), null);
    }

    @Override
    public List<AgentSnapshot> fetch(String... ids) {
      return List.of();
    }

    @Override
    public void close() {
      closed = true;
    }

    boolean isClosed() {
      return closed;
    }
  }
}
