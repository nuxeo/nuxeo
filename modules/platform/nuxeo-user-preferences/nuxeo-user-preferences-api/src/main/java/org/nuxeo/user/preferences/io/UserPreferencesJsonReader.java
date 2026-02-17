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
package org.nuxeo.user.preferences.io;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;
import static org.nuxeo.user.preferences.api.UserPreferencesUtil.newUserDocPreferences;
import static org.nuxeo.user.preferences.api.UserPreferencesUtil.newUserPreference;

import java.util.ArrayList;

import org.nuxeo.ecm.core.io.marshallers.json.EntityJsonReader;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.user.preferences.api.UserPreference;
import org.nuxeo.user.preferences.api.UserPreferences;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 2025.16
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class UserPreferencesJsonReader extends EntityJsonReader<UserPreferences> {

    public UserPreferencesJsonReader() {
        super(UserPreferencesJsonWriter.NAME);
    }

    @Override
    protected UserPreferences readEntity(JsonNode jn) {
        if (!jn.hasNonNull("preferences")) {
            throw new IllegalArgumentException("preferences is null");
        }
        var prefs = new ArrayList<UserPreference>();
        var prefsNode = jn.get("preferences");
        prefsNode.properties().forEach(properties -> {
            var propValue = properties.getValue();
            if (propValue.isNull()) {
                throw new IllegalArgumentException("value is null for key: %s".formatted(properties.getKey()));
            }
            prefs.add(newUserPreference(properties.getKey(), propValue.asText()));
        });
        return newUserDocPreferences(prefs);
    }
}
