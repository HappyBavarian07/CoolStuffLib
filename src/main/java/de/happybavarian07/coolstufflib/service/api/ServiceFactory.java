package de.happybavarian07.coolstufflib.service.api;

import java.util.concurrent.CompletableFuture;

public interface ServiceFactory<T extends Service> {
    /**
     * <p>Creates a new service instance using the provided registry.</p>
     * <pre><code>CompletableFuture&lt;MyService&gt; future = factory.create(registry);</code></pre>
     *
     * @param registry the service registry
     * @return a future with the created service instance
     */
    CompletableFuture<T> create(ServiceRegistry registry);
}
