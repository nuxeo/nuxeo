package org.nuxeo.ecm.core.cache;

import org.nuxeo.runtime.services.event.EventListener;

public abstract class AbstractCacheManager<K,V> implements CacheManager<K,V>,
        EventListener {

    protected String name = null;

    protected Integer maxSize = 0;

    protected Integer ttl = 0;

    protected Integer concurrencyLevel = 0;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(Integer maxSize) {
        this.maxSize = maxSize;
    }

    public Integer getTtl() {
        return ttl;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }

    public Integer getConcurrencyLevel() {
        return concurrencyLevel;
    }

    public void setConcurrencyLevel(Integer concurrencyLevel) {
        this.concurrencyLevel = concurrencyLevel;
    }

}
