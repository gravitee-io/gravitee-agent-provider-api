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
import io.gravitee.agentprovider.api.model.DiscoveryPage;
import io.gravitee.agentprovider.api.model.Probe;
import io.gravitee.node.logging.NodeLoggerFactory;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * The provider returned by {@link AgentProviderFactory#create(String)}: delegates to the provider of the plugin, and
 * closes it once it is retired from the cache and no operation is running on it anymore.
 *
 * @param <P> the type of the provider of the plugin
 * @author GraviteeSource Team
 */
final class CachedAgentProvider<P extends AgentProvider & AutoCloseable>
  implements AgentProvider {

  private static final Logger LOGGER = NodeLoggerFactory.getLogger(
    CachedAgentProvider.class
  );

  private final P delegate;
  private final AtomicReference<State> state = new AtomicReference<>(
    new State(0, false, false)
  );

  CachedAgentProvider(P delegate) {
    this.delegate = delegate;
  }

  @Override
  public Probe test() {
    return call(delegate::test);
  }

  @Override
  public DiscoveryPage discover(@Nullable String cursor, int pageSize) {
    return call(() -> delegate.discover(cursor, pageSize));
  }

  @Override
  public List<AgentSnapshot> fetch(String... ids) {
    return call(() -> delegate.fetch(ids));
  }

  @Override
  public void start(String id) {
    run(() -> delegate.start(id));
  }

  @Override
  public void stop(String id) {
    run(() -> delegate.stop(id));
  }

  /** Marks the provider as no longer cached: it is closed now if idle, or by the last running operation. */
  void retire() {
    state.updateAndGet(State::retire);
    closeIfIdle();
  }

  private <T> T call(Supplier<T> operation) {
    begin();
    try {
      return operation.get();
    } finally {
      end();
    }
  }

  private void run(Runnable operation) {
    begin();
    try {
      operation.run();
    } finally {
      end();
    }
  }

  private void begin() {
    state.updateAndGet(State::begin);
  }

  private void end() {
    state.updateAndGet(State::end);
    closeIfIdle();
  }

  /** Closes the provider if it is retired and idle. Only the caller that flags it as closed closes it. */
  private void closeIfIdle() {
    State current;
    do {
      current = state.get();
      if (!current.closable()) {
        return;
      }
    } while (!state.compareAndSet(current, current.close()));
    try {
      delegate.close();
    } catch (Exception ex) {
      // Never fail the operation that happened to be the last one, nor the thread of the cache.
      LOGGER.warn("Unable to close the agent provider", ex);
    }
  }

  /**
   * The lifecycle of the provider, replaced as a whole on each change.
   *
   * @param running the number of operations running on the provider
   * @param retired whether the provider is no longer cached
   * @param closed whether the provider has been closed
   */
  private record State(int running, boolean retired, boolean closed) {
    State begin() {
      if (closed) {
        throw new IllegalStateException(
          "The agent provider has been closed: create a new one from the factory"
        );
      }
      return new State(running + 1, retired, false);
    }

    State end() {
      return new State(running - 1, retired, closed);
    }

    State retire() {
      return new State(running, true, closed);
    }

    State close() {
      return new State(running, retired, true);
    }

    boolean closable() {
      return retired && running == 0 && !closed;
    }
  }
}
