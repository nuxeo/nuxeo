/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.user.preferences.api;

import java.util.Map;

import jakarta.annotation.Nonnull;

/**
 * A collection of {@link UserPreference}.
 *
 * @since 2025.16
 */
public interface UserPreferences {

    /**
     * Gets the preference value associated with the given key.
     *
     * @param key the key
     * @return the value
     */
    default String getPreference(String key) {
        return preferences().get(key);
    }

    /**
     * @return true if the collection contains no preferences.
     */
    default boolean isEmpty() {
        return preferences().isEmpty();
    }

    /**
     * Gets the preferences complex property value of the document as a map.
     * <p>
     * If the value is {@code null}, then an empty map is returned.
     *
     * @return the map of key / value preferences
     */
    @Nonnull
    Map<String, String> preferences();

    /**
     * @return the number of preferences
     */
    default int size() {
        return preferences().size();
    }

}
