/*
 * (C) Copyright 2025-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.runtime.ts;

import static org.apache.commons.lang3.ObjectUtils.getIfNull;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * @since 2025.8
 */
@XObject("dataStore")
public class TransientDataStoreDescriptor implements Descriptor {

    @XNode("@name")
    protected String name;

    @XNode("ttl")
    protected Duration ttl;

    @XNode("ttl@policy")
    protected TTLPolicy ttlPolicy;

    @XNode("provider")
    protected Provider provider;

    @Override
    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public Duration getTtl() {
        return getIfNull(ttl, () -> Duration.ofDays(1));
    }

    public TTLPolicy getTtlPolicy() {
        return getIfNull(ttlPolicy, TTLPolicy.CREATED);
    }

    public Optional<String> getProviderProperty(String key) {
        return Optional.ofNullable(provider.properties.get(key)).filter(StringUtils::isNotBlank);
    }

    public Map<String, String> getProviderProperties() {
        return Collections.unmodifiableMap(provider.properties);
    }

    @Override
    public TransientDataStoreDescriptor merge(Descriptor o) {
        TransientDataStoreDescriptor other = (TransientDataStoreDescriptor) o;
        TransientDataStoreDescriptor merged = new TransientDataStoreDescriptor();
        merged.name = getIfNull(other.name, name);
        merged.ttl = getIfNull(other.ttl, ttl);
        merged.ttlPolicy = getIfNull(other.ttlPolicy, ttlPolicy);
        merged.provider = getIfNull(other.provider, provider); // no merge on provider
        return merged;
    }

    public enum TTLPolicy {

        /**
         * Expires entries based on when they were last accessed.
         */
        ACCESSED,

        /**
         * Expires entries based on when they were created, default.
         */
        CREATED,
    }

    @XObject("provider")
    protected static class Provider {

        @XNode("@class")
        protected Class<? extends TransientDataStoreProvider> klass;

        @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
        protected Map<String, String> properties;
    }
}
