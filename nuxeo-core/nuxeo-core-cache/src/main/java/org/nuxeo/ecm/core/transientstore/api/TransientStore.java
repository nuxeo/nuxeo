/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat <tdelprat@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */

package org.nuxeo.ecm.core.transientstore.api;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.Blob;

/**
 * Service Interface for managing a transient store.
 * <p>
 * Allows to store entries in 2 sub parts: a list of blobs stored on a file system along with a map of parameters.
 *
 * @since 7.2
 */
public interface TransientStore {

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

}
