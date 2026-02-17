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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.core.io.CoreIOFeature;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonReaderTest;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.user.preferences.api.UserPreferences;

/**
 * @since 2025.16
 */
@Deploy("org.nuxeo.platform.user.preferences.api:OSGI-INF/user-prefs-io-contrib.xml")
@Features(CoreIOFeature.class)
public class TestUserPreferencesJsonReader
        extends AbstractJsonReaderTest.Local<UserPreferencesJsonReader, UserPreferences> {

    @Test
    public void testDefault() throws Exception {
        var prefs = asObject("""
                {
                  "entity-type": "userPreferences",
                  "preferences" : {
                    "key1": "value1",
                    "key2": "value2"
                  }
                }""");
        assertEquals(Map.of("key1", "value1", "key2", "value2"), prefs.preferences());
    }

    @Test
    public void testNull() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> asObject("""
                {
                  "entity-type": "userPreferences"
                }"""));
        assertThrows(IllegalArgumentException.class, () -> asObject("""
                {
                  "entity-type": "userPreferences",
                  "preferences" : {
                    "key1": null
                  }
                }"""));
        asObject("""
                {
                  "entity-type": "userPreferences",
                  "preferences" : {
                    "foo": "bar"
                  }
                }""");
    }
}
