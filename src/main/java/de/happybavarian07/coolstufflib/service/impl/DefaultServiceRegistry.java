package de.happybavarian07.coolstufflib.service.impl;

import de.happybavarian07.coolstufflib.service.annotation.ServiceComponent;
import de.happybavarian07.coolstufflib.service.api.*;
import de.happybavarian07.coolstufflib.service.spigot.ServiceFailEvent;
import de.happybavarian07.coolstufflib.service.spigot.ServiceInitEvent;
import de.happybavarian07.coolstufflib.service.spigot.ServiceReloadEvent;
import de.happybavarian07.coolstufflib.service.spigot.ServiceShutdownEvent;
import de.happybavarian07.coolstufflib.service.util.ServiceComponentScanner;
import org.bukkit.Bukkit;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

public class DefaultServiceRegistry implements ServiceRegistry, ServiceMetrics {
    private final Map<String, ServiceDescriptor> descriptors = new ConcurrentHashMap<>();
    private final Map<String, Service> services = new ConcurrentHashMap<>();
    private final Map<String, ServiceState> states = new ConcurrentHashMap<>();
    private final Map<String, HealthCheck> healthChecks = new ConcurrentHashMap<>();
    private final List<ServiceLifecycleListener> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, ServiceFactory<?>> factories = new ConcurrentHashMap<>();
    private final Map<String, Long> startupTimes = new ConcurrentHashMap<>();
    private final Map<String, Long> reloadTimes = new ConcurrentHashMap<>();
    private final Map<String, Integer> healthFailures = new ConcurrentHashMap<>();

    @Override
    public void register(ServiceDescriptor descriptor, Service impl) {
        String id = descriptor.id();
        descriptors.put(id, descriptor);
        services.put(id, impl);
        states.put(id, ServiceState.REGISTERED);
    }

    @Override
    public Optional<Service> get(String id) {
        Service svc = services.get(id);
        if (svc == null && factories.containsKey(id)) {
            ServiceFactory<?> factory = factories.get(id);
            svc = factory.create(this).join();
            if (svc != null) {
                services.put(id, svc);
                states.put(id, ServiceState.REGISTERED);
            }
        }
        return Optional.ofNullable(svc);
    }

    @Override
    public <T extends Service> Optional<T> getAs(String id, Class<T> type) {
        Service svc = get(id).orElse(null);
        if (type.isInstance(svc)) return Optional.of(type.cast(svc));
        return Optional.empty();
    }

    @Override
    public ServiceState getState(String id) {
        return states.getOrDefault(id, ServiceState.UNREGISTERED);
    }

    @Override
    public Map<String, ServiceState> snapshotStates() {
        return new HashMap<>(states);
    }

    @Override
    public CompletableFuture<Void> startAll() {
        List<String> ids = new ArrayList<>(services.keySet());
        long start = System.nanoTime();
        return startServices(ids, true).thenRun(() -> {
            for (String id : ids) {
                startupTimes.put(id, (System.nanoTime() - start) / 1_000_000);
            }
        });
    }

    @Override
    public CompletableFuture<Void> stopAll() {
        List<String> ids = new ArrayList<>(services.keySet());
        Collections.reverse(ids);
        return stopServices(ids, true);
    }

    @Override
    public CompletableFuture<Void> start(String id) {
        long start = System.nanoTime();
        return startServices(List.of(id), false).thenRun(() -> {
            startupTimes.put(id, (System.nanoTime() - start) / 1_000_000);
        });
    }

    @Override
    public CompletableFuture<Void> stop(String id) {
        return stopServices(List.of(id), false);
    }

    @Override
    public CompletableFuture<Void> restart(String id) {
        return stop(id).thenCompose(v -> start(id));
    }

    @Override
    public void addListener(ServiceLifecycleListener l) {
        listeners.add(l);
    }

    @Override
    public void removeListener(ServiceLifecycleListener l) {
        listeners.remove(l);
    }

    @Override
    public void registerHealthCheck(String serviceId, HealthCheck check) {
        healthChecks.put(serviceId, check);
    }

    @Override
    public CompletableFuture<Boolean> isHealthy(String serviceId) {
        HealthCheck check = healthChecks.get(serviceId);
        if (check == null) return CompletableFuture.completedFuture(true);
        return check.healthy().thenApply(result -> {
            if (!result) healthFailures.merge(serviceId, 1, Integer::sum);
            return result;
        });
    }

    @Override
    public long getStartupTime(String serviceId) {
        return startupTimes.getOrDefault(serviceId, -1L);
    }

    @Override
    public long getReloadTime(String serviceId) {
        return reloadTimes.getOrDefault(serviceId, -1L);
    }

    @Override
    public Map<String, Long> getAllStartupTimes() {
        return new HashMap<>(startupTimes);
    }

    @Override
    public Map<String, Long> getAllReloadTimes() {
        return new HashMap<>(reloadTimes);
    }

    @Override
    public int getHealthCheckFailures(String serviceId) {
        return healthFailures.getOrDefault(serviceId, 0);
    }

    @Override
    public Map<String, Integer> getAllHealthCheckFailures() {
        return new HashMap<>(healthFailures);
    }

    @Override
    public <T extends Service> void registerFactory(ServiceDescriptor descriptor, ServiceFactory<T> factory) {
        String id = descriptor.id();
        descriptors.put(id, descriptor);
        factories.put(id, factory);
        states.put(id, ServiceState.REGISTERED);
    }

    @Override
    public <T extends Service> void registerAnnotatedServices(String packageName, Config config) {
        List<Class<?>> annotated = ServiceComponentScanner.findAnnotatedServices(packageName);
        for (Class<?> clazz : annotated) {
            ServiceComponent meta = clazz.getAnnotation(ServiceComponent.class);
            if (meta == null) continue;
            Service instance = ServiceComponentScanner.createInstance(clazz, this, config);
            ServiceDescriptor descriptor = new ServiceDescriptor(
                    meta.id(),
                    Arrays.asList(meta.dependsOn()),
                    Duration.ofMillis(meta.startTimeoutMillis()),
                    Duration.ofMillis(meta.stopTimeoutMillis())
            );
            register(descriptor, instance);
        }
    }

    private CompletableFuture<Void> startServices(List<String> ids, boolean all) {
        Set<String> started = new HashSet<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (String id : ids) {
            futures.add(startServiceRecursive(id, new HashSet<>(), started, all));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private CompletableFuture<Void> startServiceRecursive(String id, Set<String> path, Set<String> started, boolean all) {
        if (!path.add(id)) throw new IllegalStateException("Cyclic dependency detected for " + id);
        if (!started.add(id)) return CompletableFuture.completedFuture(null);
        ServiceDescriptor desc = descriptors.get(id);
        if (desc == null) throw new IllegalArgumentException("No descriptor for " + id);
        List<String> deps = desc.dependsOn();
        List<CompletableFuture<Void>> depFutures = new ArrayList<>();
        for (String dep : deps) {
            if (all || services.containsKey(dep)) {
                depFutures.add(startServiceRecursive(dep, new HashSet<>(path), started, all));
            }
        }
        return CompletableFuture.allOf(depFutures.toArray(new CompletableFuture[0]))
                .thenCompose(v -> {
                    ServiceState state = states.get(id);
                    if (state == ServiceState.RUNNING) return CompletableFuture.completedFuture(null);
                    states.put(id, ServiceState.INITIALIZING);
                    notifyListeners(id, ServiceState.REGISTERED, ServiceState.INITIALIZING, null);
                    Service svc = services.get(id);
                    CompletableFuture<Void> fut = svc.init();
                    if (!fireSpigotInitEvent(id)) {
                        states.put(id, ServiceState.FAILED);
                        notifyListeners(id, ServiceState.INITIALIZING, ServiceState.FAILED, null);
                        return CompletableFuture.failedFuture(new RuntimeException("Service init cancelled by Spigot event"));
                    }
                    return withTimeout(fut, desc.startTimeout())
                            .thenRun(() -> {
                                states.put(id, ServiceState.RUNNING);
                                notifyListeners(id, ServiceState.INITIALIZING, ServiceState.RUNNING, null);
                            })
                            .exceptionally(ex -> {
                                states.put(id, ServiceState.FAILED);
                                notifyListeners(id, ServiceState.INITIALIZING, ServiceState.FAILED, ex);
                                fireSpigotFailEvent(id, ex);
                                throw new CompletionException(ex);
                            });
                });
    }

    private CompletableFuture<Void> stopServices(List<String> ids, boolean all) {
        Set<String> stopped = new HashSet<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (String id : ids) {
            futures.add(stopServiceRecursive(id, new HashSet<>(), stopped, all));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private CompletableFuture<Void> stopServiceRecursive(String id, Set<String> path, Set<String> stopped, boolean all) {
        if (!path.add(id)) throw new IllegalStateException("Cyclic dependency detected for " + id);
        if (!stopped.add(id)) return CompletableFuture.completedFuture(null);
        ServiceDescriptor desc = descriptors.get(id);
        if (desc == null) throw new IllegalArgumentException("No descriptor for " + id);
        List<String> dependents = getDependents(id);
        List<CompletableFuture<Void>> depFutures = new ArrayList<>();
        for (String dep : dependents) {
            if (all || services.containsKey(dep)) {
                depFutures.add(stopServiceRecursive(dep, new HashSet<>(path), stopped, all));
            }
        }
        return CompletableFuture.allOf(depFutures.toArray(new CompletableFuture[0]))
                .thenCompose(v -> {
                    ServiceState state = states.get(id);
                    if (state == ServiceState.STOPPED || state == ServiceState.UNREGISTERED)
                        return CompletableFuture.completedFuture(null);
                    states.put(id, ServiceState.STOPPING);
                    notifyListeners(id, state, ServiceState.STOPPING, null);
                    Service svc = services.get(id);
                    CompletableFuture<Void> fut = svc.shutdown();
                    if (!fireSpigotShutdownEvent(id)) {
                        states.put(id, ServiceState.FAILED);
                        notifyListeners(id, ServiceState.STOPPING, ServiceState.FAILED, null);
                        return CompletableFuture.failedFuture(new RuntimeException("Service shutdown cancelled by Spigot event"));
                    }
                    return withTimeout(fut, desc.stopTimeout())
                            .thenRun(() -> {
                                states.put(id, ServiceState.STOPPED);
                                notifyListeners(id, ServiceState.STOPPING, ServiceState.STOPPED, null);
                            })
                            .exceptionally(ex -> {
                                states.put(id, ServiceState.FAILED);
                                notifyListeners(id, ServiceState.STOPPING, ServiceState.FAILED, ex);
                                fireSpigotFailEvent(id, ex);
                                throw new CompletionException(ex);
                            });
                });
    }

    private List<String> getDependents(String id) {
        List<String> dependents = new ArrayList<>();
        for (Map.Entry<String, ServiceDescriptor> e : descriptors.entrySet()) {
            if (e.getValue().dependsOn().contains(id)) dependents.add(e.getKey());
        }
        return dependents;
    }

    private CompletableFuture<Void> withTimeout(CompletableFuture<Void> fut, Duration timeout) {
        CompletableFuture<Void> timeoutFut = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                fut.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                timeoutFut.complete(null);
            } catch (Exception ex) {
                timeoutFut.completeExceptionally(ex);
            }
        });
        return timeoutFut;
    }

    private void notifyListeners(String id, ServiceState from, ServiceState to, Throwable failure) {
        for (ServiceLifecycleListener l : listeners) {
            l.onStateChange(id, from, to, failure);
        }
    }

    @Override
    public CompletableFuture<Void> reload(String id) {
        long start = System.nanoTime();
        Service svc = get(id).orElse(null);
        if (svc == null)
            return CompletableFuture.failedFuture(new IllegalArgumentException("Service not found: " + id));
        states.put(id, ServiceState.INITIALIZING);
        notifyListeners(id, ServiceState.RUNNING, ServiceState.INITIALIZING, null);
        CompletableFuture<Void> fut = svc.onReload();
        if (!fireSpigotReloadEvent(id)) {
            states.put(id, ServiceState.FAILED);
            notifyListeners(id, ServiceState.INITIALIZING, ServiceState.FAILED, null);
            return CompletableFuture.failedFuture(new RuntimeException("Service reload cancelled by Spigot event"));
        }
        return fut.thenRun(() -> {
            reloadTimes.put(id, (System.nanoTime() - start) / 1_000_000);
            states.put(id, ServiceState.RUNNING);
            notifyListeners(id, ServiceState.INITIALIZING, ServiceState.RUNNING, null);
        }).exceptionally(ex -> {
            states.put(id, ServiceState.FAILED);
            notifyListeners(id, ServiceState.INITIALIZING, ServiceState.FAILED, ex);
            fireSpigotFailEvent(id, ex);
            throw new CompletionException(ex);
        });
    }

    @Override
    public CompletableFuture<Void> reloadAll() {
        List<CompletableFuture<Void>> reloads = new ArrayList<>();
        long start = System.nanoTime();
        for (String id : services.keySet()) {
            reloads.add(reload(id).thenRun(() -> {
                reloadTimes.put(id, (System.nanoTime() - start) / 1_000_000);
            }));
        }
        return CompletableFuture.allOf(reloads.toArray(new CompletableFuture[0]));
    }

    private boolean fireSpigotInitEvent(String id) {
        if (Bukkit.getServer() == null) return true;
        ServiceInitEvent event = new ServiceInitEvent(id);
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

    private boolean fireSpigotShutdownEvent(String id) {
        if (Bukkit.getServer() == null) return true;
        ServiceShutdownEvent event = new ServiceShutdownEvent(id);
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

    private boolean fireSpigotReloadEvent(String id) {
        if (Bukkit.getServer() == null) return true;
        ServiceReloadEvent event = new ServiceReloadEvent(id);
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

    private void fireSpigotFailEvent(String id, Throwable failure) {
        if (Bukkit.getServer() == null) return;
        ServiceFailEvent event = new ServiceFailEvent(id, failure);
        Bukkit.getPluginManager().callEvent(event);
    }
}
