/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.kv;

import java.util.stream.Stream;

/**
 * Key/Value Store SPI.
 *
 * @since 9.1
 */
public interface KeyValueStoreProvider extends KeyValueStore {

    /**
     * Initializes this Key/Value store provider.
     *
     * @param descriptor the store provider descriptor
     */
    void initialize(KeyValueStoreDescriptor descriptor);

    /**
     * Returns a {@link Stream} of the keys contained in this Key/Value store provider.
     * <p>
     * This operation may be slow and should only be used for management or debug purposes.
     *
     * @return the stream of keys
     * @since 9.3
     */
    Stream<String> keyStream();

    /**
     * Returns a {@link Stream} of the keys with the given prefix contained in this Key/Value store provider.
     * <p>
     * This operation may be slow and should only be used for management or debug purposes.
     *
     * @return the stream of keys
     * @since 10.3
     */
    Stream<String> keyStream(String prefix);

    /**
     * Closes this Key/Value store provider.
     */
    void close();

    /**
     * Clears the content of this Key/Value store provider.
     */
    void clear();

}
