package de.happybavarian07.coolstufflib.configstuff.advanced.event;

import de.happybavarian07.coolstufflib.configstuff.advanced.AdvancedInMemoryConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigEventBus.EventPriority;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ConfigEventBusShutdownTest {

    @Test
    void closingAConfigShutsDownItsEventBus() {
        AdvancedInMemoryConfig config = new AdvancedInMemoryConfig("closing");
        assertFalse(config.isClosed());
        assertFalse(config.getEventBus().isShutdown());

        config.close();

        assertTrue(config.isClosed());
        assertTrue(config.getEventBus().isShutdown());
    }

    @Test
    void closeIsIdempotent() {
        AdvancedInMemoryConfig config = new AdvancedInMemoryConfig("idempotent");
        config.close();
        config.close();
        assertTrue(config.getEventBus().isShutdown());
    }

    @Test
    void closingOneConfigLeavesTheOtherConfigUsable() {
        AdvancedInMemoryConfig closed = new AdvancedInMemoryConfig("closed");
        AdvancedInMemoryConfig open = new AdvancedInMemoryConfig("open");
        AtomicInteger calls = new AtomicInteger();
        open.getEventBus().subscribe(ConfigLifecycleEvent.class, event -> calls.incrementAndGet());

        closed.close();
        open.getEventBus().publish(ConfigLifecycleEvent.configSave(open));

        assertTrue(closed.getEventBus().isShutdown());
        assertFalse(open.getEventBus().isShutdown());
        assertEquals(1, calls.get());
        open.close();
    }

    @Test
    void asyncPublishingAfterShutdownIsDiscardedInsteadOfRejected() throws InterruptedException {
        AdvancedInMemoryConfig config = new AdvancedInMemoryConfig("async");
        ConfigEventBus bus = config.getEventBus();
        CountDownLatch latch = new CountDownLatch(1);
        bus.subscribe(ConfigLifecycleEvent.class, event -> latch.countDown(), EventPriority.NORMAL, true);

        config.close();
        bus.publishAsync(ConfigLifecycleEvent.configLoad(config));
        bus.publish(ConfigLifecycleEvent.configLoad(config));

        assertFalse(latch.await(200, TimeUnit.MILLISECONDS));
    }

    @Test
    void aReloadedConfigKeepsTheSameBusAndCloseStillShutsItDown() {
        AdvancedInMemoryConfig config = new AdvancedInMemoryConfig("reloaded");
        ConfigEventBus bus = config.getEventBus();

        config.save();
        config.reload();

        assertSame(bus, config.getEventBus());
        assertFalse(bus.isShutdown());

        config.close();

        assertSame(bus, config.getEventBus());
        assertTrue(bus.isShutdown());
    }
}
