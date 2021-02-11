/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */

package org.nuxeo.ecm.core.model;

import org.nuxeo.ecm.core.api.repository.FulltextConfiguration;

/**
 * Interface to manage a low-level repository.
 */
public interface Repository {

    String getName();

    Session getSession();

    void shutdown();

    int getActiveSessionsCount();

    /**
     * Marks the binaries in use by passing them to the binary manager(s)'s GC mark() method.
     *
     * @since 7.4
     */
    void markReferencedBinaries();

    /**
     * Gets the fulltext configuration for this repository.
     *
     * @since 10.3 (already available since 8.1 for DBSRepository)
     */
    FulltextConfiguration getFulltextConfiguration();

    /**
     * Checks whether this repository has the given capability.
     *
     * @since 11.5
     */
    default boolean hasCapability(String name) {
        Object value = getCapability(name);
        if (value == null) {
            return false;
        } else if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        } else if ("true".equals(value) || "false".equals(value)) {
            return Boolean.parseBoolean(value.toString());
        } else {
            throw new IllegalArgumentException("Capability " + name + " is not a boolean: " + value);
        }
    }

    /**
     * Gets the value of the given capability for this repository.
     *
     * @since 11.5
     */
    Object getCapability(String name);

    /**
     * Whether this repository has a {@code ecm:blobKeys} field which can be queried.
     *
     * @since 11.5
     */
    String CAPABILITY_QUERY_BLOB_KEYS = "queryBlobKeys";

}
