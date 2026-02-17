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

import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.ENTITY_FIELD_NAME;
import static org.nuxeo.user.preferences.api.UserPreferencesUtil.newUserPreference;

import jakarta.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.user.preferences.api.UserPreference;

/**
 * @since 2025.16
 */
@Deploy("org.nuxeo.platform.user.preferences.api")
@Features(CoreFeature.class)
public class TestUserPreferenceJsonWriter
        extends AbstractJsonWriterTest.Local<UserPreferenceJsonWriter, UserPreference> {

    @Inject
    protected CoreSession session;

    @Test
    public void test() throws Exception {
        var pref = newUserPreference("foo", "bar");
        JsonAssert json = jsonAssert(pref);
        json.properties(3);
        json.has(ENTITY_FIELD_NAME).isEquals("userPreference");
        json.has("key").isEquals("foo");
        json.has("value").isEquals("bar");
    }
}
