# CoolStuffLib Service API Tutorial

This tutorial introduces the Service API in CoolStuffLib, covering service registration, dependency injection, annotation-based discovery, lazy initialization, health checks, metrics, event hooks, and scoped registries. It is designed for plugin developers who want robust, modular, and maintainable service management.

## 1. Service Basics

Services are modular components with lifecycle methods (`init`, `shutdown`, `onReload`). Each service has a unique ID and can declare dependencies on other services.

```java
public class MyService implements Service {
    private final String id;
    public MyService(String id) { this.id = id; }
    public String id() { return id; }
    public CompletableFuture<Void> init() { /* ... */ }
    public CompletableFuture<Void> shutdown() { /* ... */ }
    public CompletableFuture<Void> onReload() { /* ... */ }
}
```

## 2. Registering Services

Register services with the registry using a `ServiceDescriptor`:

```java
ServiceDescriptor desc = new ServiceDescriptor("my-service", List.of(), Duration.ofSeconds(10), Duration.ofSeconds(5));
registry.register(desc, new MyService("my-service"));
```

## 3. Dependency Injection (DI)

Services can declare dependencies in their constructor. The registry will inject them if available:

```java
public class DataService implements Service {
    private final ConfigService config;
    public DataService(ConfigService config) { this.config = config; }
    // ...
}
```

## 4. Annotation-Based Discovery

Annotate your service class with `@ServiceComponent` for auto-discovery:

```java
@ServiceComponent(id = "data-service", dependsOn = {"config-service"})
public class DataService implements Service { /* ... */ }
```

Scan and register annotated services:

```java
registry.registerAnnotatedServices("de.myplugin.services");
```

### Troubleshooting Annotation Scanning
- Ensure your package name matches the compiled classpath structure.
- The scanner now recursively finds annotated classes in subpackages and avoids duplicates.
- If services are not detected, check build output and classpath configuration.

## 5. Lazy Initialization with Factories

Use a factory for services that should be created on demand:

```java
ServiceFactory<DataService> factory = reg -> CompletableFuture.completedFuture(new DataService(reg.get("config-service").get()));
registry.registerFactory(desc, factory);
```

## 6. Health Checks

Register health checks for services:

```java
registry.registerHealthCheck("data-service", () -> CompletableFuture.completedFuture(true));
CompletableFuture<Boolean> healthy = registry.isHealthy("data-service");
```

## 7. Metrics and Monitoring

Track startup/reload times and health check failures:

```java
long startupMs = registry.getStartupTime("data-service");
int failures = registry.getHealthCheckFailures("data-service");
```
- Metrics are tracked for both single-service and batch operations (startAll, reloadAll).

## 8. Event Hooks and Spigot Events

Listen for lifecycle events and cancel if needed:

```java
registry.addEventListener(new ServiceEventListener() {
    public void onInit(ServiceEvent e) { /* ... */ }
    public void onShutdown(ServiceEvent e) { /* ... */ }
    public void onReload(ServiceEvent e) { /* ... */ }
    public void onFail(ServiceEvent e) { /* ... */ }
});
```

Spigot events are fired for init, shutdown, reload, and fail. You can listen and cancel them using Bukkit’s event system.
- In non-Bukkit environments, event hooks are now null-safe and will not throw errors.

## 9. Scoped Registries

Create per-world or per-player registries for isolated service management:

```java
ServiceRegistry worldRegistry = factory.getWorldRegistry("world");
ServiceRegistry playerRegistry = factory.getPlayerRegistry("playerName");
```

### Edge Cases and Advanced Usage
- Each registry instance is isolated; services registered in one world/player registry are not visible to others unless explicitly shared.
- Cross-world dependencies: If a service in one world depends on a service from another, use registry lookup with explicit world context.
- Player disconnect: When a player leaves, ensure their registry is properly shutdown to avoid resource leaks.
- Batch operations: You can start/stop/reload all services in a scoped registry using `startAll()`, `stopAll()`, or `reloadAll()`.

## 9a. Advanced ServiceLifecycleListener Implementations

Custom listeners can handle complex lifecycle events and error recovery:

```java
public class CustomLifecycleListener implements ServiceLifecycleListener {
    public void onStateChange(Service service, ServiceState oldState, ServiceState newState) {
        if (newState == ServiceState.FAILED) {
            // Attempt recovery or log error
        }
    }
    public void onHealthCheck(Service service, boolean healthy) {
        if (!healthy) {
            // Trigger alert or fallback
        }
    }
}
registry.addLifecycleListener(new CustomLifecycleListener());
```

## 10. Full Example

```java
@ServiceComponent(id = "config-service")
public class ConfigService implements Service { /* ... */ }

@ServiceComponent(id = "data-service", dependsOn = {"config-service"})
public class DataService implements Service {
    private final ConfigService config;
    public DataService(ConfigService config) { this.config = config; }
    // ...
}

// Register all annotated services
registry.registerAnnotatedServices("de.myplugin.services");
registry.startAll();
```

## 11. Best Practices

- Use explicit IDs and dependencies.
- Prefer annotation-based registration for modularity.
- Register health checks for critical services.
- Use event hooks for advanced control and monitoring.
- Use scoped registries for per-world/per-player isolation.

## 12. Migration Notes and Troubleshooting

- When migrating from older service systems, ensure all services implement the new lifecycle methods (`init`, `shutdown`, `onReload`).
- Replace legacy event hooks with the new event listener API for consistent behavior.
- Annotation scanning now supports recursive package search and duplicate prevention; verify your build output if services are missing.
- For backend-agnostic usage, avoid direct Bukkit/Spigot API calls in service implementations; use the provided event hooks and registry methods.
- If you encounter issues with registry isolation or event propagation, check that each registry is correctly initialized and listeners are registered per scope.

## 13. Backend-Agnostic Usage

The service registry and all service operations are designed to be backend-agnostic. Avoid direct dependencies on Bukkit/Spigot APIs in your service implementations. Instead, use the registry’s event hooks and lifecycle listeners for cross-platform compatibility. If you need platform-specific logic, encapsulate it in separate modules and keep the service API usage generic.

### Example: Platform-Agnostic Event Handling

```java
public class UniversalServiceListener implements ServiceLifecycleListener {
    public void onStateChange(Service service, ServiceState oldState, ServiceState newState) {
        if (newState == ServiceState.RUNNING) {
            // Platform-agnostic logic here
        }
    }
}
registry.addLifecycleListener(new UniversalServiceListener());
```

## 14. Advanced DI and Annotation Processing

For advanced dependency injection or annotation processing, you can extend the annotation scanner or integrate with third-party DI frameworks. The registry supports constructor injection for dependencies declared in service constructors. For more complex scenarios, consider:
- Custom annotation processors to handle additional metadata or lifecycle hooks.
- Integration with DI frameworks (e.g., Guice, Spring) by adapting the registry’s factory methods.
- Extending the annotation scanner to support custom annotations or multi-stage discovery.

### Example: Custom Annotation Processor

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CustomServiceMeta {
    String value();
}

// Extend the scanner to process @CustomServiceMeta
```

---

For more advanced usage, see the roadmap and API reference in the docs folder.
