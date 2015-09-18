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

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.annotation.Experimental;
import org.nuxeo.ecm.core.api.Blob;

/**
 * Represents an entry that can be stored inside a {@link TransientStore}. The entry is will be stored in 2 sub parts :
 * the Blobs that will be stored in file system, and the java attributes that will be kept in memory
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */
@Experimental(comment = "https://jira.nuxeo.com/browse/NXP-16577")
public interface StorageEntry extends Serializable {

    /**
     * Returns the id associated with an entry In the default implementation this id must an ascii alphanumeric string
     *
     * @return the id of the entry
     */
    String getId();

    /**
     * Set the Blobs that must be associated with the entry
     */
    void setBlobs(List<Blob> blobs);

    /**
     * @return the Blobs that are associated to the entry
     */
    List<Blob> getBlobs();

    /**
     * Add a named parameter to the entry
     *
     * @param key the name of the parameter
     * @param value the {@link Serializable} value
     */
    void put(String key, Serializable value);

    /**
     * Reads the value of named parameters.
     */
    Serializable get(String key);

    /**
     * Put multiple named parameters.
     */
    void putAll(Map<String, Serializable> params);

    /**
     * Returns the named parameters.
     *
     * @since 7.4
     */
    Map<String, Serializable> getParameters();

    /**
     * Callback to do some cleanup before entry is removed from the {@link TransientStore}.
     */
    void beforeRemove();

    /**
     * Called by {@link TransientStore} to persist the Blobs to disk and then be sure that the entry can be Serialized
     * without loosing any data.
     */
    void persist(File directory);

    /**
     * Called by {@link TransientStore} to load Blobs from disk.
     */
    void load(File directory);

    /**
     * Returns the size of the persisted Blobs
     */
    long getSize();

    /**
     * Returns the size of the persisted Blobs. long getSize(); /** Returns the size last time the entry was stored.
     */
    long getLastStorageSize();

    /**
     * flag to indicate if result is ready.
     *
     * @since 7.3
     */
    boolean isCompleted();

    /**
     * Mark the storage entry as ready.
     *
     * @since 7.3
     */
    void setCompleted(boolean completed);

}
