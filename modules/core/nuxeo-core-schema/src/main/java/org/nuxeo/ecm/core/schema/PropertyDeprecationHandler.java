/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.schema;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Handler used to test if a specific property is marked as deprecated/removed and to get its fallback.
 *
 * @since 9.2
 * @deprecated since 11.1, use {@link PropertyCharacteristicHandler} service instead
 */
@Deprecated(since = "11.1")
public class PropertyDeprecationHandler {

    /**
     * Deprecated/removed properties map, its mapping is:
     * <p>
     * schemaName -&gt; propertyXPath -&gt; fallbackXPath
     * </p>
     */
    protected final Map<String, Map<String, String>> properties;

    public PropertyDeprecationHandler(Map<String, Map<String, String>> properties) {
        this.properties = properties;
    }

    /**
     * @return true if the input property has deprecated/removed property
     */
    public boolean hasMarkedProperties(String schema) {
        return properties.containsKey(schema);
    }

    /**
     * Returned properties are a path to marked property.
     *
     * @return the deprecated/removed properties for input schema or an empty set if schema doesn't have marked
     *         properties
     */
    public Set<String> getProperties(String schema) {
        Map<String, String> schemaProperties = properties.get(schema);
        if (schemaProperties == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(schemaProperties.keySet());
    }

    /**
     * @return true if the input property is marked as deprecated/removed
     */
    public boolean isMarked(String schema, String name) {
        Map<String, String> schemaProperties = properties.get(schema);
        return schemaProperties != null && schemaProperties.containsKey(name);
    }

    /**
     * @return the fallback of input property, if it is marked as deprecated/removed and has a fallback
     */
    public String getFallback(String schema, String name) {
        Map<String, String> schemaProperties = properties.get(schema);
        if (schemaProperties != null) {
            return schemaProperties.get(name);
        }
        return null;
    }

}
