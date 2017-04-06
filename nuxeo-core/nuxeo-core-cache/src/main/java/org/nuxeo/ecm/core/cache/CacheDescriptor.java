/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Maxime Hilaire
 *
 */
package org.nuxeo.ecm.core.cache;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Descriptor of cache contrib
 *
 * @since 6.0
 */
@XObject("cache")
public class CacheDescriptor {

    @XNode("@name")
    public String name;

    @XNode("@remove")
    public boolean remove = false;

    @XNode("@class")
    protected Class<? extends Cache> implClass = InMemoryCacheImpl.class;

    @XNode("ttl")
    public int ttl = 1;

    @XNodeMap(value = "option", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> options = new HashMap<String, String>();

    protected Cache cache;

    public CacheDescriptor() {
        super();
    }

    protected CacheDescriptor(String name, Class<? extends Cache> implClass, Integer ttl, Map<String, String> options) {
        this.name = name;
        this.implClass = implClass;
        this.ttl = ttl;
        this.options.putAll(options);
    }

    public CacheDescriptor(CacheDescriptor other) {
        name = other.name;
        implClass = other.implClass;
        ttl = other.ttl;
        options = new HashMap<String, String>(other.options);
    }

    @Override
    public CacheDescriptor clone() {
        return new CacheDescriptor(name, implClass, ttl, options);
    }

    public Class<?> getImplClass() {
        return implClass;
    }

    public void setImplClass(Class<Cache> implClass) {
        this.implClass = implClass;
    }

    @Override
    public String toString() {
        return name + ": " + implClass + ": " + ttl + ": " + options;
    }

    protected void invalidateAll() {
        cache.invalidateAll();
    }

    protected void start() {
        try {
            cache = implClass.getConstructor(CacheDescriptor.class).newInstance(this);
            cache = new CacheAttributesChecker(this, cache);
            cache = new CacheMetrics(cache);
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException("Failed to instantiate class " + implClass, e);
        }
    }

    protected void stop() {
        if (cache == null) {
            return;
        }
        try {
            ((CacheWrapper) cache).stop();
        } finally {
            cache = null;
        }
    }

}
