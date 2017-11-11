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

import java.util.Collection;
import java.util.Map;

/**
 * Key/Value Store.
 * <p>
 * This is the interface for a Key/Value store, which stores simple values associated to keys.
 * <p>
 * A Key/Value store is thread-safe.
 *
 * @since 9.1
 */
public interface KeyValueStore {

    /**
     * Sets the value associated to the key.
     *
     * @param key the key
     * @param value the value, which may be {@code null}
     */
    void put(String key, byte[] value);

    /**
     * Sets the value associated to the key.
     *
     * @param key the key
     * @param value the value, which may be {@code null}
     * @since 9.3
     */
    void put(String key, String value);

    /**
     * Sets the value associated to the key, and a TTL.
     *
     * @param key the key
     * @param value the value, which may be {@code null}
     * @param ttl the TTL, in seconds (0 for infinite)
     * @since 9.3
     */
    void put(String key, byte[] value, long ttl);

    /**
     * Sets the value associated to the key, and a TTL.
     *
     * @param key the key
     * @param value the value, which may be {@code null}
     * @param ttl the TTL, in seconds (0 for infinite)
     * @since 9.3
     */
    void put(String key, String value, long ttl);

    /**
     * Sets the TTL for an existing key.
     *
     * @param key the key
     * @param ttl the TTL, in seconds (0 for infinite)
     * @return {@code true} if the TTL has been set, or {@code false} if the key does not exist
     * @since 9.3
     */
    boolean setTTL(String key, long ttl);

    /**
     * Retrieves the value associated to the key.
     *
     * @param key the key
     * @return the value, or {@code null} if there is no value
     */
    byte[] get(String key);

    /**
     * Retrieves the value associated to the key.
     *
     * @param key the key
     * @return the value, or {@code null} if there is no value
     * @throws IllegalArgumentException if the value cannot be returned as a {@link String}
     * @since 9.3
     */
    String getString(String key);

    /**
     * Retrieves the key/value map associated with the keys.
     *
     * @param keys the keys
     * @return the key/value map
     * @since 9.10
     */
    Map<String, byte[]> get(Collection<String> keys);

    /**
     * Retrieves the key/value map associated with the  keys.
     *
     * @param keys the keys
     * @return the key/value map
     * @throws IllegalArgumentException if one of the values cannot be returned as a {@link String}
     * @since 9.10
     */
    Map<String, String> getStrings(Collection<String> keys);

    /**
     * Atomically sets the value associated to the key to the given value if the current value is the expected value.
     * <p>
     * Note value comparison is done by value and not by reference.
     *
     * @param key the key
     * @param expected the expected value, which may be {@code null}
     * @param value the updated value, which may be {@code null}
     * @return {@code true} if the value was updated, or {@code false} if not (the expected value was not found)
     */
    boolean compareAndSet(String key, byte[] expected, byte[] value);

    /**
     * Atomically sets the value associated to the key to the given value, with the given TTL, if the current value is
     * the expected value.
     * <p>
     * Note value comparison is done by value and not by reference.
     *
     * @param key the key
     * @param expected the expected value, which may be {@code null}
     * @param value the updated value, which may be {@code null}
     * @param ttl the TTL, in seconds (0 for infinite)
     * @return {@code true} if the value was updated, or {@code false} if not (the expected value was not found)
     * @since 9.3
     */
    boolean compareAndSet(String key, byte[] expected, byte[] value, long ttl);

    /**
     * Atomically sets the value associated to the key to the given value if the current value is the expected value.
     * <p>
     * Note value comparison is done by value and not by reference.
     *
     * @param key the key
     * @param expected the expected value, which may be {@code null}
     * @param value the updated value, which may be {@code null}
     * @return {@code true} if the value was updated, or {@code false} if not (the expected value was not found)
     * @since 9.3
     */
    boolean compareAndSet(String key, String expected, String value);

    /**
     * Atomically sets the value associated to the key to the given value, with the given TTL, if the current value is
     * the expected value.
     * <p>
     * Note value comparison is done by value and not by reference.
     *
     * @param key the key
     * @param expected the expected value, which may be {@code null}
     * @param value the updated value, which may be {@code null}
     * @param ttl the TTL, in seconds (0 for infinite)
     * @return {@code true} if the value was updated, or {@code false} if not (the expected value was not found)
     * @since 9.3
     */
    boolean compareAndSet(String key, String expected, String value, long ttl);

}
