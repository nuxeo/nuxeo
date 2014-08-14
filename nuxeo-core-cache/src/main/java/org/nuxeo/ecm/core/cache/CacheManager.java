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

import com.google.common.cache.Cache;


/**
 * @author Maxime Hilaire
 *
 * @since 5.9.6
 */
public interface CacheManager<T> {

    public static final String CORECACHEMANAGER_TOPIC = "corecachemanager";

    public String getName();

    public void setName(String name);

    public Integer getMaxSize();

    public void setMaxSize(Integer maxSize);

    public Integer getTtl();

    public void setTtl(Integer ttl);

    public Integer getConcurrencyLevel();

    public void setConcurrencyLevel(Integer concurrencyLevel);

    /**
     * Get the cache of the cache manager. Instantiate a new one if null
     * 
     * @return A cache instance. Type depends of the chosen implementation
     *
     * @since 5.9.6
     */
    public Cache<String, T> getCache();

    
    public T get(String key);
    public void invalidate(String key);
    public void invalidateAll();

    public void put(String key, T value);


}
