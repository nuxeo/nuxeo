/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.schema;

import static org.apache.commons.lang.ObjectUtils.NULL;
import static org.nuxeo.ecm.core.schema.types.ComplexTypeImpl.canonicalXPath;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.runtime.api.Framework;

/**
 * Information about what's to be prefetched: individual properties and whole schemas.
 */
public class Prefetch implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * map of prefix:name -> value
     * <p>
     * key can be a canonical xpath like prefix:name/0/othername
     * <p>
     * null values are stored as actual nulls
     */
    public Map<String, Serializable> values;

    /**
     * map of schema -> list of prefix:name
     */
    public Map<String, List<String>> keysBySchema;

    /**
     * map of schema -> name -> prefix:name
     */
    public Map<String, Map<String, String>> keysBySchemaAndName;

    public Prefetch() {
        values = new HashMap<String, Serializable>();
        keysBySchema = new HashMap<String, List<String>>();
        keysBySchemaAndName = new HashMap<String, Map<String, String>>();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public void put(String prefixedName, String schemaName, String name, Serializable value) {
        values.put(prefixedName, value);
        if (schemaName != null) {
            Map<String, String> keysByName = keysBySchemaAndName.get(schemaName);
            if (keysByName == null) {
                keysBySchemaAndName.put(schemaName, keysByName = new HashMap<String, String>());
            }
            keysByName.put(name, prefixedName);
            List<String> keys = keysBySchema.get(schemaName);
            if (keys == null) {
                keysBySchema.put(schemaName, keys = new LinkedList<String>());
            }
            keys.add(prefixedName);
        }
    }

    public Serializable get(String xpath) {
        xpath = canonicalXPath(xpath);
        if (values.containsKey(xpath)) {
            return cloned(values.get(xpath));
        }
        return NULL;
    }

    public Serializable get(String schemaName, String name) {
        Map<String, String> keysByName = keysBySchemaAndName.get(schemaName);
        if (keysByName != null) {
            String prefixedName = keysByName.get(name);
            if (prefixedName != null && values.containsKey(prefixedName)) {
                return cloned(values.get(prefixedName));
            }
        }
        return NULL;
    }

    // make sure we return a new array
    protected Serializable cloned(Serializable value) {
        if (value instanceof Object[]) {
            value = ((Object[]) value).clone();
        }
        return value;
    }

    public boolean isPrefetched(String xpath) {
        xpath = canonicalXPath(xpath);
        return values.containsKey(xpath);
    }

    public boolean isPrefetched(String schemaName, String name) {
        Map<String, String> keysByName = keysBySchemaAndName.get(schemaName);
        if (keysByName == null) {
            return false;
        }
        String prefixedName = keysByName.get(name);
        if (prefixedName == null) {
            return false;
        }
        return values.containsKey(prefixedName);
    }

    /**
     * Clears the prefetches for a given schema.
     */
    public void clearPrefetch(String schemaName) {
        keysBySchemaAndName.remove(schemaName);
        List<String> keys = keysBySchema.remove(schemaName);
        if (keys != null) {
            for (String prefixedName : keys) {
                values.remove(prefixedName);
            }
        }
    }

    /**
     * Gets the schema name for a given xpath.
     * <p>
     * The type is used to resolve non-prefixed properties.
     *
     * @return the schema name or {@code null}
     */
    public String getXPathSchema(String xpath, DocumentType type) {
        xpath = canonicalXPath(xpath);
        int i = xpath.indexOf('/');
        String prop = i == -1 ? xpath : xpath.substring(0, i);
        int p = prop.indexOf(':');
        if (p == -1) {
            for (Schema schema : type.getSchemas()) {
                if (schema.hasField(prop)) {
                    return schema.getName();
                }
            }
            return null;
        } else {
            String prefix = prop.substring(0, p);
            SchemaManager schemaManager = Framework.getService(SchemaManager.class);
            Schema schema = schemaManager.getSchemaFromPrefix(prefix);
            if (schema == null) {
                schema = schemaManager.getSchema(prefix);
            }
            return schema == null ? null : schema.getName();
        }
    }

}
