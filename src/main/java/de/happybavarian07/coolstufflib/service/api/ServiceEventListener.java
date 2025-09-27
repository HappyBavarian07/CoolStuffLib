package de.happybavarian07.coolstufflib.service.api;

public interface ServiceEventListener {
    void onInit(ServiceEvent event);
    void onShutdown(ServiceEvent event);
    void onReload(ServiceEvent event);
    void onFail(ServiceEvent event);
}