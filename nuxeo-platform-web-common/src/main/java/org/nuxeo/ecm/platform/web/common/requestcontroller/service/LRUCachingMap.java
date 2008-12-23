package org.nuxeo.ecm.platform.web.common.requestcontroller.service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @param <K>
 * @param <V>
 *
 * @author tiry
 */
public class LRUCachingMap<K,V> extends LinkedHashMap<K,V> {

    private static final long serialVersionUID = 1L;

    private final int maxCachedItems;

    public LRUCachingMap(int maxCachedItems) {
        super(maxCachedItems, 1.0f, true);
        this.maxCachedItems = maxCachedItems;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        return size() > maxCachedItems;
    }

}
