/*
 * (C) Copyright 2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Maxime Hilaire
 *
 */
package org.nuxeo.ecm.core.cache;

import java.util.concurrent.TimeUnit;

import org.nuxeo.runtime.services.event.Event;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Default in memory implementation for cache management based on guava
 * 
 * @author Maxime Hilaire
 * @param <V>
 * @since 5.9.6
 */
public class CacheManagerImpl<T> extends AbstractCacheManager<T> {

    protected Cache<String, T> cache = null;
    
    @Override
    public boolean aboutToHandleEvent(Event arg0) {
        // TODO Auto-generated method stub
        // return false;
        throw new UnsupportedOperationException();
    }

    @Override
    public void handleEvent(Event arg0) {
        // TODO Auto-generated method stub
        // 
        throw new UnsupportedOperationException();
    }
    
    private void createCache()
    {
        if(this.cache == null)
        {
            cache = CacheBuilder.newBuilder().concurrencyLevel(
                concurrencyLevel).maximumSize(
                maxSize).expireAfterWrite(
                ttl, TimeUnit.MINUTES).build();
        }
        
    }
    
    @Override
    public Cache<String, T> getCache()
    {
        createCache();
        return cache;
    }

    @Override
    public T get(String key) {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }


    @Override
    public void invalidate(String key) {
        // TODO Auto-generated method stub
        // 
        throw new UnsupportedOperationException();
    }

    @Override
    public void invalidateAll() {
        // TODO Auto-generated method stub
        // 
        throw new UnsupportedOperationException();
    }

    @Override
    public void put(String key, T value) {
        createCache();
        cache.put(key, value);
    }
    


}
