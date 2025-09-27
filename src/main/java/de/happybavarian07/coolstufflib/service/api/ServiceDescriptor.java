package de.happybavarian07.coolstufflib.service.api;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ServiceDescriptor {
    private final String id;
    private final List<String> dependsOn;
    private final Duration startTimeout;
    private final Duration stopTimeout;

    /**
     * <p>Creates a new service descriptor.</p>
     * <pre><code>ServiceDescriptor desc = new ServiceDescriptor("id", List.of("dep"), Duration.ofSeconds(5), Duration.ofSeconds(5));</code></pre>
     *
     * @param id           the service ID
     * @param dependsOn    list of dependent service IDs
     * @param startTimeout startup timeout
     * @param stopTimeout  shutdown timeout
     */
    public ServiceDescriptor(String id, List<String> dependsOn, Duration startTimeout, Duration stopTimeout) {
        this.id = Objects.requireNonNull(id);
        this.dependsOn = dependsOn == null ? Collections.emptyList() : List.copyOf(dependsOn);
        this.startTimeout = startTimeout == null ? Duration.ofSeconds(20) : startTimeout;
        this.stopTimeout = stopTimeout == null ? Duration.ofSeconds(10) : stopTimeout;
    }

    /**
     * <p>Returns the service ID.</p>
     * <pre><code>String id = desc.id();</code></pre>
     *
     * @return the service ID
     */
    public String id() {
        return id;
    }

    /**
     * <p>Returns the list of dependent service IDs.</p>
     * <pre><code>List&lt;String&gt; deps = desc.dependsOn();</code></pre>
     *
     * @return list of dependencies
     */
    public List<String> dependsOn() {
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
