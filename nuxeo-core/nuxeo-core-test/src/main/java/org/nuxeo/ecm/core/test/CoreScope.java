/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.core.test;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.Scope;

/**
 * @author matic
 *
 */
public class CoreScope implements Scope {

    protected final ThreadLocal<Map<Key<?>, Object>> values =
            new ThreadLocal<Map<Key<?>, Object>>() {
        protected java.util.Map<Key<?>,Object> initialValue() {
            return new HashMap<Key<?>, Object>();
        };
    };

    public final static CoreScope INSTANCE = new CoreScope();

    protected CoreScope() {

    }
    public void enter() {
        values.get();
    }

    public void exit() {
        values.remove();
    }

    @Override
    public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
        return new Provider<T>() {

            @Override
            public T get() {
                Map<Key<?>, Object> scopedMap = getScopedObjectMap(key);
                T current = (T)scopedMap.get(key);
                if (current == null && !scopedMap.containsKey(key)) {
                    current = unscoped.get();
                    scopedMap.put(key, current);
                }
                return current;
            }

        };
    }

    private <T> Map<Key<?>, Object> getScopedObjectMap(Key<T> key) {
        Map<Key<?>, Object> scopedObjects = values.get();
        if (scopedObjects == null) {
          throw new OutOfScopeException("Cannot access " + key
              + " outside of a scoping block");
        }
        return scopedObjects;
      }
}
