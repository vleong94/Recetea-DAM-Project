package com.recetea.core.shared.application;

import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

/**
 * Caps the number of concurrent heavy I/O operations (typically DB transactions
 * holding a connection from a finite pool). Threads that arrive while all permits
 * are taken queue uninterruptibly until a slot frees, preventing HikariCP pool
 * timeouts under burst load from many virtual threads.
 *
 * {@code Semaphore.acquireUninterruptibly()} is intentional: virtual threads
 * waiting at this guard should park-and-resume rather than propagate interrupts,
 * since the caller's intent is "complete this work" not "abort if interrupted."
 *
 * Place this guard <em>around</em> the transaction boundary, not inside it: the
 * point is to throttle connection acquisition, not to lengthen the period a
 * connection is held while waiting for a sibling operation.
 */
public final class ConcurrencyGuard {

    private final Semaphore semaphore;

    public ConcurrencyGuard(int permits) {
        if (permits <= 0) throw new IllegalArgumentException("permits must be > 0, got " + permits);
        this.semaphore = new Semaphore(permits);
    }

    public <T> T run(Supplier<T> action) {
        semaphore.acquireUninterruptibly();
        try {
            return action.get();
        } finally {
            semaphore.release();
        }
    }
}
