package org.nuxeo.ecm.core.redis.contribs;

import java.util.concurrent.TimeUnit;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.cache.CacheDescriptor;

@XObject("cache")
public class RedisCacheConfig extends CacheDescriptor {

    int ttlInSeconds;

    @XNode(value="ttl", context="nuxeo.cache.ttl")
    public void setTTL(int ttl) {
        ttlInSeconds = Long.valueOf(TimeUnit.MINUTES.convert(ttl, TimeUnit.SECONDS)).intValue();
    }
}
