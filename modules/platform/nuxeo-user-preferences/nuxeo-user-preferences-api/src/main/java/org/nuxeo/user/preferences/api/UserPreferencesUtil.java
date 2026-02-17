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

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;

import org.nuxeo.ecm.platform.htmlsanitizer.HtmlSanitizerService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.user.preferences.exception.InvalidUserPreferencesKey;

/**
 * @since 2025.16
 */
public final class UserPreferencesUtil {

    /**
     * Default global maximum number of user preferences.
     */
    public static final String DEFAULT_PROP_MAX_PREFERENCES = "2000";

    /**
     * Regex used to validate a preference key pattern. Key can have only letters, digits, dashes, underscores and
     * periods characters. Its length must be at least 1 and maximum 100 characters.
     * <p>
     * A restrictive pattern avoids to sanitize keys.
     */
    public static final String PREFERENCE_KEY_REGEX = "^[a-zA-Z0-9-_.]{1,100}$";

    /**
     * Property defining the global maximum number of preferences per user.
     * <p>
     * Default value is {@link #DEFAULT_PROP_MAX_PREFERENCES}.
     */
    public static final String PROP_MAX_PREFERENCES = "nuxeo.user.preferences.max";

    /**
     * Property controlling the sanitizing of preference values. Sanitizing is enabled by default.
     */
    public static final String PROP_SANITIZE_VALUES = "nuxeo.user.preferences.sanitizeValues.enabled";

    private static final Pattern KEY_PATTERN = Pattern.compile(PREFERENCE_KEY_REGEX);

    private UserPreferencesUtil() {
    }

    /**
     * @return the global maximum number of user preferences
     */
    public static long maxPreferences() {
        var max = Long.parseLong(Framework.getProperty(PROP_MAX_PREFERENCES, DEFAULT_PROP_MAX_PREFERENCES));
        if (max < 1) {
            throw new IllegalArgumentException("%s must be greater than 0".formatted(PROP_MAX_PREFERENCES));
        }
        return max;
    }

    /**
     * @return a new {@link UserPreferences} for creation or update.
     */
    public static UserPreferences newUserDocPreferences(@Nonnull String key, @Nonnull String value) {
        return newUserDocPreferences(Map.of(key, value));
    }

    /**
     * @return a new {@link UserPreferences} for creation or update.
     */
    public static UserPreferences newUserDocPreferences(@Nonnull Map<String, String> preferences) {
        return newUserDocPreferences(
                preferences.entrySet()
                           .stream()
                           .map(preference -> newUserPreference(preference.getKey(), preference.getValue()))
                           .toList());
    }

    /**
     * @return a new {@link UserPreferences} for creation or update.
     */
    public static UserPreferences newUserDocPreferences(@Nonnull List<UserPreference> userPreferences) {
        return new DetachedUserPreferences(userPreferences);
    }

    /**
     * @return a new {@link UserPreference} for creation or update.
     */
    public static UserPreference newUserPreference(@Nonnull String key, @Nonnull String value) {
        return new DetachedUserPreference(key, value);
    }

    /**
     * Sanitizes the given value escaping html content.
     *
     * @param value the value
     * @return the sanitized value
     */
    public static String sanitizeValue(String value) {
        if (value == null || Framework.isBooleanPropertyFalse(PROP_SANITIZE_VALUES)) {
            return value;
        }
        return Framework.getService(HtmlSanitizerService.class).sanitizeString(value, null);
    }

    /**
     * Validates a preference key against the {@link #PREFERENCE_KEY_REGEX}.
     *
     * @param key the preference key
     * @throws InvalidUserPreferencesKey if the key is invalid
     */
    public static void validateKey(String key) {
        if (key == null) {
            throw new InvalidUserPreferencesKey(null);
        }
        if (!KEY_PATTERN.matcher(key).matches()) {
            throw new InvalidUserPreferencesKey(key);
        }
    }

    private record DetachedUserPreferences(@Nonnull List<UserPreference> userPreferences) implements UserPreferences {

        public DetachedUserPreferences {
            userPreferences = List.copyOf(userPreferences);
        }

        @Override
        public boolean isEmpty() {
            return userPreferences.isEmpty();
        }

        @Override
        @Nonnull
        public Map<String, String> preferences() {
            return userPreferences().stream().collect(Collectors.toMap(UserPreference::key, UserPreference::value));
        }

        @Override
        public int size() {
            return userPreferences.size();
        }
    }

    private record DetachedUserPreference(@Nonnull String key, @Nonnull String value) implements UserPreference {
    }

}
