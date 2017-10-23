/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.storage.dbs;

import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PREFIX;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.nuxeo.ecm.core.storage.State;

/**
 * Function to flatten and convert {@link State} into NXQL {@link Map}&lt;{@link String}, {@link Serializable}&gt;.
 *
 * @since 8.3
 */
public class DBSStateFlattener {

    protected final Map<String, String> keyMappings;

    public DBSStateFlattener() {
        this.keyMappings = Collections.emptyMap();
    }

    public DBSStateFlattener(Map<String, String> keyMappings) {
        this.keyMappings = keyMappings != null ? keyMappings : Collections.emptyMap();
    }

    /**
     * Flattens with optional property key mappings.
     * @param state state
     * @return flattened result
     * @since 9.3
     */
    public Map<String, Serializable> flatten(State state) {
        Map<String, Serializable> map = new HashMap<>();
        flatten(map, state, null);
        return map;
    }

    protected void flatten(Map<String, Serializable> map, State state, String prefix) {
        for (Entry<String, Serializable> en : state.entrySet()) {
            String key = en.getKey();
            Serializable value = en.getValue();
            String name;
            String realName = keyMappings.get(key);
            if (realName != null) {
                name = realName;
            } else if (key.startsWith(KEY_PREFIX)) {
                name = DBSSession.convToNXQL(key);
                if (name == null) {
                    // present in state but not returned to caller
                    continue;
                }
            } else {
                name = key;
            }
            name = prefix == null ? name : prefix + name;
            if (value instanceof State) {
                flatten(map, (State) value, name + '/');
            } else if (value instanceof List) {
                String nameSlash = name + '/';
                int i = 0;
                for (Object v : (List<?>) value) {
                    if (v instanceof State) {
                        flatten(map, (State) v, nameSlash + i + '/');
                    } else {
                        map.put(nameSlash + i, (Serializable) v);
                    }
                    i++;
                }
            } else if (value instanceof Object[]) {
                String nameSlash = name + '/';
                int i = 0;
                for (Object v : (Object[]) value) {
                    map.put(nameSlash + i, (Serializable) v);
                    i++;
                }
            } else {
                map.put(name, value);
            }
        }
    }

}
