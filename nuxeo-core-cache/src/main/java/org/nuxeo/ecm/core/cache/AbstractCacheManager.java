package org.nuxeo.ecm.core.cache;

import org.nuxeo.runtime.services.event.EventListener;

public abstract class AbstractCacheManager implements CacheManager,
        EventListener {

    protected String name = null;

    protected long maxSize = 0L;

    protected long ttl = 0L;

    protected long concurrencyLevel = 0L;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public long getConcurrencyLevel() {
        return concurrencyLevel;
    }

    public void setConcurrencyLevel(long concurrencyLevel) {
        this.concurrencyLevel = concurrencyLevel;
    }

}
