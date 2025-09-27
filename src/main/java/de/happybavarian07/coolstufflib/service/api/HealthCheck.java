package de.happybavarian07.coolstufflib.service.api;

import java.util.concurrent.CompletableFuture;

public interface HealthCheck {
    /**
     * <p>Checks if the service is healthy asynchronously.</p>
     * <pre><code>CompletableFuture&lt;Boolean&gt; healthy = healthCheck.healthy();</code></pre>
     *
     * @return a future with the health status
     */
    CompletableFuture<Boolean> healthy();
}
