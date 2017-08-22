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

    /** Default TTL in minutes. */
    public static final long DEFAULT_TTL = 1;

    @XNode("@name")
    public String name;

    @XNode("@remove")
    public boolean remove;

    @XNode("@class")
    protected Class<? extends CacheManagement> klass;

    @XNode("ttl")
    public Long ttl;

    /** @since 9.3 */
    public static final String OPTION_CONCURRENCY_LEVEL = "concurrencyLevel";

    /**
     * Maximum number of entries the cache may contain.
     *
     * @since 9.3
     */
    public static final String OPTION_MAX_SIZE = "maxSize";

    @XNodeMap(value = "option", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> options = new HashMap<String, String>();

    public CacheDescriptor() {
    }

    /**
     * Copy constructor.
     */
    public CacheDescriptor(CacheDescriptor other) {
        name = other.name;
        ttl = other.ttl;
        klass = other.klass;
        options = new HashMap<String, String>(other.options);
    }

    @Override
    public CacheDescriptor clone() {
        return new CacheDescriptor(this);
    }

    public void merge(CacheDescriptor other) {
        remove = other.remove;
        if (other.ttl != null) {
            ttl = other.ttl;
        }
        if (other.klass != null) {
            klass = other.klass;
        }
        if (other.options != null) {
            if (options == null) {
                options = new HashMap<>(other.options);
            } else {
                options.putAll(other.options);
            }
        }
    }

    public long getTTL() {
        return ttl == null ? DEFAULT_TTL : ttl.longValue();
    }

    @Override
    public String toString() {
        return name + ": " + klass + ": " + ttl + ": " + options;
    }

    protected CacheManagement newInstance() {
        CacheManagement cache;
        if (klass == null) {
            cache = new InMemoryCacheImpl(this); // default cache implementation
        } else {
            try {
                cache = klass.getConstructor(CacheDescriptor.class).newInstance(this);
            } catch (ReflectiveOperationException e) {
                throw new NuxeoException("Failed to instantiate class: " + klass + " for cache: " + name, e);
            }
        }
        // wrap with checker and metrics
        cache = new CacheAttributesChecker(cache);
        cache = new CacheMetrics(cache);
        return cache;
    }

}
