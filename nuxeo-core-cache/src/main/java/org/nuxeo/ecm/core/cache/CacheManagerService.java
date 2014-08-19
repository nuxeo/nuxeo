package org.nuxeo.ecm.core.cache;

public interface CacheManagerService {
    public static final String INVALIDATE_ALL = "invalidateAll";
    
    public CacheManager<?,?> getCacheManager(String name);
}
