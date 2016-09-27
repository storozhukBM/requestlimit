package cc.storozhuk.requestlimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javaslang.control.Try;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author bstorozhuk
 */
public class RequestLimitTest {

    private static final int LIMIT = 50;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REFRESH_PERIOD = Duration.ofNanos(500);

    private RequestLimitConfig config;
    private RequestLimit limit;

    @Before
    public void init() {
        config = RequestLimitConfig.builder()
            .timeoutDuration(TIMEOUT)
            .limitRefreshPeriod(REFRESH_PERIOD)
            .limitForPeriod(LIMIT)
            .build();
        limit = mock(RequestLimit.class);
        when(limit.getRequestLimitConfig())
            .thenReturn(config);
    }

    @Test
    public void decorateCheckedSupplier() throws Throwable {
        Try.CheckedSupplier supplier = mock(Try.CheckedSupplier.class);
        Try.CheckedSupplier decorated = RequestLimit.decorateCheckedSupplier(supplier, limit);

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(false);

        Try decoratedSupplierResult = Try.of(decorated);
        assertThat(decoratedSupplierResult.isFailure()).isTrue();
        assertThat(decoratedSupplierResult.getCause()).isInstanceOf(RequestNotPermitted.class);
        verify(supplier, never()).get();

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(true);
        Try secondSupplierResult = Try.of(decorated);
        assertThat(secondSupplierResult.isSuccess()).isTrue();
        verify(supplier, times(1)).get();
    }

    @Test
    public void decorateCheckedRunnable() throws Throwable {
        Try.CheckedRunnable runnable = mock(Try.CheckedRunnable.class);
        Try.CheckedRunnable decorated = RequestLimit.decorateCheckedRunnable(runnable, limit);

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(false);

        Try decoratedRunnableResult = Try.run(decorated);
        assertThat(decoratedRunnableResult.isFailure()).isTrue();
        assertThat(decoratedRunnableResult.getCause()).isInstanceOf(RequestNotPermitted.class);
        verify(runnable, never()).run();

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(true);
        Try secondRunnableResult = Try.run(decorated);
        assertThat(secondRunnableResult.isSuccess()).isTrue();
        verify(runnable, times(1)).run();
    }

    @Test
    public void decorateCheckedFunction() throws Throwable {
        Try.CheckedFunction<Integer, String> function = mock(Try.CheckedFunction.class);
        Try.CheckedFunction<Integer, String> decorated = RequestLimit.decorateCheckedFunction(function, limit);

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(false);

        Try<String> decoratedFunctionResult = Try.success(1).mapTry(decorated);
        assertThat(decoratedFunctionResult.isFailure()).isTrue();
        assertThat(decoratedFunctionResult.getCause()).isInstanceOf(RequestNotPermitted.class);
        verify(function, never()).apply(any());

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(true);
        Try secondFunctionResult = Try.success(1).mapTry(decorated);
        assertThat(secondFunctionResult.isSuccess()).isTrue();
        verify(function, times(1)).apply(1);
    }

    @Test
    public void decorateSupplier() throws Exception {
        Supplier supplier = mock(Supplier.class);
        Supplier decorated = RequestLimit.decorateSupplier(supplier, limit);

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(false);

        Try decoratedSupplierResult = Try.success(decorated).map(Supplier::get);
        assertThat(decoratedSupplierResult.isFailure()).isTrue();
        assertThat(decoratedSupplierResult.getCause()).isInstanceOf(RequestNotPermitted.class);
        verify(supplier, never()).get();

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(true);
        Try secondSupplierResult = Try.success(decorated).map(Supplier::get);
        assertThat(secondSupplierResult.isSuccess()).isTrue();
        verify(supplier, times(1)).get();
    }

    @Test
    public void decorateConsumer() throws Exception {
        Consumer<Integer> consumer = mock(Consumer.class);
        Consumer<Integer> decorated = RequestLimit.decorateConsumer(consumer, limit);

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(false);

        Try<Integer> decoratedConsumerResult = Try.success(1).andThen(decorated);
        assertThat(decoratedConsumerResult.isFailure()).isTrue();
        assertThat(decoratedConsumerResult.getCause()).isInstanceOf(RequestNotPermitted.class);
        verify(consumer, never()).accept(any());

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(true);
        Try secondConsumerResult = Try.success(1).andThen(decorated);
        assertThat(secondConsumerResult.isSuccess()).isTrue();
        verify(consumer, times(1)).accept(1);
    }

    @Test
    public void decorateRunnable() throws Exception {
        Runnable runnable = mock(Runnable.class);
        Runnable decorated = RequestLimit.decorateRunnable(runnable, limit);

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(false);

        Try decoratedRunnableResult = Try.success(decorated).andThen(Runnable::run);
        assertThat(decoratedRunnableResult.isFailure()).isTrue();
        assertThat(decoratedRunnableResult.getCause()).isInstanceOf(RequestNotPermitted.class);
        verify(runnable, never()).run();

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(true);
        Try secondRunnableResult = Try.success(decorated).andThen(Runnable::run);
        assertThat(secondRunnableResult.isSuccess()).isTrue();
        verify(runnable, times(1)).run();
    }

    @Test
    public void decorateFunction() throws Exception {
        Function<Integer, String> function = mock(Function.class);
        Function<Integer, String> decorated = RequestLimit.decorateFunction(function, limit);

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(false);

        Try<String> decoratedFunctionResult = Try.success(1).map(decorated);
        assertThat(decoratedFunctionResult.isFailure()).isTrue();
        assertThat(decoratedFunctionResult.getCause()).isInstanceOf(RequestNotPermitted.class);
        verify(function, never()).apply(any());

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(true);
        Try secondFunctionResult = Try.success(1).map(decorated);
        assertThat(secondFunctionResult.isSuccess()).isTrue();
        verify(function, times(1)).apply(1);
    }

    @Test
    public void waitForPermissionWithOne() throws Exception {
        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(true);
        RequestLimit.waitForPermission(limit);
        verify(limit, times(1))
            .getPermission(config.getTimeoutDuration());
    }

    @Test(expected = RequestNotPermitted.class)
    public void waitForPermissionWithoutOne() throws Exception {
        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(false);
        RequestLimit.waitForPermission(limit);
        verify(limit, times(1))
            .getPermission(config.getTimeoutDuration());
    }
}