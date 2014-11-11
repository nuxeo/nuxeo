package org.nuxeo.ecm.platform.ui.web.cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Inner class to manage LRU cache clean up
 *
 * @author tiry
 *
 * @param <K>
 * @param <V>
 */
public class LRUCachingMap<K, V> extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = 1L;

    private final int maxCachedItems;

    public LRUCachingMap(int maxCachedItems) {
        super(maxCachedItems, 1.0f, true);
        this.maxCachedItems = maxCachedItems;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxCachedItems;
    }

}