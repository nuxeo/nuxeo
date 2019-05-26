/*
 * (C) Copyright 2010-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *    Olivier Grisel
 *
 */
package org.nuxeo.ecm.platform.suggestbox.jsf;

import java.io.Serializable;

/**
 * Simple cached item holder with time + key invalidation strategy
 */
public class Cached<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    public long cachedAt;

    public long expireMillis;

    public Object[] keys = new Object[0];

    public T value;

    public boolean expired = false;

    public Cached(long expireMillis) {
        this.expireMillis = expireMillis;
        // expired by default
        expired = true;
    }

    public void cache(T value, Object... keys) {
        this.expired = false;
        this.cachedAt = System.currentTimeMillis();
        this.keys = keys;
        this.value = value;
    }

    public void expire() {
        expired = true;
        value = null;
        keys = new Object[0];
    }

    public boolean hasExpired(Object... invalidationKeys) {
        if (expired) {
            return true;
        }
        long now = System.currentTimeMillis();
        if (now - cachedAt > expireMillis) {
            return true;
        }
        if (invalidationKeys.length != keys.length) {
            return true;
        }
        for (int i = 0; i < keys.length; i++) {
            if (!keys[i].equals(invalidationKeys[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Empty marker to be used as default value
     */
    public static <E> Cached<E> expired(long expireMillis) {
        return new Cached<>(expireMillis);
    }
}
