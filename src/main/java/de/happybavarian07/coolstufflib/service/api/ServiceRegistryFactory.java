package de.happybavarian07.coolstufflib.service.api;

import de.happybavarian07.coolstufflib.service.impl.DefaultServiceRegistry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServiceRegistryFactory {
    public static Builder builder() {
        return new Builder();
    }
    public static class Builder {
        private ExecutorService executor = Executors.newCachedThreadPool();
        private boolean enforceUniqueNames = true;
        public Builder executor(ExecutorService executor) {
            this.executor = executor;
            return this;
        }
        public Builder enforceUniqueNames(boolean enforce) {
            this.enforceUniqueNames = enforce;
            return this;
        }
        public DefaultServiceRegistry build() {
            return new DefaultServiceRegistry(executor, enforceUniqueNames);
        }
    }
}

