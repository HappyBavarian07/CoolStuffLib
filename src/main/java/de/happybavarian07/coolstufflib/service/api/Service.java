package de.happybavarian07.coolstufflib.service.api;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Service {
    /**
     * <p>Returns the unique identifier for this service.</p>
     * <pre><code>String id = service.id();</code></pre>
     * <pre><code>UUID id = service.id();</code></pre>
     * @return the service ID
     */
    UUID id();

    /**
     * <p>Returns the human-readable name of the service.</p>
     * <pre><code>String name = service.serviceName();</code></pre>
     *
     * @return the service name
     */
    String serviceName();
    /**
     * <p>Initializes the service asynchronously.</p>
     * <pre><code>service.init().join();</code></pre>
     *
     * @return a future that completes when initialization is done
     */
    CompletableFuture<Void> init();

    /**
     * <p>Shuts down the service asynchronously.</p>
     * <pre><code>service.shutdown().join();</code></pre>
     *
     * @return a future that completes when shutdown is done
     */
    CompletableFuture<Void> shutdown();

    /**
     * <p>Reloads the service asynchronously. Default implementation does nothing.</p>
     * <pre><code>service.onReload().join();</code></pre>
     *
     * @return a future that completes when reload is done
     */
    default CompletableFuture<Void> onReload() {
        return CompletableFuture.completedFuture(null);
    }
}
