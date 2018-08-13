/*
 * (C) Copyright 2006-2007 Nuxeo (http://nuxeo.com/) and others.
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
 *     Dragos Mihalache
 */
package org.nuxeo.ecm.core.uidgen;

import java.util.ArrayList;
import java.util.List;

/**
 * UID Sequencer interface defines a method to retrieve next ids based on a given key.
 */
public interface UIDSequencer {

    /**
     * Gets the sequencer name.
     *
     * @since 7.4
     */
    String getName();

    /**
     * Sets the sequencer name.
     *
     * @since 7.4
     */
    void setName(String name);

    /**
     * Init Sequencer
     *
     * @since 7.3
     */
    void init();

    /**
     * Initializes the sequencer with the given key to at least the given long id.
     * <p>
     * A sequence can only be incremented, so if its current id is greater than the given id the sequence won't be
     * decremented to reach the given id.
     *
     * @since 9.10
     */
    void initSequence(String key, long id);

    /**
     * Initializes the sequencer with the given key to at least the given id.
     * @since 7.4
     * @deprecated since 9.10 use {@link #initSequence(String, long)} instead.
     */
    @Deprecated
    void initSequence(String key, int id);

    /**
     * For the given key returns the incremented UID which is also stored in the same sequence entry. This is a
     * "one time use" function for a document.
     *
     * @deprecated since 9.10 use {@link #getNextLong(String)} instead.
     */
    @Deprecated
    int getNext(String key);

    /**
     * Extends {@link UIDSequencer#getNext(java.lang.String)} to return a long value. This method is compatible
     * with getNext in the integer range.
     *
     * @since 8.3
     */
    long getNextLong(String key);

    /**
     * Returns a block containing {@code blockSize} sequences.
     *
     * @since 10.3
     */
    default List<Long> getNextBlock(String key, int blockSize) {
        List<Long> ret = new ArrayList<>(blockSize);
        for (int i = 0; i < blockSize; i++) {
            ret.add(getNextLong(key));
        }
        return ret;
    }

    /**
     * Cleanup callback
     *
     * @since 7.3
     */
    void dispose();

}
