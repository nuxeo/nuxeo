/*
 * (C) Copyright 2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Maxime Hilaire
 *
 */
package org.nuxeo.ecm.core.cache;

import java.io.IOException;
import java.io.Serializable;

/**
 * Class to implement mandatory check attributes before calling implementation
 * of cache This enable to have the same behavior for any use of cache for all
 * implementation of cache
 *
 * @since 6.0
 */
public class CacheAttributesChecker extends AbstractCache {

    protected Cache cache;

    protected CacheAttributesChecker(CacheDescriptor desc) {
        super(desc);
    }

    void setCache(Cache cache) {
        this.cache = cache;
    }

    public Cache getCache() {
        return cache;
    }

    @Override
    public Serializable get(String key) throws IOException {
        if(key == null)
        {
            return null;
        }
        return cache.get(key);
    }

    @Override
    public void invalidate(String key) throws IOException {
        if (key == null) {
            throw new IllegalArgumentException(String.format(
                    "Can't invalidate a null key for the cache '%s'!", name));
        }
        cache.invalidate(key);
    }

    @Override
    public void invalidateAll() throws IOException {
        cache.invalidateAll();
    }

    @Override
    public void put(String key, Serializable value) throws IOException {
        if (key == null) {
            throw new IllegalArgumentException(String.format(
                    "Can't put a null key for the cache '%s'!", name));
        }
        if (value == null) {
            throw new IllegalArgumentException(String.format(
                    "Can't put a null value for the cache '%s'!", name));
        }
        cache.put(key, value);
    }

}
