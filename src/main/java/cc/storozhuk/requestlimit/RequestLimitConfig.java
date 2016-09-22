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

    public RequestLimitConfig(final Duration timeoutDuration, final Duration limitRefreshPeriod, final int limitForPeriod) {
        this.timeoutDuration = requireNonNull(timeoutDuration, TIMEOUT_DURATION_MUST_NOT_BE_NULL);
        this.limitRefreshPeriod = requireNonNull(limitRefreshPeriod, LIMIT_REFRESH_PERIOD_MUST_NOT_BE_NULL);
        this.limitForPeriod = limitForPeriod;

        if (limitForPeriod < 1) {
            throw new IllegalArgumentException("LimitForPeriod should be greater than 0");
        }

        boolean refreshPeriodIsTooShort = this.limitRefreshPeriod.compareTo(ACCEPTABLE_REFRESH_PERIOD) < 0;
        if (refreshPeriodIsTooShort) {
            throw new IllegalArgumentException("LimitRefreshPeriod is too short");
        }
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
}
