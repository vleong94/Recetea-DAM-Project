package com.recetea.infrastructure.concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Centralized source for the application's I/O executor.
 *
 * Uses {@code Executors.newVirtualThreadPerTaskExecutor()} so every submitted task
 * runs on its own Java Virtual Thread. Virtual threads are cheap enough to be
 * created per-task and park without blocking their carrier platform thread,
 * making them ideal for JDBC, file-system, and PDF-generation workloads.
 *
 * Virtual threads created by this executor are daemon threads, so they never
 * prevent JVM shutdown.
 */
public final class ConcurrencyProvider {

    private final ExecutorService executor;

    public ConcurrencyProvider() {
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public ExecutorService executor() {
        return executor;
    }
}
