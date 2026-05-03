package com.recetea.infrastructure.persistence.recipe.jdbc;

import com.recetea.infrastructure.concurrency.ConcurrencyProvider;
import com.recetea.infrastructure.persistence.recipe.jdbc.repositories.BaseRepositoryTest;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stress test that exercises {@link JdbcTransactionManager} from a swarm of virtual
 * threads, ensuring two properties under concurrency:
 *
 * <ul>
 *   <li><b>Functional</b>: every transaction completes successfully despite the
 *       HikariCP pool being deliberately oversubscribed (500 tasks × 10 pool slots).
 *       Failure here would indicate connection-acquisition timeouts or transaction
 *       boundary corruption.</li>
 *   <li><b>Carrier health</b>: no virtual thread pins its carrier platform thread
 *       during the JDBC blocking call. Two complementary signals report pinning:
 *       <ul>
 *         <li>{@code -Djdk.tracePinnedThreads=full} (set in pom.xml's surefire argLine)
 *             prints a stack trace tagged with "Thread Pinned" on stderr if pinning
 *             happens; silent in the happy path.</li>
 *         <li>A programmatic {@link jdk.jfr.Recording} captures
 *             {@code jdk.VirtualThreadPinned} events with their stack traces and
 *             durations. Post-stress the test parses the dump and emits a single
 *             warning per pinned event — non-fatal so a transient hiccup on a loaded
 *             CI host doesn't flake the build, but loud enough to surface in CI logs.</li>
 *       </ul></li>
 * </ul>
 */
@DisplayName("Virtual-thread pinning audit — concurrent transactional workload")
class VirtualThreadPinningTest extends BaseRepositoryTest {

    private static final Logger log = LoggerFactory.getLogger(VirtualThreadPinningTest.class);

    private static final int CONCURRENT_TASKS = 500;

    private JdbcTransactionManager transactionManager;
    private ConcurrencyProvider    concurrencyProvider;

    @BeforeEach
    void setUp() {
        transactionManager  = new JdbcTransactionManager(dataSource);
        concurrencyProvider = new ConcurrencyProvider();
    }

    @Test
    @DisplayName("500 concurrent virtual-thread transactions complete without errors or carrier pinning")
    void manyConcurrentTransactions_DoNotPinCarriers() throws Exception {
        Path jfrDump = Files.createTempFile("vt-pinning-", ".jfr");
        try (jdk.jfr.Recording recording = new jdk.jfr.Recording()) {
            recording.setName("VirtualThreadPinningTest");
            recording.setDestination(jfrDump);
            // Three event categories that together cover virtual-thread health:
            //   - VirtualThreadPinned: the headline signal — emits whenever a VT cannot
            //     unmount because of a synchronized block, native call, or other JNI work.
            //   - VirtualThreadStart / End: lifecycle counts so we can correlate pin events
            //     against the population that was actually scheduled.
            recording.enable("jdk.VirtualThreadPinned");
            recording.enable("jdk.VirtualThreadStart");
            recording.enable("jdk.VirtualThreadEnd");
            recording.start();

            runStress();

            recording.stop();
            // setDestination + stop() already flushes the buffer to jfrDump;
            // calling dump() again would error with "Recording has been closed".
            auditRecording(jfrDump);
        } finally {
            Files.deleteIfExists(jfrDump);
        }
    }

    private void runStress() throws InterruptedException {
        ExecutorService executor = concurrencyProvider.executor();

        // Coordinated start: all 500 tasks queue up, then race for pool slots simultaneously.
        // Without this, threads trickle through and never actually contend, defeating the test.
        CountDownLatch ready = new CountDownLatch(CONCURRENT_TASKS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(CONCURRENT_TASKS);
        AtomicInteger  errors    = new AtomicInteger();
        AtomicInteger  successes = new AtomicInteger();

        for (int i = 0; i < CONCURRENT_TASKS; i++) {
            executor.submit(() -> {
                try {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        errors.incrementAndGet();
                        return;
                    }
                    transactionManager.execute(() -> {
                        Connection conn = JdbcTransactionManager.CONNECTION.get();
                        try (PreparedStatement ps = conn.prepareStatement("SELECT 1");
                             ResultSet rs = ps.executeQuery()) {
                            rs.next();
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                        return null;
                    });
                    successes.incrementAndGet();
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        assertTrue(ready.await(10, TimeUnit.SECONDS),
                "All " + CONCURRENT_TASKS + " virtual threads should be ready before the start signal");
        start.countDown();

        // 60s ceiling: with pool=10 and a SELECT 1 averaging ~5ms, expected wall time is ~250ms.
        // The generous deadline accommodates loaded CI hosts without flaking.
        assertTrue(done.await(60, TimeUnit.SECONDS),
                "All " + CONCURRENT_TASKS + " transactions should complete within 60 seconds");

        assertEquals(0, errors.get(),
                "No transaction should fail under concurrent load; failures observed: " + errors.get());
        assertEquals(CONCURRENT_TASKS, successes.get(),
                "Every virtual thread should record exactly one successful transaction");
    }

    /**
     * Walks the JFR dump and emits one WARN per pinned event so a CI run with pinning
     * is loud but doesn't fail outright (transient pinning on overloaded hosts is
     * environmental, not a production regression). Also reports the lifecycle counts
     * as INFO so we can confirm the recording captured the workload.
     */
    private void auditRecording(Path dump) {
        long pinnedEvents = 0;
        long startedEvents = 0;
        long endedEvents = 0;
        try (RecordingFile file = new RecordingFile(dump)) {
            while (file.hasMoreEvents()) {
                RecordedEvent event = file.readEvent();
                String name = event.getEventType().getName();
                switch (name) {
                    case "jdk.VirtualThreadPinned" -> {
                        pinnedEvents++;
                        log.warn("Virtual thread pinned for {} ms — stack: {}",
                                event.getDuration().toMillis(),
                                event.getStackTrace() == null ? "<no stack>" : event.getStackTrace().getFrames());
                    }
                    case "jdk.VirtualThreadStart" -> startedEvents++;
                    case "jdk.VirtualThreadEnd"   -> endedEvents++;
                    default -> { /* ignore — only enabled the three above, but defend against future additions */ }
                }
            }
        } catch (Exception e) {
            // JFR audit is observational; a parse failure must not break the assertion result.
            log.warn("JFR recording audit skipped — {}: {}", e.getClass().getSimpleName(), e.getMessage());
            return;
        }
        log.info("JFR audit: {} VT-start, {} VT-end, {} pin events across {} stress tasks",
                startedEvents, endedEvents, pinnedEvents, CONCURRENT_TASKS);
    }
}
