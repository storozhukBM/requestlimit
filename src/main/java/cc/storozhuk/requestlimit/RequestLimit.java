package cc.storozhuk.requestlimit;

import javaslang.control.Try;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface RequestLimit {

    boolean getPermission(Duration timeoutDuration);

    String getName();

    Metrics getMetrics();

    RequestLimitConfig getRequestLimitConfig();

    interface Metrics {
        /**
         * Returns an estimate of the number of threads waiting for permission
         * in this JVM process.
         *
         * @return estimate of the number of threads waiting for permission.
         */
        int getNumberOfWaitingThreads();
    }

    static <T> Try.CheckedSupplier<T> decorateCheckedSupplier(Try.CheckedSupplier<T> supplier, RequestLimit requestLimit) {
        RequestLimitConfig requestLimitConfig = requestLimit.getRequestLimitConfig();
        Duration timeoutDuration = requestLimitConfig.getTimeoutDuration();

        Try.CheckedSupplier<T> decoratedSupplier = () -> {
            requestLimit.getPermission(timeoutDuration);
            T result = supplier.get();
            return result;
        };
        return decoratedSupplier;
    }

    static Try.CheckedRunnable decorateCheckedRunnable(Try.CheckedRunnable runnable, RequestLimit requestLimit) {
        RequestLimitConfig requestLimitConfig = requestLimit.getRequestLimitConfig();
        Duration timeoutDuration = requestLimitConfig.getTimeoutDuration();

        Try.CheckedRunnable decoratedRunnable = () -> {
            requestLimit.getPermission(timeoutDuration);
            runnable.run();
        };
        return decoratedRunnable;
    }

    static <T, R> Try.CheckedFunction<T, R> decorateCheckedFunction(Try.CheckedFunction<T, R> function, RequestLimit requestLimit) {
        RequestLimitConfig requestLimitConfig = requestLimit.getRequestLimitConfig();
        Duration timeoutDuration = requestLimitConfig.getTimeoutDuration();

        Try.CheckedFunction<T, R> decoratedFunction = (T t) -> {
            requestLimit.getPermission(timeoutDuration);
            R result = function.apply(t);
            return result;
        };
        return decoratedFunction;
    }

    static <T> Supplier<T> decorateSupplier(Supplier<T> supplier, RequestLimit requestLimit) {
        RequestLimitConfig requestLimitConfig = requestLimit.getRequestLimitConfig();
        Duration timeoutDuration = requestLimitConfig.getTimeoutDuration();

        Supplier<T> decoratedSupplier = () -> {
            requestLimit.getPermission(timeoutDuration);
            T result = supplier.get();
            return result;
        };
        return decoratedSupplier;
    }

    static <T> Consumer<T> decorateConsumer(Consumer<T> consumer, RequestLimit requestLimit) {
        RequestLimitConfig requestLimitConfig = requestLimit.getRequestLimitConfig();
        Duration timeoutDuration = requestLimitConfig.getTimeoutDuration();

        Consumer<T> decoratedConsumer = (T t) -> {
            requestLimit.getPermission(timeoutDuration);
            consumer.accept(t);
        };
        return decoratedConsumer;
    }

    static Runnable decorateRunnable(Runnable runnable, RequestLimit requestLimit) {
        RequestLimitConfig requestLimitConfig = requestLimit.getRequestLimitConfig();
        Duration timeoutDuration = requestLimitConfig.getTimeoutDuration();

        Runnable decoratedRunnable = () -> {
            requestLimit.getPermission(timeoutDuration);
            runnable.run();
        };
        return decoratedRunnable;
    }

    static <T, R> Function<T, R> decorateFunction(Function<T, R> function, RequestLimit requestLimit) {
        RequestLimitConfig requestLimitConfig = requestLimit.getRequestLimitConfig();
        Duration timeoutDuration = requestLimitConfig.getTimeoutDuration();

        Function<T, R> decoratedFunction = (T t) -> {
            requestLimit.getPermission(timeoutDuration);
            R result = function.apply(t);
            return result;
        };
        return decoratedFunction;
    }
}
