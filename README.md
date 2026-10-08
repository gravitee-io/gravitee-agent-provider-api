# gravitee-agent-provider-api

Contract between Gravitee and the `agent-provider` plugins: the plugins that let Gravitee discover the AI agents hosted on an external platform.

## Contract

| Type                         | Role                                                                                 |
|------------------------------|--------------------------------------------------------------------------------------|
| `AgentProviderFactory`       | Entry point of a plugin. Abstract class: caches one `AgentProviderApi` per JSON configuration in the node's cache, and closes it once idle. The plugin implements `instantiateProvider(configuration)`; its provider implements `AutoCloseable` to release its resources. |
| `AgentProviderApi`           | `test()`, `discover(cursor, pageSize)`, `fetch(ids)`, and optional `start(id)` / `stop(id)`. |
| `AgentProviderConfiguration` | Marker of the configuration class of a plugin.                                       |

A configuration carries everything the plugin needs to reach its platform, credentials included. The plugin describes it in its `schema-form.json`.

## Build

```bash
mvn clean install
```
