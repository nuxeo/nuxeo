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
package org.nuxeo.user.preferences.exception;

import static jakarta.servlet.http.HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE;
import static org.nuxeo.user.preferences.api.UserPreferencesUtil.PROP_MAX_PREFERENCES;

import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * @since 2025.16
 */
public class TooManyUserPreferencesException extends NuxeoException {

    public TooManyUserPreferencesException(String userId, long actual, long limit) {
        super("Too many preferences for User: '%s', size: %s, limit is %s set by '%s'".formatted(userId, actual, limit,
                PROP_MAX_PREFERENCES), SC_REQUEST_ENTITY_TOO_LARGE);
    }

}
