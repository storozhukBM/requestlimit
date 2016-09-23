package cc.storozhuk.requestlimit;

import cc.storozhuk.requestlimit.internal.InMemoryRequestLimitRegistry;

import java.util.function.Supplier;

/**
 * @author bstorozhuk
 */
public interface RequestLimitRegistry {

    RequestLimit requestLimit(String name);

    RequestLimit requestLimit(String name, RequestLimitConfig requestLimitConfig);

    RequestLimit requestLimit(String name, Supplier<RequestLimitConfig> requestLimitConfig);

    static RequestLimitRegistry of(RequestLimitConfig defaultRequestLimitConfig) {
        return new InMemoryRequestLimitRegistry(defaultRequestLimitConfig);
    }
}
