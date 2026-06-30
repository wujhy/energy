package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryModulePollContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

class BatteryModulePollContextHolderTest {

    @AfterEach
    void cleanup() {
        BatteryModulePollContextHolder.clear();
    }

    @Test
    void shouldReturnNullWhenNoContextSet() {
        Assertions.assertNull(BatteryModulePollContextHolder.get());
    }

    @Test
    void shouldReturnContextAfterSet() {
        BatteryModulePollContext context = BatteryModulePollContext.builder()
                .pollBatchNo("batch-1")
                .cells(Collections.emptyList())
                .groups(Collections.emptyList())
                .build();

        BatteryModulePollContextHolder.set(context);

        Assertions.assertSame(context, BatteryModulePollContextHolder.get());
    }

    @Test
    void shouldReturnNullAfterClear() {
        BatteryModulePollContext context = BatteryModulePollContext.builder()
                .pollBatchNo("batch-1")
                .cells(Collections.emptyList())
                .groups(Collections.emptyList())
                .build();
        BatteryModulePollContextHolder.set(context);

        BatteryModulePollContextHolder.clear();

        Assertions.assertNull(BatteryModulePollContextHolder.get());
    }

    @Test
    void shouldNotThrowWhenClearWithoutPriorSet() {
        Assertions.assertDoesNotThrow(BatteryModulePollContextHolder::clear);
    }

    @Test
    void shouldIsolateContextBetweenThreads() throws InterruptedException {
        BatteryModulePollContext mainContext = BatteryModulePollContext.builder()
                .pollBatchNo("main-batch")
                .cells(Collections.emptyList())
                .groups(Collections.emptyList())
                .build();
        BatteryModulePollContextHolder.set(mainContext);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<BatteryModulePollContext> otherThreadContext = new AtomicReference<>();

        Thread otherThread = new Thread(() -> {
            otherThreadContext.set(BatteryModulePollContextHolder.get());
            latch.countDown();
        });
        otherThread.start();
        Assertions.assertTrue(latch.await(5, TimeUnit.SECONDS));

        Assertions.assertNull(otherThreadContext.get());
        Assertions.assertSame(mainContext, BatteryModulePollContextHolder.get());
    }

    @Test
    void shouldOverwritePreviousContext() {
        BatteryModulePollContext first = BatteryModulePollContext.builder()
                .pollBatchNo("first")
                .cells(Collections.emptyList())
                .groups(Collections.emptyList())
                .build();
        BatteryModulePollContext second = BatteryModulePollContext.builder()
                .pollBatchNo("second")
                .cells(Collections.emptyList())
                .groups(Collections.emptyList())
                .build();

        BatteryModulePollContextHolder.set(first);
        BatteryModulePollContextHolder.set(second);

        Assertions.assertSame(second, BatteryModulePollContextHolder.get());
    }
}
