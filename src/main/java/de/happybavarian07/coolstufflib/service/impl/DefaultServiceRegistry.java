package de.happybavarian07.coolstufflib.service.impl;

import de.happybavarian07.coolstufflib.service.annotation.ServiceComponent;
import de.happybavarian07.coolstufflib.service.api.*;
import de.happybavarian07.coolstufflib.service.event.ServiceFailEvent;
import de.happybavarian07.coolstufflib.service.event.ServiceInitEvent;
import de.happybavarian07.coolstufflib.service.event.ServiceReloadEvent;
import de.happybavarian07.coolstufflib.service.event.ServiceShutdownEvent;
import de.happybavarian07.coolstufflib.service.exception.ServiceDependencyCycleException;
import de.happybavarian07.coolstufflib.service.exception.DuplicateServiceNameException;
import de.happybavarian07.coolstufflib.service.exception.ServiceDescriptorNotFoundException;
import de.happybavarian07.coolstufflib.service.exception.ServiceIdInjectionException;
import de.happybavarian07.coolstufflib.service.exception.ServiceRegistrationException;
import de.happybavarian07.coolstufflib.service.util.ServiceComponentScanner;
import de.happybavarian07.coolstufflib.service.util.Tuples;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

public class DefaultServiceRegistry implements ServiceRegistry, ServiceMetrics, ServiceManagementAPI {
    private final Map<UUID, ServiceDescriptor> descriptors = new ConcurrentHashMap<>();
    private final Map<UUID, Service> services = new ConcurrentHashMap<>();
    private final Map<String, UUID> nameToId = new ConcurrentHashMap<>();
    private final Map<UUID, ServiceState> states = new ConcurrentHashMap<>();
    private final Map<UUID, HealthCheck> healthChecks = new ConcurrentHashMap<>();
    private final List<ServiceLifecycleListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<UUID, ServiceFactory<?>> factories = new ConcurrentHashMap<>();
    private final Map<UUID, Long> startupTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Long> reloadTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> healthFailures = new ConcurrentHashMap<>();
    private final boolean enforceUniqueNames;
    private ExecutorService executor;

    public DefaultServiceRegistry() {
        this(Executors.newCachedThreadPool(), true);
    }

    public DefaultServiceRegistry(ExecutorService externalExecutor, boolean enforceUniqueNames) {
        this.executor = externalExecutor;
        this.enforceUniqueNames = enforceUniqueNames;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService externalExecutor) {
        this.executor = externalExecutor;
    }

    private void injectIdAndNameFields(Service service, UUID uuid, String name) {
        boolean idInjected = false;
        try {
            var idField = service.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            Object currentId = idField.get(service);
            if (currentId == null) {
                idField.set(service, uuid);
            }
            idInjected = true;
        } catch (Exception e) {
            try {
                var idField = service.getClass().getDeclaredField("id");
                idField.setAccessible(true);
                Object currentId = idField.get(service);
                if (currentId == null) {
                    throw new ServiceIdInjectionException("Failed to inject id into service: " + service.getClass().getName(), e);
                }
            } catch (Exception inner) {
                throw new ServiceIdInjectionException("Service is missing a valid id field: " + service.getClass().getName(), e);
            }
        }
        try {
            var nameField = service.getClass().getDeclaredField("name");
            nameField.setAccessible(true);
            Object currentName = nameField.get(service);
            if (currentName == null) {
                nameField.set(service, name);
            }
        } catch (Exception ignored) { }
    }

    @Override
    public Tuples.Tuple2<Service, UUID> register(ServiceDescriptor descriptor, Service impl, UUID uuid) {
        uuid = uuid == null ? UUID.randomUUID() : uuid;
        injectIdAndNameFields(impl, uuid, descriptor.serviceName());
        registerInternal(descriptor, impl, uuid);
        return Tuples.of(impl, uuid);
    }

    private void registerInternal(ServiceDescriptor descriptor, Service impl, UUID uuid) {
        String name = descriptor.serviceName();
        if (enforceUniqueNames && nameToId.containsKey(name)) {
            throw new DuplicateServiceNameException("Duplicate service name: " + name);
        }
        descriptors.put(uuid, descriptor);
        services.put(uuid, impl);
        nameToId.put(name, uuid);
        states.put(uuid, ServiceState.REGISTERED);
        autoDetectOrphanedServices();
        autoDetectDuplicateNames();
        autoDetectCyclicDependencies();
    }

    private void setServiceUuidIfPossible(Service service, UUID uuid) {
        try {
            Method setter = service.getClass().getMethod("setUuid", UUID.class);
            setter.invoke(service, uuid);
        } catch (Exception ignored) {
        }
    }

    @Override
    public <T extends Service> Tuples.Tuple2<T, UUID> registerFactory(ServiceDescriptor descriptor, ServiceFactory<T> factory, UUID uuid) {
        uuid = uuid == null ? UUID.randomUUID() : uuid;
        T instance = factory.create(this).join();
        injectIdAndNameFields(instance, uuid, descriptor.serviceName());
        registerInternal(descriptor, instance, uuid);
        factories.put(uuid, factory);
        return Tuples.of(instance, uuid);
    }

    @Override
    public List<Tuples.Tuple2<Service, UUID>> registerAnnotatedServices(String packageName, Config config) {
        List<Class<?>> annotated = ServiceComponentScanner.findAnnotatedServices(packageName);
        List<Tuples.Tuple2<Service, UUID>> result = new ArrayList<>();
        for (Class<?> clazz : annotated) {
            ServiceComponent meta = clazz.getAnnotation(ServiceComponent.class);
            if (meta == null) continue;
            UUID uuid = meta.uuid().isEmpty() ? UUID.randomUUID() : UUID.fromString(meta.uuid());
            String name = meta.serviceName();
            Service instance = ServiceComponentScanner.createInstance(clazz, this, config);
            injectIdAndNameFields(instance, uuid, name);
            ServiceDescriptor descriptor = new ServiceDescriptor(uuid, name, List.of(),
                    Duration.ofMillis(meta.startTimeoutMillis()), Duration.ofMillis(meta.stopTimeoutMillis()));
            registerInternal(descriptor, instance, uuid);
            result.add(Tuples.of(instance, uuid));
        }
        return result;
    }

    @Override
    public Optional<Service> get(UUID id) {
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
    public Optional<Service> getByName(String serviceName) {
        UUID id = nameToId.get(serviceName);
        return id == null ? Optional.empty() : get(id);
    }

    @Override
    public String getNameById(UUID id) {
        return descriptors.containsKey(id) ? descriptors.get(id).serviceName() : null;
    }

    @Override
    public <T extends Service> Optional<T> getAs(UUID id, Class<T> type) {
        Service svc = get(id).orElse(null);
        if (type.isInstance(svc)) return Optional.of(type.cast(svc));
        return Optional.empty();
    }

    @Override
    public <T extends Service> Optional<T> getAsByName(String serviceName, Class<T> type) {
        Service svc = getByName(serviceName).orElse(null);
        if (type.isInstance(svc)) return Optional.of(type.cast(svc));
        return Optional.empty();
    }

    @Override
    public ServiceState getState(UUID id) {
        return states.getOrDefault(id, ServiceState.UNREGISTERED);
    }

    @Override
    public ServiceState getStateByName(String serviceName) {
        UUID id = nameToId.get(serviceName);
        return id == null ? ServiceState.UNREGISTERED : getState(id);
    }

    @Override
    public Map<UUID, ServiceState> snapshotStates() {
        return new HashMap<>(states);
    }

    @Override
    public Map<String, ServiceState> snapshotStatesByName() {
        Map<String, ServiceState> result = new HashMap<>();
        for (Map.Entry<String, UUID> entry : nameToId.entrySet()) {
            result.put(entry.getKey(), states.getOrDefault(entry.getValue(), ServiceState.UNREGISTERED));
        }
        return result;
    }

    @Override
    public CompletableFuture<Void> startAll() {
        List<UUID> ids = new ArrayList<>(services.keySet());
        long start = System.nanoTime();
        return startServices(ids, true).thenRun(() -> {
            for (UUID id : ids) {
                startupTimes.put(id, (System.nanoTime() - start) / 1_000_000);
            }
        });
    }

    @Override
    public CompletableFuture<Void> stopAll() {
        List<UUID> ids = new ArrayList<>(services.keySet());
        Collections.reverse(ids);
        return stopServices(ids, true);
    }

    @Override
    public CompletableFuture<Void> start(UUID id) {
        long start = System.nanoTime();
        return startServices(List.of(id), false).thenRun(() -> {
            startupTimes.put(id, (System.nanoTime() - start) / 1_000_000);
        });
    }

    @Override
    public CompletableFuture<Void> stop(UUID id) {
        return stopServices(List.of(id), false);
    }

    @Override
    public CompletableFuture<Void> restart(UUID id) {
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
    public void registerHealthCheck(UUID serviceId, HealthCheck check) {
        healthChecks.put(serviceId, check);
    }

    @Override
    public CompletableFuture<Boolean> isHealthy(UUID serviceId) {
        HealthCheck check = healthChecks.get(serviceId);
        if (check == null) return CompletableFuture.completedFuture(true);
        return check.healthy().thenApply(result -> {
            if (!result) healthFailures.merge(serviceId, 1, Integer::sum);
            return result;
        });
    }

    @Override
    public long getStartupTime(UUID serviceId) {
        return startupTimes.getOrDefault(serviceId, -1L);
    }

    @Override
    public long getReloadTime(UUID serviceId) {
        return reloadTimes.getOrDefault(serviceId, -1L);
    }

    @Override
    public Map<UUID, Long> getAllStartupTimes() {
        return new HashMap<>(startupTimes);
    }

    @Override
    public Map<UUID, Long> getAllReloadTimes() {
        return new HashMap<>(reloadTimes);
    }

    @Override
    public int getHealthCheckFailures(UUID serviceId) {
        return healthFailures.getOrDefault(serviceId, 0);
    }

    @Override
    public Map<UUID, Integer> getAllHealthCheckFailures() {
        return new HashMap<>(healthFailures);
    }

    private CompletableFuture<Void> startServices(List<UUID> ids, boolean all) {
        Set<UUID> started = new HashSet<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (UUID id : ids) {
            futures.add(startServiceRecursive(id, new HashSet<>(), started, all));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private CompletableFuture<Void> startServiceRecursive(UUID id, Set<UUID> path, Set<UUID> started, boolean all) {
        if (!path.add(id)) throw new ServiceDependencyCycleException("Cyclic dependency detected for " + id);
        if (!started.add(id)) return CompletableFuture.completedFuture(null);
        ServiceDescriptor desc = descriptors.get(id);
        if (desc == null) throw new ServiceDescriptorNotFoundException("No descriptor for " + id);
        List<UUID> deps = desc.dependsOn();
        List<CompletableFuture<Void>> depFutures = new ArrayList<>();
        for (UUID dep : deps) {
            if (all || services.containsKey(dep)) {
                depFutures.add(startServiceRecursive(dep, new HashSet<>(path), started, all));
            }
        }
        return CompletableFuture.allOf(depFutures.toArray(new CompletableFuture[0]))
                .thenCompose(v -> {
                    ServiceState state = states.get(id);
                    if (state == ServiceState.RUNNING) return CompletableFuture.completedFuture(null);
                    states.put(id, ServiceState.INITIALIZING);
                    Service svc = services.get(id);
                    notifyListeners(id, svc.serviceName(), ServiceState.REGISTERED, ServiceState.INITIALIZING, null);
                    CompletableFuture<Void> fut = svc.init();
                    if (!fireSpigotInitEvent(id)) {
                        states.put(id, ServiceState.FAILED);
                        notifyListeners(id, svc.serviceName(), ServiceState.INITIALIZING, ServiceState.FAILED, null);
                        return CompletableFuture.failedFuture(new RuntimeException("Service init cancelled by Spigot event"));
                    }
                    return withTimeout(fut, desc.startTimeout())
                            .thenRun(() -> {
                                states.put(id, ServiceState.RUNNING);
                                notifyListeners(id, svc.serviceName(), ServiceState.INITIALIZING, ServiceState.RUNNING, null);
                            })
                            .exceptionally(ex -> {
                                states.put(id, ServiceState.FAILED);
                                notifyListeners(id, svc.serviceName(), ServiceState.INITIALIZING, ServiceState.FAILED, ex);
                                fireSpigotFailEvent(id, ex);
                                throw new CompletionException(ex);
                            });
                });
    }

    private CompletableFuture<Void> stopServices(List<UUID> ids, boolean all) {
        Set<UUID> stopped = new HashSet<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (UUID id : ids) {
            futures.add(stopServiceRecursive(id, new HashSet<>(), stopped, all));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private CompletableFuture<Void> stopServiceRecursive(UUID id, Set<UUID> path, Set<UUID> stopped, boolean all) {
        if (!path.add(id)) throw new ServiceDependencyCycleException("Cyclic dependency detected for " + id);
        if (!stopped.add(id)) return CompletableFuture.completedFuture(null);
        ServiceDescriptor desc = descriptors.get(id);
        if (desc == null) throw new ServiceDescriptorNotFoundException("No descriptor for " + id);
        List<UUID> dependents = getDependents(id);
        List<CompletableFuture<Void>> depFutures = new ArrayList<>();
        for (UUID dep : dependents) {
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
                    Service svc = services.get(id);
                    notifyListeners(id, svc.serviceName(), state, ServiceState.STOPPING, null);
                    CompletableFuture<Void> fut = svc.shutdown();
                    if (!fireSpigotShutdownEvent(id)) {
                        states.put(id, ServiceState.FAILED);
                        notifyListeners(id, svc.serviceName(), ServiceState.STOPPING, ServiceState.FAILED, null);
                        return CompletableFuture.failedFuture(new RuntimeException("Service shutdown cancelled by Spigot event"));
                    }
                    return withTimeout(fut, desc.stopTimeout())
                            .thenRun(() -> {
                                states.put(id, ServiceState.STOPPED);
                                notifyListeners(id, svc.serviceName(), ServiceState.STOPPING, ServiceState.STOPPED, null);
                            })
                            .exceptionally(ex -> {
                                states.put(id, ServiceState.FAILED);
                                notifyListeners(id, svc.serviceName(), ServiceState.STOPPING, ServiceState.FAILED, ex);
                                fireSpigotFailEvent(id, ex);
                                throw new CompletionException(ex);
                            });
                });
    }

    private List<UUID> getDependents(UUID id) {
        List<UUID> dependents = new ArrayList<>();
        for (Map.Entry<UUID, ServiceDescriptor> e : descriptors.entrySet()) {
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

    private void notifyListeners(UUID id, String serviceName, ServiceState from, ServiceState to, Throwable failure) {
        for (ServiceLifecycleListener l : listeners) {
            l.onStateChange(id, serviceName, from, to, failure);
        }
    }

    @Override
    public CompletableFuture<Void> reload(UUID id) {
        long start = System.nanoTime();
        Service svc = get(id).orElse(null);
        if (svc == null)
            return CompletableFuture.failedFuture(new IllegalArgumentException("Service not found: " + id));
        states.put(id, ServiceState.INITIALIZING);
        notifyListeners(id, svc.serviceName(), ServiceState.RUNNING, ServiceState.INITIALIZING, null);
        CompletableFuture<Void> fut = svc.onReload();
        if (!fireSpigotReloadEvent(id)) {
            states.put(id, ServiceState.FAILED);
            notifyListeners(id, svc.serviceName(), ServiceState.INITIALIZING, ServiceState.FAILED, null);
            return CompletableFuture.failedFuture(new RuntimeException("Service reload cancelled by Spigot event"));
        }
        return fut.thenRun(() -> {
            reloadTimes.put(id, (System.nanoTime() - start) / 1_000_000);
            states.put(id, ServiceState.RUNNING);
            notifyListeners(id, svc.serviceName(), ServiceState.INITIALIZING, ServiceState.RUNNING, null);
        }).exceptionally(ex -> {
            states.put(id, ServiceState.FAILED);
            notifyListeners(id, svc.serviceName(), ServiceState.INITIALIZING, ServiceState.FAILED, ex);
            fireSpigotFailEvent(id, ex);
            throw new CompletionException(ex);
        });
    }

    @Override
    public CompletableFuture<Void> reloadAll() {
        List<CompletableFuture<Void>> reloadFutures = new ArrayList<>();
        long start = System.nanoTime();
        for (Map.Entry<UUID, Service> entry : services.entrySet()) {
            CompletableFuture<Void> reloadFuture = entry.getValue().onReload();
            reloadFutures.add(reloadFuture.thenRun(() -> {
                reloadTimes.put(entry.getKey(), (System.nanoTime() - start) / 1_000_000);
            }));
        }
        return CompletableFuture.allOf(reloadFutures.toArray(new CompletableFuture[0]));
    }

    @Override
    public UUID getIdByName(String serviceA) {
        return nameToId.get(serviceA);
    }

    private boolean fireSpigotInitEvent(UUID id) {
        if (Bukkit.getServer() == null) return true;
        ServiceInitEvent event = new ServiceInitEvent(id, getNameById(id));
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

    private boolean fireSpigotShutdownEvent(UUID id) {
        if (Bukkit.getServer() == null) return true;
        ServiceShutdownEvent event = new ServiceShutdownEvent(id, getNameById(id));
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

    private boolean fireSpigotReloadEvent(UUID id) {
        if (Bukkit.getServer() == null) return true;
        ServiceReloadEvent event = new ServiceReloadEvent(id, getNameById(id));
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

    private void fireSpigotFailEvent(UUID id, Throwable failure) {
        if (Bukkit.getServer() == null) return;
        ServiceFailEvent event = new ServiceFailEvent(id, getNameById(id), failure);
        Bukkit.getPluginManager().callEvent(event);
    }

    @Override
    public Map<UUID, ServiceState> getServiceStates() {
        return snapshotStates();
    }

    @Override
    public Optional<ServiceState> getServiceState(UUID id) {
        return Optional.ofNullable(getState(id));
    }

    @Override
    public CompletableFuture<Void> restartService(UUID id) {
        return restart(id);
    }

    @Override
    public CompletableFuture<Void> reloadService(UUID id) {
        return reload(id);
    }

    @Override
    public CompletableFuture<Void> reloadAllServices() {
        return reloadAll();
    }

    @Override
    public Map<UUID, Long> getStartupTimes() {
        return getAllStartupTimes();
    }

    @Override
    public Map<UUID, Long> getReloadTimes() {
        return getAllReloadTimes();
    }

    @Override
    public Map<UUID, Integer> getHealthFailures() {
        return getAllHealthCheckFailures();
    }

    // Auto-detection features
    private void autoDetectOrphanedServices() {
        for (UUID id : descriptors.keySet()) {
            if (!states.containsKey(id)) {
                states.put(id, ServiceState.UNREGISTERED);
            }
        }
    }

    private void autoDetectDuplicateNames() {
        Set<String> seen = new HashSet<>();
        for (String name : nameToId.keySet()) {
            if (!seen.add(name)) {
                throw new DuplicateServiceNameException("Duplicate service name detected: " + name);
            }
        }
    }

    private void autoDetectCyclicDependencies() {
        Set<UUID> visited = new HashSet<>();
        Set<UUID> stack = new HashSet<>();
        for (UUID id : descriptors.keySet()) {
            if (!visited.contains(id)) {
                detectCycle(id, visited, stack);
            }
        }
    }

    private void detectCycle(UUID id, Set<UUID> visited, Set<UUID> stack) {
        if (stack.contains(id)) {
            throw new ServiceDependencyCycleException("Cyclic dependency detected for " + id);
        }
        if (visited.contains(id)) return;
        visited.add(id);
        stack.add(id);
        ServiceDescriptor desc = descriptors.get(id);
        if (desc != null) {
            for (UUID dep : desc.dependsOn()) {
                detectCycle(dep, visited, stack);
            }
        }
        stack.remove(id);
    }
}

