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
import java.util.Optional;

import jakarta.annotation.Nonnull;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;

/**
 * Service providing the storage and retrieval of user preferences.
 * <p>
 * There are two different user preferences:
 * <ul>
 * <li>{@link UserPreference}: a global preference entity providing a {@code value} for a given {@code key}.
 * <li>{@link UserPreferences}: an entity handling a collection of {@link UserPreference} used to hold all the
 * preferences for a given document or list all the current user global preference.
 * </ul>
 * <p>
 * {@code key} must contain letters, digits, dashes, underscores or periods characters only. By default, {@code value}
 * is html-sanitized before being persisted. Sanitizing can be disabled with the
 * {@link UserPreferencesUtil#PROP_SANITIZE_VALUES} property. Null {@code value} is rejected.
 * <p>
 * User preferences are saved per repository, meaning 2 preferences with the same key but for different repositories can
 * exist.
 * <p>
 * The maximum number of preferences for a given user and repository is defined by the
 * {@link UserPreferencesUtil#PROP_MAX_PREFERENCES} property (default value:
 * {@link UserPreferencesUtil#DEFAULT_PROP_MAX_PREFERENCES}). If this limit is exceeded, a
 * {@link org.nuxeo.user.preferences.exception.TooManyUserPreferencesException} is thrown.
 *
 * @since 2025.16
 */
public interface UserPreferencesService {

    /**
     * Gets an optional current user's preference for the given key.
     *
     * @param session the core session
     * @param key the preference key
     * @return the user preference entity if any
     */
    Optional<UserPreference> get(CoreSession session, @Nonnull String key);

    /**
     * Gets current user's preferences for the given document reference.
     *
     * @param session the core session
     * @param docRef the document reference
     * @return the user document preferences entity
     */
    UserPreferences get(CoreSession session, @Nonnull DocumentRef docRef);

    /**
     * Gets optional current user's preference for the given document reference and key.
     *
     * @param session the core session
     * @param docRef the document reference
     * @param key the user preference key
     * @return the user document preferences entity if any
     */
    Optional<UserPreference> get(CoreSession session, @Nonnull DocumentRef docRef, @Nonnull String key);

    /**
     * Lists all current user's global preferences.
     *
     * @param session the core session
     * @return all the user preference entities
     */
    UserPreferences list(CoreSession session);

    /**
     * Creates a current user's preference value for the given key.
     *
     * @param session the core session
     * @param key the preference key
     * @param value the preference value
     * @return the created user preference
     * @throws org.nuxeo.user.preferences.exception.UserPreferencesExistsException if a {@link UserPreference} already
     *             exists for the current user and key.
     * @throws org.nuxeo.user.preferences.exception.TooManyUserPreferencesException if the number of preferences exceeds
     *             {@link UserPreferencesUtil#PROP_MAX_PREFERENCES}
     */
    UserPreference create(CoreSession session, @Nonnull String key, @Nonnull String value);

    /**
     * Creates or updates a current user's preference.
     *
     * @param session the core session
     * @param key the user preference key
     * @param value the preference value
     * @return the updated user preference
     */
    UserPreference createOrUpdate(CoreSession session, @Nonnull String key, @Nonnull String value);

    /**
     * Creates current user's document preferences.
     *
     * @param session the core session
     * @param docRef the document reference
     * @param preferences the key / preferences map
     * @return the created user document preferences
     * @throws org.nuxeo.user.preferences.exception.UserPreferencesExistsException if a {@link UserPreferences} already
     *             exists for the current user and document reference.
     * @throws org.nuxeo.user.preferences.exception.TooManyUserPreferencesException if the number of preferences exceeds
     *             {@link UserPreferencesUtil#PROP_MAX_PREFERENCES}
     * @apiNote an empty preferences map is considered as a null preference, and so rejected
     */
    UserPreferences create(CoreSession session, @Nonnull DocumentRef docRef, @Nonnull Map<String, String> preferences);

    /**
     * Updates a current user's preference value.
     *
     * @param session the core session
     * @param key the user preference key
     * @param value the preference new value
     * @return the updated user preference
     * @throws org.nuxeo.user.preferences.exception.UserPreferencesNotFound if the user preference does not exist
     */
    UserPreference update(CoreSession session, @Nonnull String key, @Nonnull String value);

    /**
     * Updates a current user's document preferences.
     * <p>
     * Existing document preferences are erased unlike {@link #putAll} which preserves existing document preferences.
     *
     * @param session the core session
     * @param preferences the user preferences to be updated
     * @return the updated user document preferences
     * @throws org.nuxeo.user.preferences.exception.UserPreferencesNotFound if the user document preferences does not
     *             exist
     * @throws org.nuxeo.user.preferences.exception.TooManyUserPreferencesException if the number of preferences exceeds
     *             {@link UserPreferencesUtil#PROP_MAX_PREFERENCES}
     * @apiNote an empty preferences map is considered as a null preference, and so rejected
     */
    UserPreferences update(CoreSession session, @Nonnull DocumentRef docRef, @Nonnull UserPreferences preferences);

    /**
     * Puts all current user's document preferences. If no {@link UserPreferences} exists for current user and document
     * reference, it is created.
     * <p>
     * Existing document preferences are preserved unless explicitly overwritten by the given {@link UserPreferences}.
     *
     * @param session the core session
     * @param preferences the user preferences
     * @return the created or updated user document preferences
     * @throws org.nuxeo.user.preferences.exception.TooManyUserPreferencesException if the number of preferences exceeds
     *             {@link UserPreferencesUtil#PROP_MAX_PREFERENCES}
     * @apiNote an empty {@link UserPreferences#preferences()} map is considered as a null preference, and so rejected
     */
    UserPreferences putAll(CoreSession session, @Nonnull DocumentRef docRef, @Nonnull UserPreferences preferences);

    /**
     * Deletes the current user's preference for the given key.
     *
     * @param session the core session
     * @param key the user preference key
     */
    void delete(CoreSession session, @Nonnull String key);

    /**
     * Deletes all the current user's preferences for the given document reference.
     *
     * @param session the core session
     * @param docRef the user preferences document reference
     */
    void delete(CoreSession session, @Nonnull DocumentRef docRef);

    /**
     * Removes the current user's preference for the given document reference and key.
     *
     * @param session the core session
     * @param docRef the document reference
     * @param key the preference key
     * @return the updated user preferences
     */
    UserPreferences remove(CoreSession session, @Nonnull DocumentRef docRef, @Nonnull String key);

    /**
     * Internal use.
     * <p>
     * Deletes all user preferences for the given document reference. The core session must have sufficient privileges
     * to delete preferences of all users.
     *
     * @param session the core session
     * @param idRef the document id reference
     */
    void deleteAllForDocument(CoreSession session, @Nonnull IdRef idRef);

    /**
     * Internal use.
     * <p>
     * Deletes all preferences for the given user id. The core session must have sufficient privileges to delete
     * preferences of the given user id.
     *
     * @param session the core session
     * @param userId the user id
     */
    void deleteAllForUser(CoreSession session, @Nonnull String userId);

}
