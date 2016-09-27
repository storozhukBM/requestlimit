package cc.storozhuk.requestlimit.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cc.storozhuk.requestlimit.RequestLimit;
import cc.storozhuk.requestlimit.RequestLimitConfig;
import cc.storozhuk.requestlimit.RequestLimitRegistry;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * @author bstorozhuk
 */
public class InMemoryRequestLimitRegistryTest {

    private static final int LIMIT = 50;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REFRESH_PERIOD = Duration.ofNanos(500);
    private static final String CONFIG_MUST_NOT_BE_NULL = "Config must not be null";
    private static final String NAME_MUST_NOT_BE_NULL = "Name must not be null";
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
    public void requestLimitPositive() throws Exception {
        RequestLimitRegistry registry = RequestLimitRegistry.of(config);
        RequestLimit firstRequestLimit = registry.requestLimit("test");
        RequestLimit anotherLimit = registry.requestLimit("test1");
        RequestLimit sameAsFirst = registry.requestLimit("test");

        assertThat(firstRequestLimit).isEqualTo(sameAsFirst);
        assertThat(firstRequestLimit).isNotEqualTo(anotherLimit);
    }

    @Test
    public void requestLimitPositiveWithSupplier() throws Exception {
        RequestLimitRegistry registry = new InMemoryRequestLimitRegistry(config);
        Supplier<RequestLimitConfig> requestLimitConfigSupplier = mock(Supplier.class);
        when(requestLimitConfigSupplier.get())
            .thenReturn(config);

        RequestLimit firstRequestLimit = registry.requestLimit("test", requestLimitConfigSupplier);
        verify(requestLimitConfigSupplier, times(1)).get();
        RequestLimit sameAsFirst = registry.requestLimit("test", requestLimitConfigSupplier);
        verify(requestLimitConfigSupplier, times(1)).get();
        RequestLimit anotherLimit = registry.requestLimit("test1", requestLimitConfigSupplier);
        verify(requestLimitConfigSupplier, times(2)).get();

        assertThat(firstRequestLimit).isEqualTo(sameAsFirst);
        assertThat(firstRequestLimit).isNotEqualTo(anotherLimit);
    }

    @Test
    public void requestLimitConfigIsNull() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage(CONFIG_MUST_NOT_BE_NULL);
        new InMemoryRequestLimitRegistry(null);
    }

    @Test
    public void requestLimitNewWithNullName() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage(NAME_MUST_NOT_BE_NULL);
        RequestLimitRegistry registry = new InMemoryRequestLimitRegistry(config);
        registry.requestLimit(null);
    }

    @Test
    public void requestLimitNewWithNullNonDefaultConfig() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage(CONFIG_MUST_NOT_BE_NULL);
        RequestLimitRegistry registry = new InMemoryRequestLimitRegistry(config);
        RequestLimitConfig requestLimitConfig = null;
        registry.requestLimit("name", requestLimitConfig);
    }

    @Test
    public void requestLimitNewWithNullNameAndNonDefaultConfig() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage(NAME_MUST_NOT_BE_NULL);
        RequestLimitRegistry registry = new InMemoryRequestLimitRegistry(config);
        registry.requestLimit(null, config);
    }

    @Test
    public void requestLimitNewWithNullNameAndConfigSupplier() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage(NAME_MUST_NOT_BE_NULL);
        RequestLimitRegistry registry = new InMemoryRequestLimitRegistry(config);
        registry.requestLimit(null, () -> config);
    }

    @Test
    public void requestLimitNewWithNullConfigSupplier() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage("Supplier must not be null");
        RequestLimitRegistry registry = new InMemoryRequestLimitRegistry(config);
        Supplier<RequestLimitConfig> requestLimitConfigSupplier = null;
        registry.requestLimit("name", requestLimitConfigSupplier);
    }
}