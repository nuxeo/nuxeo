/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.cache;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

public class ThreadSafeCacheHolder<T extends Serializable> implements Serializable {

    public static final int DEFAULT_SIZE = 20;

    private static final long serialVersionUID = 1L;

    protected final Map<String, T> cacheMap;

    protected final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    public ThreadSafeCacheHolder() {
        this(DEFAULT_SIZE);
    }

    public ThreadSafeCacheHolder(int maxSize) {
        cacheMap = new LRUCachingMap<>(maxSize);
    }

    protected String getKey(DocumentRef docRef, String key) {
        if (docRef == null) {
            if (key == null) {
                return "default";
            } else {
                return key;
            }
        } else {
            if (key == null) {
                return docRef.toString();
            } else {
                return docRef.toString() + "-" + key;
            }
        }
    }

    protected String getKey(DocumentModel doc, String key) {
        DocumentRef docRef = doc.getRef();
        Calendar modified = (Calendar) doc.getProperty("dublincore", "modified");
        if (key == null) {
            if (modified != null) {
                key = modified.toString();
            }
        } else {
            key = key + "-" + key;
        }

        return getKey(docRef, key);
    }

    // Adders
    protected void doAdd(String key, T value) {
        try {
            cacheLock.writeLock().lock();
            cacheMap.put(key, value);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    public void addToCache(String key, T value) {
        doAdd(key, value);
    }

    public void addToCache(DocumentRef docRef, String key, T value) {
        doAdd(getKey(docRef, key), value);
    }

    public void addToCache(DocumentModel doc, String key, T value) {
        doAdd(getKey(doc, key), value);
    }

    // Getters
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

    // Removers
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
