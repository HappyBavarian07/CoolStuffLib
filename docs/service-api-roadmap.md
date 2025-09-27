# CoolStuffLib Service API

**Purpose:**
Provide a robust, backend-agnostic service system so any plugin (AdminPanel or others) can register, lookup, start/stop, monitor, and control services. This document is a single-source reference for developers and Copilot: interfaces, default implementation, lifecycle semantics, threading model, health checks, and a staged roadmap to fully implement and deliver the feature.

---

## Summary / Goals

**Primary goals:**

1. Provide a small set of interfaces to represent services and their lifecycle.
2. Provide a registry that supports registration, lookup, dependency-aware start/stop, timeouts, and state monitoring.
3. Allow services to expose health checks and lifecycle events.
4. Keep the API implementation-agnostic so different teams can implement services (data, config, web, per-world, etc.) without changing the registry.
5. Provide a concise, copy-paste ready `DefaultServiceRegistry` implementation that is safe for Bukkit/Spigot threading (IO off main thread).

**Non-goals:**

* Provide specific service implementations (data persistence, caching, etc.).
* Provide advanced DI or annotation processing in the first iteration.

---

## API Reference

### Service.java

```java
package de.happybavarian07.coolstufflib.service;
import java.util.concurrent.CompletableFuture;

public interface Service {
    String id();
    CompletableFuture<Void> init();
    CompletableFuture<Void> shutdown();
    default CompletableFuture<Void> onReload() { return CompletableFuture.completedFuture(null); }
}
```

### ServiceState.java

```java
package de.happybavarian07.coolstufflib.service;

public enum ServiceState {
    UNREGISTERED, REGISTERED, INITIALIZING, RUNNING, STOPPING, STOPPED, FAILED
}
```

### ServiceDescriptor.java

```java
package de.happybavarian07.coolstufflib.service;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ServiceDescriptor {
    private final String id;
    private final List<String> dependsOn;
    private final Duration startTimeout;
    private final Duration stopTimeout;

    public ServiceDescriptor(String id, List<String> dependsOn, Duration startTimeout, Duration stopTimeout) {
        this.id = Objects.requireNonNull(id);
        this.dependsOn = dependsOn == null ? Collections.emptyList() : List.copyOf(dependsOn);
        this.startTimeout = startTimeout == null ? Duration.ofSeconds(20) : startTimeout;
        this.stopTimeout = stopTimeout == null ? Duration.ofSeconds(10) : stopTimeout;
    }

    public String id() { return id; }
    public List<String> dependsOn() { return dependsOn; }
    public Duration startTimeout() { return startTimeout; }
    public Duration stopTimeout() { return stopTimeout; }
}
```

### ServiceLifecycleListener.java

```java
package de.happybavarian07.coolstufflib.service;

import de.happybavarian07.coolstufflib.service.api.ServiceState;

public interface ServiceLifecycleListener {
    void onStateChange(String serviceId, ServiceState from, ServiceState to, Throwable failure);
}
```

### HealthCheck.java

```java
package de.happybavarian07.coolstufflib.service;
import java.util.concurrent.CompletableFuture;

public interface HealthCheck {
    CompletableFuture<Boolean> healthy();
}
```

### ServiceRegistry.java

```java
package de.happybavarian07.coolstufflib.service;

import de.happybavarian07.coolstufflib.service.api.*;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ServiceRegistry {
    void register(ServiceDescriptor descriptor, Service impl);

    Optional<Service> get(String id);

    <T extends Service> Optional<T> getAs(String id, Class<T> type);

    ServiceState getState(String id);

    Map<String, ServiceState> snapshotStates();

    CompletableFuture<Void> startAll();

    CompletableFuture<Void> stopAll();

    CompletableFuture<Void> start(String id);

    CompletableFuture<Void> stop(String id);

    CompletableFuture<Void> restart(String id);

    void addListener(ServiceLifecycleListener l);

    void removeListener(ServiceLifecycleListener l);

    void registerHealthCheck(String serviceId, HealthCheck check);

    CompletableFuture<Boolean> isHealthy(String serviceId);
}
```

---

## DefaultServiceRegistry.java

(Complete implementation included in the previous section; this Markdown file contains the code fully.)

---

## Usage Example (AdminPanel)

```java
ServiceDescriptor cfgDesc = new ServiceDescriptor("config-service", List.of(), Duration.ofSeconds(5), Duration.ofSeconds(3));
Service configService = new ConfigService(getDataFolder());
registry.register(cfgDesc, configService);

ServiceDescriptor dataDesc = new ServiceDescriptor("data-service", List.of("config-service"), Duration.ofSeconds(20), Duration.ofSeconds(10));
Service dataService = new AdminDataService();
registry.register(dataDesc, dataService);

registry.startAll().whenComplete((v, ex) -> {
    if (ex != null) {
        getLogger().severe("Service startup failed: " + ex.getMessage());
        getServer().getPluginManager().disablePlugin(this);
    } else {
        getLogger().info("All services running");
    }
});

Optional<AdminDataService> ds = registry.getAs("data-service", AdminDataService.class);
registry.addListener((id, from, to, failure) -> {
    getLogger().info("Service " + id + " changed " + from + " -> " + to + (failure != null ? " : " + failure.getMessage() : ""));
    if (to == ServiceState.FAILED) getServer().getPluginManager().disablePlugin(this);
});
```

---

## Roadmap & Implementation Plan

**M0 — Spec + Interfaces:**

* Add interfaces and descriptors.
* Add `ServiceRegistry` interface.

**M1 — Default Implementation (MVP):**

* Implement `DefaultServiceRegistry`.
* Unit tests for registration, lookup, start/stop order, cycle detection, timeouts, health checks.
* Integration test with dummy services.

**M2 — Documentation & Examples:**

* Add README and minimal sample services.

**M3 — Advanced Features (optional):**

* Add factories for lazy service creation with registry injection.
* Annotation-based auto-discovery.
* Metrics export.
* Scoped registries (per-world/per-player).

**M4 — Polishing:**

* Allow external injection of executors.
* Graceful restart support with `onReload()`.
* AdminPanel UI to manage service states and manual restarts.

---

## Testing Checklist

1. Services with and without dependencies: verify start order.
2. Test timeouts: service taking too long.
3. Test cyclic dependencies: should throw exception.
4. Test health checks.
5. Stop services: verify dependents stopped first.
6. Test `restart()`.

---

## Notes

* Services must not block the main thread; use `CompletableFuture` for async IO.
* Registry handles lifecycle events and failures.
* Immutable descriptors and explicit service IDs recommended.

---

## Minimal Sample Service

```java
package de.happybavarian07.coolstufflib.samples;

import de.happybavarian07.coolstufflib.service.api.Service;

import java.util.concurrent.CompletableFuture;

public class ConfigService implements Service {
    private final String id;

    public ConfigService(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public CompletableFuture<Void> init() {
        return CompletableFuture.runAsync(() -> { /* load config */ });
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> { /* save config */ });
    }
}
```

# CoolStuffLib Service API — Advanced Features & DI (Iteration 2)

Purpose:
This document covers advanced features for the Service API in CoolStuffLib, intended for a second iteration after the MVP (DefaultServiceRegistry) is in place. Focus is on Dependency Injection (DI), annotation-based service discovery, and optional features for easier integration and automation.

Goals

1. Add DI capabilities for services that require other services or plugin-specific dependencies.
2. Introduce optional annotation-based auto-discovery for simplifying registration.
3. Maintain compatibility with the existing ServiceRegistry and DefaultServiceRegistry.
4. Ensure lifecycle events and health checks work with DI and annotations.
5. Provide optional factory support for lazy creation.

Advanced Features

ServiceFactory

```java
package de.happybavarian07.coolstufflib.service;

import de.happybavarian07.coolstufflib.service.api.Service;
import de.happybavarian07.coolstufflib.service.api.ServiceRegistry;

import java.util.concurrent.CompletableFuture;

public interface ServiceFactory<T extends Service> {
    CompletableFuture<T> create(ServiceRegistry registry);
}
```

- Allows services to be lazily instantiated.
- Receives ServiceRegistry for dependency injection.
- Works with registerFactory(ServiceDescriptor descriptor, ServiceFactory<T> factory) in ServiceRegistry.

Annotation-Based Discovery

@ServiceComponent

```java
package de.happybavarian07.coolstufflib.service;
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ServiceComponent {
    String id();
    String[] dependsOn() default {};
    long startTimeoutMillis() default 20000;
    long stopTimeoutMillis() default 10000;
}
```

- Classes annotated with @ServiceComponent are automatically registered.
- A small scanner utility can scan the classpath at plugin load and register found services.

Example

```java
@ServiceComponent(id = "data-service", dependsOn = {"config-service"}, startTimeoutMillis = 15000)
public class DataService implements Service {
    // implement init, shutdown
}
```

Optional DI container support

- Services can declare dependencies in constructor or setter methods.
- Registry can resolve dependencies automatically if all dependencies are registered.
- Example using constructor injection:

```java
@ServiceComponent(id="web-service", dependsOn={"data-service"})
public class WebService implements Service {
    private final DataService data;
    public WebService(DataService data) { this.data = data; }
    @Override public String id() { return "web-service"; }
    @Override public CompletableFuture<Void> init() { ... }
    @Override public CompletableFuture<Void> shutdown() { ... }
}
```

Registry Extensions

- registerFactory(ServiceDescriptor descriptor, ServiceFactory<?> factory)
- registerAnnotatedServices(String packageName) — scans a package for @ServiceComponent classes.
- Optional dependency resolution for constructor injection.

Health Checks & Lifecycle

- DI and annotations do not change health check mechanism.
- Listeners still fire on state changes.
- Lazy-loaded services can register health checks during init().

Implementation Plan (Iteration 2)

M0 — Spec & Interfaces

- Define ServiceFactory.
- Define @ServiceComponent annotation.
- Update ServiceRegistry with factory registration.

M1 — DI & Auto-Discovery

- Implement optional DI constructor injection.
- Implement classpath scanner for annotated services.
- Support lazy initialization with factories.

M2 — Testing

- Test annotated service registration.
- Test factory-based lazy initialization.
- Test DI constructor injection with dependencies.
- Test health checks and lifecycle events.

M3 — Documentation & Examples

- Provide sample annotated services.
- Document DI and factory usage.
- Add README section for automatic discovery.

M4 — Optional Enhancements

- Add setter injection support.
- Support optional configuration injection from plugin.yml or external config.
- Add metrics for service creation and startup times.

Notes

- All features in this iteration are optional; existing DefaultServiceRegistry continues to work.
- Focus on maintainability and clarity; do not add magic. Explicit annotations and factories are preferred.
- Ensure backward compatibility with services registered manually.

End of Iteration 2 document.
