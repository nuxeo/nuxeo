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

import static org.nuxeo.ecm.directory.api.DirectoryConstants.EXTERNAL_ID_TYPE;
import static org.nuxeo.ecm.directory.api.DirectoryConstants.READONLY_ENTRY_FLAG;
import static org.nuxeo.ecm.directory.api.DirectoryConstants.SYSTEM_SCHEMA;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.collections4.MapUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
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
public interface Directory {

    /**
     * INTERNAL, DO NOT CALL. Initializes the directory when Nuxeo starts. Called without a transaction.
     *
     * @since 10.10
     */
    void initialize();

    /**
     * INTERNAL, DO NOT CALL. Initializes the directory when Nuxeo starts. Called without a transaction.
     *
     * @since 10.10
     */
    void initializeReferences();

    /**
     * INTERNAL, DO NOT CALL. Initializes the directory when Nuxeo starts. Called without a transaction.
     *
     * @since 10.10
     */
    void initializeInverseReferences();

    /**
     * Loads a CSV into a Directory.
     *
     * @see BaseDirectoryDescriptor#DATA_LOADING_POLICIES
     * @since 11.1
     */
    void loadFromCSV(Blob dataBlob, String dataLoadingPolicy);

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
     * Returns a bare document model suitable for directory implementations.
     *
     * @return the directory entry
     * @since 2025.9
     */
    default DocumentModel createBareDocumentModel() {
        return createBareDocumentModel(null, null);
    }

    /**
     * Returns a bare document model suitable for directory implementations.
     *
     * @param id the entry id, or {@code null}
     * @param values the entry values, or {@code null}
     * @return the directory entry
     * @since 2025.9
     */
    default DocumentModel createBareDocumentModel(@Nullable String id, @Nullable Map<String, Object> values) {
        var schemaNames = new ArrayList<>(List.of(getSchema()));
        if (getTypes().contains(EXTERNAL_ID_TYPE)) {
            schemaNames.add(SYSTEM_SCHEMA);
        }
        DocumentModelImpl entry = new DocumentModelImpl(getSchema(), id, null, null, null,
                schemaNames.toArray(String[]::new), new HashSet<>(), null, false, null, null, null);
        var nonNullValues = MapUtils.emptyIfNull(values);
        schemaNames.forEach(name -> entry.addDataModel(new DataModelImpl(name, nonNullValues)));
        if (isReadOnly()) {
            entry.putContextData(READONLY_ENTRY_FLAG, Boolean.TRUE);
        }
        return entry;
    }

    /**
     * Get descriptor
     *
     * @since 9.2
     */
    BaseDirectoryDescriptor getDescriptor();

}
