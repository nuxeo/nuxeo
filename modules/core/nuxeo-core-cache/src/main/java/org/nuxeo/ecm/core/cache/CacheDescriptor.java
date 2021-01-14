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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * Descriptor of cache contrib
 *
 * @since 6.0
 */
@XObject("cache")
@XRegistry
public class CacheDescriptor {

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
    @XRegistryId
    protected String name;

    @XNode("@class")
    protected Class<? extends CacheManagement> klass;

    @XNode("ttl")
    protected Long ttl;

    @XNodeMap(value = "option", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> options = new HashMap<>();

    public CacheDescriptor() {
    }

    /** @since 11.5 */
    public CacheDescriptor(String name, Class<? extends CacheManagement> klass, Long ttl, Map<String, String> options) {
        this.name = name;
        this.klass = klass;
        this.ttl = ttl;
        this.options.putAll(options);
    }

    public String getName() {
        return name;
    }

    public long getTTL() {
        return ttl == null ? DEFAULT_TTL : ttl.longValue();
    }

    public Class<? extends CacheManagement> getKlass() {
        return klass;
    }

    public Map<String, String> getOptions() {
        return Collections.unmodifiableMap(options);
    }

}
