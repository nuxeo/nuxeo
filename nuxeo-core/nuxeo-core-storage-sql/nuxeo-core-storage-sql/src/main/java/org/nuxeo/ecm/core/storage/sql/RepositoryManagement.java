/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import java.util.Calendar;

import org.nuxeo.ecm.core.storage.binary.BinaryGarbageCollector;

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
     * @since 5.7.2
     */
    long getCachePristineSize();


    /**
     * Evaluate number of elements in selection cache
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
     * Gets the binary GC for this repository.
     *
     * @return the binary garbage collector
     */
    BinaryGarbageCollector getBinaryGarbageCollector();

    /**
     * Marks the binaries actually in use with the GC so that they won't be
     * deleted.
     * <p>
     * The passed GC may or may not be the one returned by
     * {@link #getBinaryGarbageCollector} in case it's been determined that
     * another repository's GC is pointing to the same binary data.
     *
     * @param gc the binary garbage collector to use for this repository's
     *            binaries
     */
    void markReferencedBinaries(BinaryGarbageCollector gc);

    /**
     * Cleans up (hard-deletes) any documents that have been soft-deleted in the
     * database.
     *
     * @param max the maximum number of documents to delete at a time
     * @param beforeTime the maximum deletion time of the documents to delete
     * @return the number of documents deleted
     */
    int cleanupDeletedDocuments(int max, Calendar beforeTime);

}
