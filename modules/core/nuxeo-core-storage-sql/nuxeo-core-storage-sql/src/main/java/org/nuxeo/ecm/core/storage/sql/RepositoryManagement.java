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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import java.util.Calendar;

import org.nuxeo.ecm.core.api.repository.FulltextConfiguration;

/**
 * @author Florent Guillaume
 */
public interface RepositoryManagement {

    /**
     * Gets the repository name.
     */
    String getName();

    /**
     * Gets the number of active sessions.
     */
    int getActiveSessionsCount();

    /**
     * Evaluate number of elements in all caches
     *
     * @since 5.7.2
     */
    long getCacheSize();

    /**
     * Evaluate number of elements in hier cache
     *
     * @since 5.7.2
     */
    long getCachePristineSize();

    /**
     * Evaluate number of elements in selection cache
     *
     * @since 5.7.2
     */
    long getCacheSelectionSize();

    /**
     * Clears all the caches.
     *
     * @return an indicative count of objects removed
     */
    int clearCaches();

    /**
     * Makes sure that the next transaction will process cluster invalidations.
     */
    void processClusterInvalidationsNext();

    /**
     * Marks the binaries in use by passing them to the binary manager(s)'s GC mark() method.
     */
    void markReferencedBinaries();

    /**
     * Cleans up (hard-deletes) any documents that have been soft-deleted in the database.
     *
     * @param max the maximum number of documents to delete at a time
     * @param beforeTime the maximum deletion time of the documents to delete
     * @return the number of documents deleted
     */
    int cleanupDeletedDocuments(int max, Calendar beforeTime);

    /**
     * Gets the fulltext configuration.
     *
     * @since 10.3
     */
    FulltextConfiguration getFulltextConfiguration();

}
