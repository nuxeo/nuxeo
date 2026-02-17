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

import static jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND;

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * @since 2025.16
 */
public class UserPreferencesNotFound extends NuxeoException {

    public UserPreferencesNotFound(String key) {
        super("User preference: '%s' not found".formatted(key), SC_NOT_FOUND);
    }

    public UserPreferencesNotFound(DocumentRef docRef) {
        super("User document preferences for doc: '%s' not found".formatted(docRef), SC_NOT_FOUND);
    }
}
