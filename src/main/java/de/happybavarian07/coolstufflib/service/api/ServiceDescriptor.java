package de.happybavarian07.coolstufflib.service.api;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ServiceDescriptor {
    private final UUID id;
    private final String serviceName;
    private final List<UUID> dependsOn;
    private final Duration startTimeout;
    private final Duration stopTimeout;

    /**
     * <p>Creates a new service descriptor.</p>
     * <pre><code>ServiceDescriptor desc = new ServiceDescriptor(UUID.randomUUID(), "ServiceName", List.of(UUID.randomUUID()), Duration.ofSeconds(5), Duration.ofSeconds(5));</code></pre>
     *
     * @param id           the service ID
     * @param serviceName  the service name
     * @param dependsOn    list of dependent service IDs
     * @param startTimeout startup timeout
     * @param stopTimeout  shutdown timeout
     */
    public ServiceDescriptor(UUID id, String serviceName, List<UUID> dependsOn, Duration startTimeout, Duration stopTimeout) {
        this.id = Objects.requireNonNull(id);
        this.serviceName = Objects.requireNonNull(serviceName);
        this.dependsOn = dependsOn == null ? Collections.emptyList() : List.copyOf(dependsOn);
        this.startTimeout = startTimeout == null ? Duration.ofSeconds(20) : startTimeout;
        this.stopTimeout = stopTimeout == null ? Duration.ofSeconds(10) : stopTimeout;
    }

    /**
     * <p>Returns the service ID.</p>
     * <pre><code>UUID id = desc.id();</code></pre>
     *
     * @return the service ID
     */
    public UUID id() {
        return id;
    }

    /**
     * <p>Returns the service name.</p>
     * <pre><code>String name = desc.serviceName();</code></pre>
     *
     * @return the service name
     */
    public String serviceName() {
        return serviceName;
    }

    /**
     * <p>Returns the list of dependent service IDs.</p>
     * <pre><code>List&lt;UUID&gt; deps = desc.dependsOn();</code></pre>
     *
     * @return list of dependencies
     */
    public List<UUID> dependsOn() {
        return dependsOn;
    }

    /**
     * <p>Returns the startup timeout.</p>
     * <pre><code>Duration timeout = desc.startTimeout();</code></pre>
     *
     * @return startup timeout
     */
    public Duration startTimeout() {
        return startTimeout;
    }

    /**
     * <p>Returns the shutdown timeout.</p>
     * <pre><code>Duration timeout = desc.stopTimeout();</code></pre>
     *
     * @return shutdown timeout
     */
    public Duration stopTimeout() {
        return stopTimeout;
    }
}
