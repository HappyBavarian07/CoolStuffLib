package de.happybavarian07.coolstufflib.service.api;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ServiceRegistry {
    /**
     * <p>Registers a service implementation with its descriptor.</p>
     * <pre><code>registry.register(descriptor, serviceImpl);</code></pre>
     *
     * @param descriptor the service descriptor
     * @param impl       the service implementation
     */
    void register(ServiceDescriptor descriptor, Service impl);

    /**
     * <p>Registers a factory for lazy service instantiation.</p>
     * <pre><code>registry.registerFactory(descriptor, factory);</code></pre>
     *
     * @param descriptor the service descriptor
     * @param factory    the service factory
     */
    <T extends Service> void registerFactory(ServiceDescriptor descriptor, ServiceFactory<T> factory);

    /**
     * <p>Retrieves a service by its ID.</p>
     * <pre><code>Optional&lt;Service&gt; svc = registry.get("serviceA");</code></pre>
     *
     * @param id the service ID
     * @return the service, or empty if not found
     */
    Optional<Service> get(String id);

    /**
     * <p>Retrieves a service by its ID and type.</p>
     * <pre><code>Optional&lt;MyService&gt; svc = registry.getAs("serviceA", MyService.class);</code></pre>
     *
     * @param id   the service ID
     * @param type the expected service type
     * @return the service, or empty if not found or type mismatch
     */
    <T extends Service> Optional<T> getAs(String id, Class<T> type);

    /**
     * <p>Gets the current state of a service.</p>
     * <pre><code>ServiceState state = registry.getState("serviceA");</code></pre>
     *
     * @param id the service ID
     * @return the service state
     */
    ServiceState getState(String id);

    /**
     * <p>Returns a snapshot of all service states.</p>
     * <pre><code>Map&lt;String, ServiceState&gt; states = registry.snapshotStates();</code></pre>
     *
     * @return map of service IDs to states
     */
    Map<String, ServiceState> snapshotStates();

    /**
     * <p>Starts all registered services asynchronously.</p>
     * <pre><code>registry.startAll().join();</code></pre>
     *
     * @return a future that completes when all services are started
     */
    CompletableFuture<Void> startAll();

    /**
     * <p>Stops all registered services asynchronously.</p>
     * <pre><code>registry.stopAll().join();</code></pre>
     *
     * @return a future that completes when all services are stopped
     */
    CompletableFuture<Void> stopAll();

    /**
     * <p>Starts a specific service asynchronously.</p>
     * <pre><code>registry.start("serviceA").join();</code></pre>
     *
     * @param id the service ID
     * @return a future that completes when the service is started
     */
    CompletableFuture<Void> start(String id);

    /**
     * <p>Stops a specific service asynchronously.</p>
     * <pre><code>registry.stop("serviceA").join();</code></pre>
     *
     * @param id the service ID
     * @return a future that completes when the service is stopped
     */
    CompletableFuture<Void> stop(String id);

    /**
     * <p>Restarts a specific service asynchronously.</p>
     * <pre><code>registry.restart("serviceA").join();</code></pre>
     *
     * @param id the service ID
     * @return a future that completes when the service is restarted
     */
    CompletableFuture<Void> restart(String id);

    /**
     * <p>Adds a lifecycle listener for service events.</p>
     * <pre><code>registry.addListener(listener);</code></pre>
     *
     * @param l the listener to add
     */
    void addListener(ServiceLifecycleListener l);

    /**
     * <p>Removes a lifecycle listener.</p>
     * <pre><code>registry.removeListener(listener);</code></pre>
     *
     * @param l the listener to remove
     */
    void removeListener(ServiceLifecycleListener l);

    /**
     * <p>Registers a health check for a service.</p>
     * <pre><code>registry.registerHealthCheck("serviceA", healthCheck);</code></pre>
     *
     * @param serviceId the service ID
     * @param check     the health check
     */
    void registerHealthCheck(String serviceId, HealthCheck check);

    /**
     * <p>Checks if a service is healthy asynchronously.</p>
     * <pre><code>CompletableFuture&lt;Boolean&gt; healthy = registry.isHealthy("serviceA");</code></pre>
     *
     * @param serviceId the service ID
     * @return a future with the health status
     */
    CompletableFuture<Boolean> isHealthy(String serviceId);

    /**
     * <p>Registers all annotated services in a package.</p>
     * <pre><code>registry.registerAnnotatedServices("my.package", config);</code></pre>
     *
     * @param packageName the package to scan
     * @param config      the config to inject
     */
    <T extends Service> void registerAnnotatedServices(String packageName, Config config);

    /**
     * <p>Reloads a specific service asynchronously.</p>
     * <pre><code>registry.reload("serviceA").join();</code></pre>
     *
     * @param id the service ID
     * @return a future that completes when reload is done
     */
    CompletableFuture<Void> reload(String id);

    /**
     * <p>Reloads all services asynchronously.</p>
     * <pre><code>registry.reloadAll().join();</code></pre>
     *
     * @return a future that completes when reload is done
     */
    CompletableFuture<Void> reloadAll();
}
