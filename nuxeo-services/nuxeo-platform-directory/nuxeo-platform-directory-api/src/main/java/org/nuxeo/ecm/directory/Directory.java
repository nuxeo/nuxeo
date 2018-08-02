/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */

package org.nuxeo.ecm.directory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.directory.api.DirectoryDeleteConstraint;

/**
 * The directory interface.
 * <p>
 * This interface is implemented in order to create an NXDirectory. One should implement this interface in order to
 * create either a new Directory implementation or a new Directory Source.
 *
 * @author glefter@nuxeo.com
 */
// TODO: maybe separate Directory implementation and Directory Source
public interface Directory {

    /**
     * Gets the unique name of the directory, used for registering.
     *
     * @return the unique directory name
     */
    String getName();

    /**
     * Gets the schema name used by this directory.
     *
     * @return the schema name
     */
    String getSchema();

    /**
     * Gets the name of the parent directory. This is used for hierarchical vocabularies.
     *
     * @return the name of the parent directory, or null.
     */
    String getParentDirectory();

    /**
     * Gets the id field of the schema for this directory.
     *
     * @return the id field.
     */
    String getIdField();

    /**
     * Gets the password field of the schema for this directory.
     *
     * @return the password field.
     */
    String getPasswordField();

    /**
     * Checks if this directory is read-only.
     *
     * @since 8.2
     */
    boolean isReadOnly();

    /**
     * Shuts down the directory.
     */
    void shutdown();

    /**
     * Creates a session for accessing entries in this directory.
     *
     * @return a Session object
     */
    Session getSession();

    /**
     * Lookup a Reference by field name.
     *
     * @return the matching reference implementation or null
     * @deprecated since 7.4, kept for compatibility with old code, use {@link #getReferences(String)} instead
     */
    @Deprecated
    Reference getReference(String referenceFieldName);

    /**
     * Lookup the References by field name.
     *
     * @return the matching references implementation or null
     */
    List<Reference> getReferences(String referenceFieldName);

    /**
     * Lookup all References defined on the directory.
     *
     * @return all registered references
     */
    Collection<Reference> getReferences();

    /**
     * Gets the cache instance of the directory
     *
     * @return the cache of the directory
     */
    DirectoryCache getCache();

    /**
     * Invalidates the cache instance of the directory
     */
    void invalidateDirectoryCache();

    /**
     * Returns {@code true} if this directory is a multi tenant directory, {@code false} otherwise.
     *
     * @since 5.6
     */
    boolean isMultiTenant();

    /**
     * @since 8.4
     */
    List<String> getTypes();

    /**
     * @since 8.4
     */
    List<DirectoryDeleteConstraint> getDirectoryDeleteConstraints();

    /**
     * Invalidate caches
     *
     * @since 9.2
     */
    void invalidateCaches();

    /**
     * Get schema field map
     *
     * @since 9.2
     */
    Map<String, Field> getSchemaFieldMap();

    /**
     * Get descriptor
     *
     * @since 9.2
     */
    BaseDirectoryDescriptor getDescriptor();

}
