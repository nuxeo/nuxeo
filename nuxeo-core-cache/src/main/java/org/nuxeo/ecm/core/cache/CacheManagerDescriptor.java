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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author Maxime Hilaire
 * 
 * @since 5.9.6
 */
@XObject("cacheManager")
public class CacheManagerDescriptor {

    @XNode("@name")
    public String name;

    @XNode("@remove")
    public boolean remove;

    @XNode("@class")
    protected Class<?> implClass;

    @XNode("ttl")
    protected long ttl;

    @XNode("maxSize")
    protected long maxSize;

    @XNode("concurrency-level")
    protected long concurrencyLevel;

    public CacheManagerDescriptor()
    {
        
    }
    
    public CacheManagerDescriptor(String name, Class<?> implClass, long ttl,
            long concurrencyLevel, long maxSize) {
        this.name = name;
        this.implClass = implClass;
        this.ttl = ttl;
        this.concurrencyLevel = concurrencyLevel;
        this.maxSize = maxSize;
    }

    public CacheManagerDescriptor clone() {
        return new CacheManagerDescriptor(name, implClass, ttl,
                concurrencyLevel, maxSize);
    }

    public Class<?> getImplClass() {
        return implClass;
    }

    public void setImplClass(Class<CacheManager> implClass) {
        this.implClass = implClass;
    }

    @Override
    public String toString() {
        return name + ": " + implClass + ": " + ttl + ": " + concurrencyLevel;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public long getConcurrencyLevel() {
        return concurrencyLevel;
    }

    public void setConcurrencyLevel(long concurrencyLevel) {
        this.concurrencyLevel = concurrencyLevel;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }

}
