package de.happybavarian07.coolstufflib.service.api;

import de.happybavarian07.coolstufflib.service.util.Tuples;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ServiceRegistry {
    /**
     * <p>Registers a service implementation with its descriptor.</p>
     * <pre><code>registry.register(descriptor, serviceImpl);</code></pre>
     *
     * @param descriptor the service descriptor
     * @param impl       the service implementation
     * @param uuid       the UUID to assign (or null for random)
     */
    Tuples.Tuple2<Service, UUID> register(ServiceDescriptor descriptor, Service impl, UUID uuid);

    /**
     * <p>Registers a factory for lazy service instantiation.</p>
     * <pre><code>registry.registerFactory(descriptor, factory);</code></pre>
     *
     * @param descriptor the service descriptor
     * @param factory    the service factory
     * @param uuid       the UUID to assign (or null for random)
     */
    <T extends Service> Tuples.Tuple2<T, UUID> registerFactory(ServiceDescriptor descriptor, ServiceFactory<T> factory, UUID uuid);

    /**
     * <p>Retrieves a service by its ID.</p>
     * <pre><code>Optional&lt;Service&gt; svc = registry.get("serviceA");</code></pre>
     *
     * @param id the service ID
     * @return the service, or empty if not found
     */
    Optional<Service> get(UUID id);

    /**
     * <p>Retrieves a service by its name.</p>
     * <pre><code>Optional&lt;Service&gt; svc = registry.getByName("serviceA");</code></pre>
     *
     * @param serviceName the service name
     * @return the service, or empty if not found
     */
    Optional<Service> getByName(String serviceName);

    /**
     * <p>Gets the ID of a service by its name.</p>
     * <pre><code>UUID id = registry.getIdByName("serviceA");</code></pre>
     *
     * @param id the service ID
     * @return the service name, or null ("") if not found
     */
    String getNameById(UUID id);

    /**
     * <p>Retrieves a service by its ID and type.</p>
     * <pre><code>Optional&lt;MyService&gt; svc = registry.getAs("serviceA", MyService.class);</code></pre>
     *
     * @param id   the service ID
     * @param type the expected service type
     * @return the service, or empty if not found or type mismatch
     */
    <T extends Service> Optional<T> getAs(UUID id, Class<T> type);

    /**
     * <p>Retrieves a service by its name and type.</p>
     * <pre><code>Optional&lt;MyService&gt; svc = registry.getAsByName("serviceA", MyService.class);</code></pre>
     *
     * @param serviceName the service name
     * @param type        the expected service type
     * @return the service, or empty if not found or type mismatch
     */
    <T extends Service> Optional<T> getAsByName(String serviceName, Class<T> type);

    /**
     * <p>Gets the current state of a service.</p>
     * <pre><code>ServiceState state = registry.getState("serviceA");</code></pre>
     *
     * @param id the service ID
     * @return the service state
     */
    ServiceState getState(UUID id);

    /**
     * <p>Gets the current state of a service by its name.</p>
     * <pre><code>ServiceState state = registry.getStateByName("serviceA");</code></pre>
     *
     * @param serviceName the service name
     * @return the service state
     */
    ServiceState getStateByName(String serviceName);

    /**
     * <p>Returns a snapshot of all service states.</p>
     * <pre><code>Map&lt;String, ServiceState&gt; states = registry.snapshotStates();</code></pre>
     *
     * @return map of service IDs to states
     */
    Map<UUID, ServiceState> snapshotStates();

    /**
     * <p>Returns a snapshot of all service states indexed by service name.</p>
     * <pre><code>Map&lt;String, ServiceState&gt; states = registry.snapshotStatesByName();</code></pre>
     *
     * @return map of service names to states
     */
    Map<String, ServiceState> snapshotStatesByName();

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
    CompletableFuture<Void> start(UUID id);

    /**
     * <p>Stops a specific service asynchronously.</p>
     * <pre><code>registry.stop("serviceA").join();</code></pre>
     *
     * @param id the service ID
     * @return a future that completes when the service is stopped
     */
    CompletableFuture<Void> stop(UUID id);

    /**
     * <p>Restarts a specific service asynchronously.</p>
     * <pre><code>registry.restart("serviceA").join();</code></pre>
     *
     * @param id the service ID
     * @return a future that completes when the service is restarted
     */
    CompletableFuture<Void> restart(UUID id);

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
    void registerHealthCheck(UUID serviceId, HealthCheck check);

    /**
     * <p>Checks if a service is healthy asynchronously.</p>
     * <pre><code>CompletableFuture&lt;Boolean&gt; healthy = registry.isHealthy("serviceA");</code></pre>
     *
     * @param serviceId the service ID
     * @return a future with the health status
     */
    CompletableFuture<Boolean> isHealthy(UUID serviceId);

    /**
     * <p>Registers all annotated services in a package.</p>
     * <pre><code>registry.registerAnnotatedServices("my.package", config);</code></pre>
     *
     * @param packageName the package to scan
     * @param config      the config to inject
     */
    List<Tuples.Tuple2<Service, UUID>> registerAnnotatedServices(String packageName, Config config);

    /**
     * <p>Reloads a specific service asynchronously.</p>
     * <pre><code>registry.reload("serviceA").join();</code></pre>
     *
     * @param id the service ID
     * @return a future that completes when reload is done
     */
    CompletableFuture<Void> reload(UUID id);

    /**
     * <p>Reloads all services asynchronously.</p>
     * <pre><code>registry.reloadAll().join();</code></pre>
     *
     * @return a future that completes when reload is done
     */
    CompletableFuture<Void> reloadAll();

    /**
     * <p>Gets the ID of a service by its name.</p>
     * <pre><code>UUID id = registry.getIdByName("serviceA");</code></pre>
     *
     * @param serviceA the service name
     * @return the service ID, or null if not found
     */
    UUID getIdByName(String serviceA);
}
