/*
 * (C) Copyright 2011-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.binary;

import java.io.File;
import java.io.IOException;

/**
 * Interface to store a file or fetch a file or its length.
 *
 * @since 5.9.2
 */
public interface FileStorage {

    /**
     * Stores a file based on a key.
     *
     * @param key the file key
     * @param file the file
     * @throws IOException if a storage error occurred
     */
    void storeFile(String key, File file) throws IOException;

    /**
     * Fetches a file based on its key.
     *
     * @param key the file key
     * @param file the file to use to store the fetched data
     * @return {@code true} if the file was fetched, {@code false} if the file
     *         was not found
     * @throws IOException if a storage error occurred
     */
    boolean fetchFile(String key, File file) throws IOException;

    /**
     * Fetches the length of a file based on its key.
     *
     * @param key the file key
     * @return the length, or {@code null} if the file was not found
     * @throws IOException if a storage error occurred
     */
    Long fetchLength(String key) throws IOException;

}
