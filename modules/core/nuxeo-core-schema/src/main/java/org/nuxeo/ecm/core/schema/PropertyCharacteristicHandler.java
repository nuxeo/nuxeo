/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.schema;

import java.util.Optional;
import java.util.Set;

/**
 * Handler used to provide property's characteristics.
 *
 * @since 11.1
 */
public interface PropertyCharacteristicHandler {

    /**
     * Checks if the property represented by the given {@code schema} and {@code path} is secured.
     *
     * @param schema the schema name
     * @param path the property path to test
     * @return whether or not the given property is secured (ie: only administrators can edit it)
     */
    boolean isSecured(String schema, String path);

    /**
     * Checks if the property represented by the given {@code schema} and {@code path} is deprecated.
     *
     * @param schema the schema name (not the prefix)
     * @param path the property path to test
     * @return whether or not this property is marked as deprecated
     */
    boolean isDeprecated(String schema, String path);

    /**
     * Checks if the property represented by the given {@code schema} and {@code path} is removed.
     *
     * @param schema the schema name (not the prefix)
     * @param path the property path to test
     * @return whether or not this property is marked as removed
     */
    boolean isRemoved(String schema, String path);

    /**
     * The returned path are not Nuxeo xpath as they don't have the schema prefix.
     *
     * @param schema the schema name (not the prefix)
     * @return a {@link Set} holding the deprecated property path
     */
    Set<String> getDeprecatedProperties(String schema);

    /**
     * The returned path are not Nuxeo xpath as they don't have the schema prefix.
     *
     * @param schema the schema name (not the prefix)
     * @return a {@link Set} holding the removed property path
     */
    Set<String> getRemovedProperties(String schema);

    /**
     * Returns the fallback associated to this property if exist. Fallbacks exist when property is deprecated or
     * removed.
     *
     * @param schema the schema name (not the prefix)
     * @param path the property path to test
     * @return fallback associated to this property if exist
     */
    Optional<String> getFallback(String schema, String path);
}
