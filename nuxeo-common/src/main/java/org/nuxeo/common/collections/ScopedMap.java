/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: ScopedMap.java 20283 2007-06-11 09:45:21Z dmihalache $
 */

package org.nuxeo.common.collections;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Scoped map holding data for a given scope.
 * <p>
 * Used to store context data and invalidate some data given its scope.
 * Implements Map for easier use from interface.
 *
 * @see ScopeType
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class ScopedMap extends HashMap<String, Serializable> {

    private static final Log log = LogFactory.getLog(ScopedMap.class);

    private static final long serialVersionUID = -616031057117818344L;

    /**
     * Gets value for given scope and given key.
     */
    public Serializable getScopedValue(ScopeType scope, String key) {
        Serializable res = null;
        if (scope != null && key != null) {
            res = get(scope.getScopedKey(key));
        }
        return res;
    }

    /**
     * Gets value for given key using default scope.
     */
    public Serializable getScopedValue(String key) {
        return getScopedValue(ScopeType.DEFAULT, key);
    }

    /**
     * Creates a Map with entries from DEFAULT scope.
     */
    public Map<String, Serializable> getDefaultScopeValues() {
        return getScopeValues(ScopeType.DEFAULT);
    }

    /**
     * Creates a Map with entries from specified scope.
     */
    public Map<String, Serializable> getScopeValues(ScopeType scopeType) {
        Map<String, Serializable> defMap = new HashMap<String, Serializable>();

        final String defaultScopePrefix = scopeType.getScopePrefix();
        final int prefixLen = defaultScopePrefix.length();
        for (Map.Entry<String, Serializable> entry : entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(defaultScopePrefix)) {
                defMap.put(key.substring(prefixLen), entry.getValue());
            }
        }

        return defMap;
    }

    /**
     * Sets value for given scope and given key.
     */
    public void putScopedValue(ScopeType scope, String key,
            Serializable value) {
        if (scope == null || key == null) {
            log.error(String.format(
                    "Cannot set scope value using scopeType=%s and key=%s",
                    scope, key));
        } else {
            put(scope.getScopedKey(key), value);
        }
    }

    /**
     * Sets key using default scope.
     */
    public void putScopedValue(String key, Serializable value) {
        putScopedValue(ScopeType.DEFAULT, key, value);
    }

    /**
     * Removes all mappings for given scope.
     */
    public void clearScope(ScopeType scopeType) {
        if (scopeType == null) {
            log.error("Cannot clear map, specified scope is null");
        } else {
            String prefix = scopeType.getScopePrefix();
            List<String> toRemove = new ArrayList<String>();
            for (Map.Entry<String, Serializable> entry : entrySet()) {
                String key = entry.getKey();
                if (key.startsWith(prefix)) {
                    toRemove.add(key);
                }
            }
            for (String key : toRemove) {
                remove(key);
            }
        }
    }

}
