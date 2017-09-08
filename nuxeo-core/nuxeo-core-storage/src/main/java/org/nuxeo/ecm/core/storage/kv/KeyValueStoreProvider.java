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
package org.nuxeo.ecm.core.storage.kv;

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
     * Closes this Key/Value store provider.
     */
    void close();

    /**
     * Clears the content of this Key/Value store provider.
     */
    void clear();

}
