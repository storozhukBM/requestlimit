package cc.storozhuk.requestlimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Duration;

/**
 * @author bstorozhuk
 */
public class RequestLimitConfigTest {

    private static final int LIMIT = 50;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REFRESH_PERIOD = Duration.ofNanos(500);
    private static final String TIMEOUT_DURATION_MUST_NOT_BE_NULL = "TimeoutDuration must not be null";
    private static final String REFRESH_PERIOD_MUST_NOT_BE_NULL = "RefreshPeriod must not be null";

    @Rule
    public ExpectedException exception = ExpectedException.none();


    @Test
    public void builderPositive() throws Exception {
        RequestLimitConfig config = RequestLimitConfig.builder()
            .timeoutDuration(TIMEOUT)
            .limitRefreshPeriod(REFRESH_PERIOD)
            .limitForPeriod(LIMIT)
            .build();

        assertThat(config.getLimitForPeriod()).isEqualTo(LIMIT);
        assertThat(config.getLimitRefreshPeriod()).isEqualTo(REFRESH_PERIOD);
        assertThat(config.getTimeoutDuration()).isEqualTo(TIMEOUT);
    }

    @Test
    public void builderTimeoutIsNull() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage(TIMEOUT_DURATION_MUST_NOT_BE_NULL);
        RequestLimitConfig.builder()
            .timeoutDuration(null);
    }

    @Test
    public void builderTimeoutEmpty() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage(TIMEOUT_DURATION_MUST_NOT_BE_NULL);
        RequestLimitConfig.builder()
            .limitRefreshPeriod(REFRESH_PERIOD)
            .limitForPeriod(LIMIT)
            .build();
    }

    @Test
    public void builderRefreshPeriodIsNull() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage(REFRESH_PERIOD_MUST_NOT_BE_NULL);
        RequestLimitConfig.builder()
            .limitRefreshPeriod(null);
    }

    @Test
    public void builderRefreshPeriodEmpty() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage(REFRESH_PERIOD_MUST_NOT_BE_NULL);
        RequestLimitConfig.builder()
            .timeoutDuration(TIMEOUT)
            .limitForPeriod(LIMIT)
            .build();
    }

    @Test
    public void builderRefreshPeriodTooShort() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("RefreshPeriod is too short");
        RequestLimitConfig.builder()
            .timeoutDuration(TIMEOUT)
            .limitRefreshPeriod(Duration.ofNanos(499L))
            .limitForPeriod(LIMIT)
            .build();
    }

    @Test
    public void builderLimitIsLessThanOne() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("LimitForPeriod should be greater than 0");
        RequestLimitConfig.builder()
            .limitForPeriod(0);
    }
}
