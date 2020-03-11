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
import org.nuxeo.runtime.model.Descriptor;

/**
 * Descriptor of cache contrib
 *
 * @since 6.0
 */
@XObject("cache")
public class CacheDescriptor implements Descriptor {

    /** Default TTL in minutes. */
    public static final long DEFAULT_TTL = 1;

    /**
     * Default max size
     *
     * @since 9.3
     */
    public static final long DEFAULT_MAX_SIZE = 100;

    /**
     * Maximum number of entries the cache may contain.
     *
     * @since 9.3
     */
    public static final String OPTION_MAX_SIZE = "maxSize";

    /** @since 9.3 */
    public static final String OPTION_CONCURRENCY_LEVEL = "concurrencyLevel";

    @XNode("@name")
    public String name;

    @XNode("@remove")
    public boolean remove;

    @XNode("@class")
    protected Class<? extends CacheManagement> klass;

    @XNode("ttl")
    private Long ttl;

    @XNodeMap(value = "option", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> options = new HashMap<>();

    @Override
    public String getId() {
        return name;
    }

    public long getTTL() {
        return ttl == null ? DEFAULT_TTL : ttl.longValue();
    }

    public void setTTL(Long value) {
        ttl = value;
    }

    @Override
    public Descriptor merge(Descriptor o) {
        CacheDescriptor other = (CacheDescriptor) o;
        CacheDescriptor merged = new CacheDescriptor();
        merged.name = name;
        merged.remove = other.remove;
        merged.ttl = other.ttl != null ? other.ttl : ttl;
        merged.klass = other.klass != null ? other.klass : klass;
        merged.options.putAll(options);
        merged.options.putAll(other.options);
        return merged;
    }

    @Override
    public boolean doesRemove() {
        return remove;
    }

}
