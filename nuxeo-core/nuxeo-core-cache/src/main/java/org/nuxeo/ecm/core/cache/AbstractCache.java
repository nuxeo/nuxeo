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

/**
 * Abstract class to be extended to provide new cache implementation
 *
 * @since 6.0
 */
public abstract class AbstractCache implements CacheManagement {

    protected final String name;

    public final long ttl;

    protected AbstractCache(CacheDescriptor desc) {
        name = desc.name;
        ttl = desc.getTTL();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void start() {
        // nothing
    }

    @Override
    public void stop() {
        // nothing
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + name + ")";
    }

}
