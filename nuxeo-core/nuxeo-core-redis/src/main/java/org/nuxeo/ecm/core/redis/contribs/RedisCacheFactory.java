package org.nuxeo.ecm.core.redis.contribs;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.nuxeo.common.xmap.XMap;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.cache.Cache;
import org.nuxeo.ecm.core.cache.CacheDescriptor;
import org.nuxeo.ecm.core.cache.CacheFactory;


public class RedisCacheFactory implements CacheFactory {

    final XMap xmap = configureXMap();

    XMap configureXMap() {
        XMap xmap = new XMap();
        xmap.register(RedisCacheConfig.class);
        return xmap;
    }

    @Override
    public XMap xmap(CacheDescriptor config) {
        return xmap;
    }

    @Override
    public Cache createCache(CacheDescriptor config) {
        return new RedisCache(config);
    }

    @Override
    public void destroyCache(Cache cache) {
        try {
            cache.invalidateAll();
        } catch (IOException cause) {
            throw new NuxeoException("Cannot empty cache " + cache.getName(), cause);
        }
    }

    @Override
    public boolean isOf(Class<? extends Cache> type) {
        return type.isAssignableFrom(RedisCache.class);
    }

    @Override
    public void merge(CacheDescriptor src, CacheDescriptor dst) {
        ((RedisCacheConfig)dst).ttlInSeconds = ((RedisCacheConfig)src).ttlInSeconds;
    }

    @Override
    public CacheDescriptor createConfig(String name) {
        RedisCacheConfig config = new RedisCacheConfig();
        config.name = name;
        config.ttl = 10;
        config.ttlInSeconds = Long.valueOf(TimeUnit.MINUTES.convert(config.ttl, TimeUnit.SECONDS)).intValue();
        return config;
    }

}
