package cc.storozhuk.requestlimit.internal;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

import cc.storozhuk.requestlimit.RequestLimit;
import cc.storozhuk.requestlimit.RequestLimitConfig;
import javaslang.control.Option;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * @author bstorozhuk
 */
public class SemaphoreBasedRequestLimitImpl implements RequestLimit {

    private static final String NAME_MUST_NOT_BE_NULL = "Name must not be null";
    private static final String CONFIG_MUST_NOT_BE_NULL = "RequestLimitConfig must not be null";

    private final String name;
    private final RequestLimitConfig requestLimitConfig;
    private final ScheduledExecutorService scheduler;
    private final Semaphore semaphore;
    private final SemaphoreBasedRequestLimitMetrics metrics;

    private ScheduledExecutorService configureScheduler() {
        ThreadFactory threadFactory = target -> {
            Thread thread = new Thread(target, "SchedulerForSemaphoreBasedRequestLimitImpl-" + name);
            thread.setDaemon(true);
            return thread;
        };
        return newSingleThreadScheduledExecutor(threadFactory);
    }

    private void scheduleLimitRefresh() {
        scheduler.scheduleAtFixedRate(
            this::refreshLimit,
            this.requestLimitConfig.getLimitRefreshPeriod().toNanos(),
            this.requestLimitConfig.getLimitRefreshPeriod().toNanos(),
            TimeUnit.NANOSECONDS
        );
    }

    public SemaphoreBasedRequestLimitImpl(final String name, final RequestLimitConfig requestLimitConfig) {
        this(name, requestLimitConfig, null);
    }

    public SemaphoreBasedRequestLimitImpl(String name, RequestLimitConfig requestLimitConfig,
                                          ScheduledExecutorService scheduler) {
        this.name = requireNonNull(name, NAME_MUST_NOT_BE_NULL);
        this.requestLimitConfig = requireNonNull(requestLimitConfig, CONFIG_MUST_NOT_BE_NULL);
        this.scheduler = Option.of(scheduler).getOrElse(this::configureScheduler);
        this.semaphore = new Semaphore(this.requestLimitConfig.getLimitForPeriod(), true);
        this.metrics = this.new SemaphoreBasedRequestLimitMetrics();

        scheduleLimitRefresh();
    }

    void refreshLimit() {
        semaphore.release(this.requestLimitConfig.getLimitForPeriod());
    }

    /**
     * Acquires a permit from this request limit, blocking until one is
     * available.
     * <p>
     * <p>If the current thread is {@linkplain Thread#interrupt interrupted}
     * while waiting for a permit then it won't throw {@linkplain InterruptedException},
     * but its interrupt status will be set.
     *
     * @return {@code true} if a permit was acquired and {@code false}
     * if the waiting time elapsed before a permit was acquired
     */
    @Override
    public boolean getPermission(final Duration timeoutDuration) {
        try {
            boolean success = semaphore.tryAcquire(timeoutDuration.toNanos(), TimeUnit.NANOSECONDS);
            return success;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Metrics getMetrics() {
        return this.metrics;
    }

    @Override
    public RequestLimitConfig getRequestLimitConfig() {
        return this.requestLimitConfig;
    }

    public SemaphoreBasedRequestLimitMetrics getDetailedMetrics() {
        return this.metrics;
    }

    public final class SemaphoreBasedRequestLimitMetrics implements Metrics {
        private SemaphoreBasedRequestLimitMetrics() {
        }

        /**
         * Returns the current number of permits available in this request limit
         * until the next refresh.
         * <p>
         * <p>This method is typically used for debugging and testing purposes.
         *
         * @return the number of permits available in this request limit until the next refresh.
         */
        public int getAvailablePermits() {
            return semaphore.availablePermits();
        }

        @Override
        public int getNumberOfWaitingThreads() {
            return semaphore.getQueueLength();
        }
    }
}
