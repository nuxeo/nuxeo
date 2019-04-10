/*
 * (C) Copyright 2010-13 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
        return new Cached<E>(expireMillis);
    }
}
