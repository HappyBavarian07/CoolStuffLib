package de.happybavarian07.coolstufflib.configstuff.advanced.event;

@FunctionalInterface
public interface ConfigEventListener<T extends ConfigEvent> {
    void onEvent(T event);
}
