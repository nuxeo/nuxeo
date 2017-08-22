/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.cache;

import java.io.Serializable;

import org.nuxeo.ecm.core.cache.CacheServiceImpl.CachePubSubInvalidator;

/**
 * Wrapper managing cache invalidations.
 *
 * @since 9.3
 */
public class CacheInvalidator extends CacheWrapper {

    protected final CachePubSubInvalidator invalidator;

    public CacheInvalidator(CacheManagement cache, CachePubSubInvalidator invalidator) {
        super(cache);
        this.invalidator = invalidator;
    }

    @Override
    public void put(String key, Serializable value) {
        super.put(key, value);
        invalidator.sendInvalidation(getName(), key);
    }

    @Override
    public void invalidate(String key) {
        super.invalidate(key);
        invalidator.sendInvalidation(getName(), key);
    }

    @Override
    public void invalidateAll() {
        super.invalidateAll();
        invalidator.sendInvalidationsAll(getName());
    }

}
