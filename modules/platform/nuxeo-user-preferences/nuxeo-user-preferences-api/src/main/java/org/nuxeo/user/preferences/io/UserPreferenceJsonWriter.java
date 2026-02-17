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

import java.io.IOException;

import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.user.preferences.api.UserPreference;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 2025.16
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class UserPreferenceJsonWriter extends ExtensibleEntityJsonWriter<UserPreference> {

    public static final String NAME = "userPreference";

    public UserPreferenceJsonWriter() {
        super(NAME);
    }

    @Override
    protected void writeEntityBody(UserPreference entity, JsonGenerator jg) throws IOException {
        jg.writeStringField("key", entity.key());
        jg.writeStringField("value", entity.value());
    }
}
