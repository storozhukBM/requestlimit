package cc.storozhuk.requestlimit;

import java.time.Duration;

/**
 * @author bstorozhuk
 */
public class RequestLimitConfig {
    private final Duration timeoutDuration;

    public RequestLimitConfig(final Duration timeoutDuration) {
        this.timeoutDuration = timeoutDuration;
    }

    public Duration getTimeoutDuration() {
        return timeoutDuration;
    }
}
