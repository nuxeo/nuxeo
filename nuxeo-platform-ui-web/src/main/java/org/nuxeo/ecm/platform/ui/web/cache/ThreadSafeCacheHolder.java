package org.nuxeo.ecm.platform.ui.web.cache;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

public class ThreadSafeCacheHolder<T> implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;


    protected Map<String, T> cacheMap = null;

    public final static int DEFAULT_SIZE = 20;

    protected ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    public ThreadSafeCacheHolder() {
        this(DEFAULT_SIZE);
    }

    public ThreadSafeCacheHolder(int maxSize) {
        cacheMap = new LRUCachingMap<String, T>(maxSize);
    }

    protected String getKey(DocumentRef docRef, String key) {
        if (docRef == null) {
            if (key == null)
                return "default";
            else
                return key;
        } else {
            if (key == null)
                return docRef.toString();
            else
                return docRef.toString() + "-" + key;
        }
    }

    protected String getKey(DocumentModel doc, String key) {
        DocumentRef docRef = doc.getRef();
        Calendar modified = (Calendar) doc
                .getProperty("dublincore", "modified");
        if (key == null) {
            if (modified != null)
                key = modified.toString();
        }

        else
            key = key + "-" + key;

        return getKey(docRef, key);
    }

    // Add management
    protected void doAdd(String key, T value) {
        try {
            cacheLock.writeLock().lock();
            cacheMap.put(key, value);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    public void addTocache(String key, T value) {
        doAdd(key, value);
    }

    public void addToCache(DocumentRef docRef, String key, T value) {
        doAdd(getKey(docRef, key), value);
    }

    public void addToCache(DocumentModel doc, String key, T value) {
        doAdd(getKey(doc, key), value);
    }

    // get management
    protected T doGet(String key) {
        try {
            cacheLock.readLock().lock();
            return cacheMap.get(key);
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    public T getFromCache(String key) {
        return doGet(key);
    }

    public T getFromCache(DocumentRef docRef, String key) {
        return doGet(getKey(docRef, key));
    }

    public T getFromCache(DocumentModel doc, String key) {
        return doGet(getKey(doc, key));
    }

    // remove management
    protected void doRemove(String key) {
        try {
            cacheLock.writeLock().lock();
            cacheMap.remove(key);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    public void removeFromCache(DocumentRef docRef, String key) {
        doRemove(getKey(docRef, key));
    }

    public void removeFromCache(DocumentModel doc, String key) {
        doRemove(getKey(doc, key));
    }

    public void removeFromCache(String key) {
        doRemove(key);
    }

}
