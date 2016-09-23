package cc.storozhuk.requestlimit.internal;

import static java.util.Objects.requireNonNull;

import cc.storozhuk.requestlimit.RequestLimit;
import cc.storozhuk.requestlimit.RequestLimitConfig;
import cc.storozhuk.requestlimit.RequestLimitRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * @author bstorozhuk
 */
public class InMemoryRequestLimitRegistry implements RequestLimitRegistry {

    private static final String NAME_MUST_NOT_BE_NULL = "Name must not be null";
    private static final String CONFIG_MUST_NOT_BE_NULL = "Config must not be null";
    private static final String SUPPLIER_MUST_NOT_BE_NULL = "Supplier must not be null";

    private final RequestLimitConfig defaultRequestLimitConfig;
    private final Map<String, RequestLimit> requestLimits;

    public InMemoryRequestLimitRegistry(final RequestLimitConfig requestLimitConfig) {
        defaultRequestLimitConfig = requireNonNull(requestLimitConfig, CONFIG_MUST_NOT_BE_NULL);
        requestLimits = new ConcurrentHashMap<>();
    }

    @Override
    public RequestLimit requestLimit(final String name) {
        return requestLimit(name, defaultRequestLimitConfig);
    }

    @Override
    public RequestLimit requestLimit(final String name, final RequestLimitConfig requestLimitConfig) {
        requireNonNull(name, NAME_MUST_NOT_BE_NULL);
        requireNonNull(requestLimitConfig, CONFIG_MUST_NOT_BE_NULL);
        return requestLimits.computeIfAbsent(
            name,
            limitName -> new SemaphoreBasedRequestLimitImpl(name, requestLimitConfig)
        );
    }

    @Override
    public RequestLimit requestLimit(final String name, final Supplier<RequestLimitConfig> requestLimitConfigSupplier) {
        requireNonNull(name, NAME_MUST_NOT_BE_NULL);
        requireNonNull(requestLimitConfigSupplier, SUPPLIER_MUST_NOT_BE_NULL);
        return requestLimits.computeIfAbsent(
            name,
            limitName -> {
                RequestLimitConfig requestLimitConfig = requestLimitConfigSupplier.get();
                requireNonNull(requestLimitConfig, CONFIG_MUST_NOT_BE_NULL);
                return new SemaphoreBasedRequestLimitImpl(limitName, requestLimitConfig);
            }
        );
    }
}
