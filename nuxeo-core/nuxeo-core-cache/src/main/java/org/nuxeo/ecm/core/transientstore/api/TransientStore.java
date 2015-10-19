/*
 * (C) Copyright 2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.transientstore.api;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.annotation.Experimental;
import org.nuxeo.ecm.core.api.Blob;

/**
 * Service Interface for managing a transient store.
 * <p>
 * Allows to store entries in 2 sub parts: a list of blobs stored on a file system along with a map of parameters.
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */
@Experimental(comment = "https://jira.nuxeo.com/browse/NXP-16577")
public interface TransientStore {

    /**
     * Initializes the store from the given {@code config}.
     */
    void init(TransientStoreConfig config);

    /**
     * Shuts down the store.
     */
    void shutdown();

    /**
     * Returns {@code true} if an entry exists with the given {@code key}.
     *
     * @since 7.10
     */
    boolean exists(String key);

    /**
     * Sets {@code parameter} to {@code value} in the entry with the given {@code key}.
     * <p>
     * If entry does not exist a new entry is created. If {@code parameter} already exists in the entry it is
     * overwritten.
     *
     * @since 7.10
     */
    void putParameter(String key, String parameter, Serializable value);

    /**
     * Gets the value of {@code parameter} in the entry with the given {@code key}.
     * <p>
     * Returns {@code null} if entry or parameter does not exist.
     *
     * @since 7.10
     */
    Serializable getParameter(String key, String parameter);

    /**
     * Puts {@code parameters} in the entry with the given {@code key}. Overwrites any existing parameter in the entry.
     * <p>
     * If entry does not exist a new entry is created.
     *
     * @since 7.10
     */
    void putParameters(String key, Map<String, Serializable> parameters);

    /**
     * Gets values of the parameters in the entry with the given {@code key}.
     * <p>
     * Returns {@code null} if entry does not exist.
     *
     * @since 7.10
     */
    Map<String, Serializable> getParameters(String key);

    /**
     * Associates the given {@code blobs} with the entry with the given {@code key}.
     * <p>
     * If entry does not exist a new entry is created.
     *
     * @since 7.10
     */
    void putBlobs(String key, List<Blob> blobs);

    /**
     * Gets the blobs associated with the entry with the given {@code key}.
     * <p>
     * Returns {@code null} if entry does not exist.
     *
     * @since 7.10
     */
    List<Blob> getBlobs(String key);

    /**
     * Returns the size of the blobs associated with the entry with the given {@code key} or {@code -1} if entry does
     * not exist.
     *
     * @since 7.10
     */
    long getSize(String key);

    /**
     * Returns {@code true} if the entry with the given {@code key} is ready.
     *
     * @since 7.10
     */
    boolean isCompleted(String key);

    /**
     * Marks the entry with the given {@code key} as ready.
     * <p>
     * If entry does not exist a new entry is created.
     *
     * @since 7.10
     */
    void setCompleted(String key, boolean completed);

    /**
     * Removes entry with the given {@code key}.
     * <p>
     * Has no effect if entry does not exist.
     */
    void remove(String key);

    /**
     * Informs the store that the entry with the given {@code key} can be released if TTL or GC parameters require to do
     * some cleanup.
     * <p>
     * Has no effect if entry does not exist.
     */
    void release(String key);

    /**
     * Returns the size of the used disk storage in MB.
     */
    int getStorageSizeMB();

    /**
     * Runs garbage collecting to delete the file system resources that are associated with entries that were removed.
     */
    void doGC();

    /**
     * Removes all entries from the store.
     */
    void removeAll();

}
