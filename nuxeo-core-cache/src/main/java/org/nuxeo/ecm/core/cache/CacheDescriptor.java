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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor of cache contrib
 * 
 * @since 5.9.6
 */
@XObject("cache")
public class CacheDescriptor {

    @XNode("@name")
    public String name;

    @XNode("@remove")
    public boolean remove;

    @XNode("@class")
    protected Class<?> implClass;

    @XNode("ttl")
    protected Integer ttl;

    @XNode("maxSize")
    protected Integer maxSize;

    @XNode("concurrency-level")
    protected Integer concurrencyLevel;

    public CacheDescriptor()
    {
        
    }
    
    public CacheDescriptor(String name, Class<?> implClass, Integer ttl,
            Integer concurrencyLevel, Integer maxSize) {
        this.name = name;
        this.implClass = implClass;
        this.ttl = ttl;
        this.concurrencyLevel = concurrencyLevel;
        this.maxSize = maxSize;
    }

    public CacheDescriptor clone() {
        return new CacheDescriptor(name, implClass, ttl,
                concurrencyLevel, maxSize);
    }

    public Class<?> getImplClass() {
        return implClass;
    }

    public void setImplClass(Class<Cache> implClass) {
        this.implClass = implClass;
    }

    @Override
    public String toString() {
        return name + ": " + implClass + ": " + ttl + ": " + concurrencyLevel;
    }

    public Integer getTtl() {
        return ttl;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }

    public Integer getConcurrencyLevel() {
        return concurrencyLevel;
    }

    public void setConcurrencyLevel(Integer concurrencyLevel) {
        this.concurrencyLevel = concurrencyLevel;
    }

    public Integer getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(Integer maxSize) {
        this.maxSize = maxSize;
    }

}
