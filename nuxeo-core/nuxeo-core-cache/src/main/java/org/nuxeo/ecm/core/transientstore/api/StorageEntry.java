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
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;

/**
 * Represents an entry that can be stored inside a {@link TransientStore}. The entry is will be stored in 2 sub parts :
 * the Blobs that will be stored in file system, and the java attributes that will be kept in memory
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */
public interface StorageEntry extends Serializable {

    /**
     * Returns the id associated with an entry In the default implementation this id must an ascii alphanumeric string
     *
     * @return the id of the entry
     */
    String getId();

    /**
     * Set the Blobs that must be associated with the entry
     *
     * @param blobs
     */
    void setBlobs(List<Blob> blobs);

    /**
     * Adds a {@link Blob} to the list of persisted Blobs
     *
     * @param blob
     * @return
     */
    List<Blob> addBlob(Blob blob);

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
     * Reads the value of named parameters
     *
     * @param key
     * @return
     */
    Serializable get(String key);

    /**
     * Callback to do some cleanup before entry is removed from the {@link TransientStore}
     */
    void beforeRemove();

    /**
     * Called by {@link TransientStore} to persist the Blobs to disk and then be sure that the entry can be Serialized
     * without loosing any data
     *
     * @param directory
     * @throws IOException
     */
    void persist(File directory) throws IOException;

    /**
     * Called by {@link TransientStore} to load Blobs from disk
     *
     * @param directory
     * @throws IOException
     */
    void load(File directory) throws IOException;

    /**
     *
     * @param other
     */
    void update(StorageEntry other);

    /**
     * Returns the size of the persisted Blobs
     *
     * @return
     */
    long getSize();
}
