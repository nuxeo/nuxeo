/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.multi.tenant.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author tiry
 */
public class SimpleCache<V> extends LinkedHashMap<String, V> {

    private static final long serialVersionUID = 1L;

    protected final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    private final int maxCachedItems;

    public SimpleCache(int maxCachedItems) {
        super(maxCachedItems, 1.0f, true);
        this.maxCachedItems = maxCachedItems;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<String, V> eldest) {
        return size() > maxCachedItems;
    }

    public V getIfPresent(String key) {
        return get(key);
    }

    @Override
    public V get(Object key) {
        try {
            cacheLock.readLock().lock();
            return super.get(key);
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    @Override
    public V put(String key, V arg1) {
        try {
            cacheLock.writeLock().lock();
            return super.put(key, arg1);
        } finally {
            cacheLock.writeLock().unlock();
        }

    }

    public void invalidate(String key) {
        try {
            cacheLock.writeLock().lock();
            super.remove(key);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

}
