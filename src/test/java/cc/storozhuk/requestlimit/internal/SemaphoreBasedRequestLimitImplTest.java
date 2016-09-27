package cc.storozhuk.requestlimit.internal;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.FIVE_HUNDRED_MILLISECONDS;
import static java.lang.Thread.State.RUNNABLE;
import static java.lang.Thread.State.TERMINATED;
import static java.lang.Thread.State.TIMED_WAITING;
import static java.time.Duration.ZERO;
import static javaslang.control.Try.run;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cc.storozhuk.requestlimit.RequestLimit;
import cc.storozhuk.requestlimit.RequestLimitConfig;
import com.jayway.awaitility.core.ConditionFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author bstorozhuk
 */
public class SemaphoreBasedRequestLimitImplTest {

    private static final int LIMIT = 2;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REFRESH_PERIOD = Duration.ofMillis(100);
    private static final String CONFIG_MUST_NOT_BE_NULL = "RequestLimitConfig must not be null";
    private static final String NAME_MUST_NOT_BE_NULL = "Name must not be null";
    private static final Object O = new Object();

    private RequestLimitConfig config;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void init() {
        config = RequestLimitConfig.builder()
            .timeoutDuration(TIMEOUT)
            .limitRefreshPeriod(REFRESH_PERIOD)
            .limitForPeriod(LIMIT)
            .build();
    }

    @Test
    public void requestLimitCreationWithProvidedScheduler() throws Exception {
        ScheduledExecutorService scheduledExecutorService = mock(ScheduledExecutorService.class);
        RequestLimitConfig configSpy = spy(config);
        SemaphoreBasedRequestLimitImpl limit = new SemaphoreBasedRequestLimitImpl("test", configSpy, scheduledExecutorService);

        ArgumentCaptor<Runnable> refreshLimitRunnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduledExecutorService)
            .scheduleAtFixedRate(
                refreshLimitRunnableCaptor.capture(),
                eq(config.getLimitRefreshPeriod().toNanos()),
                eq(config.getLimitRefreshPeriod().toNanos()),
                eq(TimeUnit.NANOSECONDS)
            );

        Runnable refreshLimitRunnable = refreshLimitRunnableCaptor.getValue();

        assertThat(limit.getPermission(ZERO)).isTrue();
        assertThat(limit.getPermission(ZERO)).isTrue();
        assertThat(limit.getPermission(ZERO)).isFalse();

        Thread.sleep(REFRESH_PERIOD.toMillis() * 2);
        verify(configSpy, times(1)).getLimitForPeriod();

        refreshLimitRunnable.run();

        verify(configSpy, times(2)).getLimitForPeriod();

        assertThat(limit.getPermission(ZERO)).isTrue();
        assertThat(limit.getPermission(ZERO)).isTrue();
        assertThat(limit.getPermission(ZERO)).isFalse();
    }

    @Test
    public void requestLimitCreationWithDefaultScheduler() throws Exception {
        SemaphoreBasedRequestLimitImpl limit = new SemaphoreBasedRequestLimitImpl("test", config);
        awaitImpatiently().atMost(FIVE_HUNDRED_MILLISECONDS)
            .until(() -> limit.getPermission(ZERO), equalTo(false));
        awaitImpatiently().atMost(110, TimeUnit.MILLISECONDS)
            .until(() -> limit.getPermission(ZERO), equalTo(true));
    }

    @Test
    public void getPermissionAndMetrics() throws Exception {
        ScheduledExecutorService scheduledExecutorService = mock(ScheduledExecutorService.class);
        RequestLimitConfig configSpy = spy(config);
        SemaphoreBasedRequestLimitImpl limit = new SemaphoreBasedRequestLimitImpl("test", configSpy, scheduledExecutorService);
        SemaphoreBasedRequestLimitImpl.SemaphoreBasedRequestLimitMetrics detailedMetrics = limit.getDetailedMetrics();

        SynchronousQueue synchronousQueue = new SynchronousQueue();
        Thread thread = new Thread(() -> {
            run(() -> {
                for (int i = 0; i < LIMIT; i++) {
                    System.out.println("SLAVE -> WAITING FOR MASTER");
                    synchronousQueue.put(O);
                    System.out.println("SLAVE -> HAVE COMMAND FROM MASTER");
                    limit.getPermission(TIMEOUT);
                    System.out.println("SLAVE -> ACQUIRED PERMISSION");
                }
                System.out.println("SLAVE -> LAST PERMISSION ACQUIRE");
                limit.getPermission(TIMEOUT);
                System.out.println("SLAVE -> I'M DONE");
            });
        });
        thread.setDaemon(true);
        thread.start();

        for (int i = 0; i < LIMIT; i++) {
            System.out.println("MASTER -> TAKE PERMISSION");
            synchronousQueue.take();
        }

        System.out.println("MASTER -> CHECK IF SLAVE IS WAITING FOR PERMISSION");
        awaitImpatiently()
            .atMost(100, TimeUnit.MILLISECONDS).until(detailedMetrics::getAvailablePermits, equalTo(0));
        System.out.println("MASTER -> SLAVE CONSUMED ALL PERMISSIONS");
        awaitImpatiently()
            .atMost(2, TimeUnit.SECONDS).until(thread::getState, equalTo(TIMED_WAITING));
        assertThat(detailedMetrics.getAvailablePermits()).isEqualTo(0);
        System.out.println("MASTER -> SLAVE WAS WAITING");

        limit.refreshLimit();
        awaitImpatiently()
            .atMost(100, TimeUnit.MILLISECONDS).until(detailedMetrics::getAvailablePermits, equalTo(1));
        awaitImpatiently()
            .atMost(2, TimeUnit.SECONDS).until(thread::getState, equalTo(TERMINATED));
        assertThat(detailedMetrics.getAvailablePermits()).isEqualTo(1);
    }

    @Test
    public void getPermissionInterruption() throws Exception {
        ScheduledExecutorService scheduledExecutorService = mock(ScheduledExecutorService.class);
        RequestLimitConfig configSpy = spy(config);
        SemaphoreBasedRequestLimitImpl limit = new SemaphoreBasedRequestLimitImpl("test", configSpy, scheduledExecutorService);
        limit.getPermission(ZERO);
        limit.getPermission(ZERO);

        Thread thread = new Thread(() -> {
            limit.getPermission(TIMEOUT);
            while (true) {
                Function.identity().apply(1);
            }
        });
        thread.setDaemon(true);
        thread.start();

        awaitImpatiently()
            .atMost(2, TimeUnit.SECONDS).until(thread::getState, equalTo(TIMED_WAITING));

        thread.interrupt();

        awaitImpatiently()
            .atMost(2, TimeUnit.SECONDS).until(thread::getState, equalTo(RUNNABLE));
        assertThat(thread.isInterrupted()).isTrue();
    }

    @Test
    public void getName() throws Exception {
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        SemaphoreBasedRequestLimitImpl limit = new SemaphoreBasedRequestLimitImpl("test", config, scheduler);
        assertThat(limit.getName()).isEqualTo("test");
    }

    @Test
    public void getMetrics() throws Exception {
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        SemaphoreBasedRequestLimitImpl limit = new SemaphoreBasedRequestLimitImpl("test", config, scheduler);
        RequestLimit.Metrics metrics = limit.getMetrics();
        assertThat(metrics.getNumberOfWaitingThreads()).isEqualTo(0);
    }

    @Test
    public void getRequestLimitConfig() throws Exception {
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        SemaphoreBasedRequestLimitImpl limit = new SemaphoreBasedRequestLimitImpl("test", config, scheduler);
        assertThat(limit.getRequestLimitConfig()).isEqualTo(config);
    }

    @Test
    public void getDetailedMetrics() throws Exception {
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        SemaphoreBasedRequestLimitImpl limit = new SemaphoreBasedRequestLimitImpl("test", config, scheduler);
        SemaphoreBasedRequestLimitImpl.SemaphoreBasedRequestLimitMetrics metrics = limit.getDetailedMetrics();
        assertThat(metrics.getNumberOfWaitingThreads()).isEqualTo(0);
        assertThat(metrics.getAvailablePermits()).isEqualTo(2);
    }

    @Test
    public void constructionWithNullName() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage(NAME_MUST_NOT_BE_NULL);
        new SemaphoreBasedRequestLimitImpl(null, config, null);
    }

    @Test
    public void constructionWithNullConfig() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage(CONFIG_MUST_NOT_BE_NULL);
        new SemaphoreBasedRequestLimitImpl("test", null, null);
    }

    private static ConditionFactory awaitImpatiently() {
        return await()
            .pollDelay(1, TimeUnit.MICROSECONDS)
            .pollInterval(2, TimeUnit.MILLISECONDS);
    }
}