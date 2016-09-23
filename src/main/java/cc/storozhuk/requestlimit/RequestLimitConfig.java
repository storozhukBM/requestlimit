package cc.storozhuk.requestlimit;

import static java.util.Objects.requireNonNull;

import java.time.Duration;

/**
 * @author bstorozhuk
 */
public class RequestLimitConfig {
    private static final String TIMEOUT_DURATION_MUST_NOT_BE_NULL = "TimeoutDuration must not be null";
    private static final String LIMIT_REFRESH_PERIOD_MUST_NOT_BE_NULL = "LimitRefreshPeriod must not be null";

    private static final Duration ACCEPTABLE_REFRESH_PERIOD = Duration.ofNanos(500L); // TODO: use jmh to find real one

    private final Duration timeoutDuration;
    private final Duration limitRefreshPeriod;
    private final int limitForPeriod;

    private RequestLimitConfig(final Duration timeoutDuration, final Duration limitRefreshPeriod, final int limitForPeriod) {
        this.timeoutDuration = checkTimeoutDuration(timeoutDuration);
        this.limitRefreshPeriod = checkLimitRefreshPeriod(limitRefreshPeriod);
        this.limitForPeriod = checkLimitForPeriod(limitForPeriod);
    }

    public Duration getTimeoutDuration() {
        return timeoutDuration;
    }

    public Duration getLimitRefreshPeriod() {
        return limitRefreshPeriod;
    }

    public int getLimitForPeriod() {
        return limitForPeriod;
    }

    public static Builder builder() {
        return new Builder();
    }

    private static Duration checkTimeoutDuration(final Duration timeoutDuration) {
        return requireNonNull(timeoutDuration, TIMEOUT_DURATION_MUST_NOT_BE_NULL);
    }

    private static Duration checkLimitRefreshPeriod(Duration limitRefreshPeriod) {
        requireNonNull(limitRefreshPeriod, LIMIT_REFRESH_PERIOD_MUST_NOT_BE_NULL);
        boolean refreshPeriodIsTooShort = limitRefreshPeriod.compareTo(ACCEPTABLE_REFRESH_PERIOD) < 0;
        if (refreshPeriodIsTooShort) {
            throw new IllegalArgumentException("LimitRefreshPeriod is too short");
        }
        return limitRefreshPeriod;
    }

    private static int checkLimitForPeriod(final int limitForPeriod) {
        if (limitForPeriod < 1) {
            throw new IllegalArgumentException("LimitForPeriod should be greater than 0");
        }
        return limitForPeriod;
    }


    public static class Builder {
        private Duration timeoutDuration;
        private Duration limitRefreshReriod;
        private int limitForPeriod;

        public RequestLimitConfig build() {
            return new RequestLimitConfig(
                timeoutDuration,
                limitRefreshReriod,
                limitForPeriod
            );
        }

        public Builder timeoutDuration(final Duration timeoutDuration) {
            this.timeoutDuration = checkTimeoutDuration(timeoutDuration);
            return this;
        }

        public Builder limitRefreshPeriod(final Duration limitRefreshPeriod) {
            this.limitRefreshReriod = checkLimitRefreshPeriod(limitRefreshPeriod);
            return this;
        }

        public Builder limitForPeriod(final int limitForPeriod) {
            this.limitForPeriod = checkLimitForPeriod(limitForPeriod);
            return this;
        }
    }
}
