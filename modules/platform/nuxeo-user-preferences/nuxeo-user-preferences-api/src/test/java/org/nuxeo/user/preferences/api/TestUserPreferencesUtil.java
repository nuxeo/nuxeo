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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.nuxeo.user.preferences.api.UserPreferencesUtil.DEFAULT_PROP_MAX_PREFERENCES;
import static org.nuxeo.user.preferences.api.UserPreferencesUtil.PROP_MAX_PREFERENCES;
import static org.nuxeo.user.preferences.api.UserPreferencesUtil.PROP_SANITIZE_VALUES;
import static org.nuxeo.user.preferences.api.UserPreferencesUtil.maxPreferences;
import static org.nuxeo.user.preferences.api.UserPreferencesUtil.newUserDocPreferences;
import static org.nuxeo.user.preferences.api.UserPreferencesUtil.newUserPreference;
import static org.nuxeo.user.preferences.api.UserPreferencesUtil.sanitizeValue;
import static org.nuxeo.user.preferences.api.UserPreferencesUtil.validateKey;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;
import org.nuxeo.user.preferences.exception.InvalidUserPreferencesKey;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestUserPreferencesUtil {

    @Test
    public void testNewUserDocPreferences() {
        var pref = newUserPreference("key", "value");
        assertEquals("key", pref.key());
        assertEquals("value", pref.value());
        var prefs = newUserDocPreferences(List.of(pref));
        assertEquals(1, prefs.preferences().size());
        assertEquals("value", prefs.preferences().get("key"));
        prefs = newUserDocPreferences(List.of(pref, pref));
        assertThrows("Duplicated pref is illegal", IllegalStateException.class, prefs::preferences);
    }

    @Test
    public void testDefaultMaximumPreferencesProperty() {
        assertEquals(Long.parseLong(DEFAULT_PROP_MAX_PREFERENCES), maxPreferences());
    }

    @Test
    @WithFrameworkProperty(name = PROP_MAX_PREFERENCES, value = "-1")
    public void testInvalidMaximumPreferencesProperty() {
        assertThrows(IllegalArgumentException.class, UserPreferencesUtil::maxPreferences);
    }

    @Test
    @WithFrameworkProperty(name = PROP_MAX_PREFERENCES, value = "1")
    public void testValidMaximumPreferencesProperty() {
        assertEquals(1, maxPreferences());
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.htmlsanitizer")
    public void testSanitizeValues() {
        assertEquals("foobar", sanitizeValue("foo<script>alert('XSS')</script>bar"));
        assertNull(sanitizeValue(null));
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.htmlsanitizer")
    @WithFrameworkProperty(name = PROP_SANITIZE_VALUES, value = "false")
    public void testDoNotSanitizeValues() {
        assertEquals("foo<script>alert('XSS')</script>bar", sanitizeValue("foo<script>alert('XSS')</script>bar"));
        assertNull(sanitizeValue(null));
    }

    @Test
    public void testValidateKey() {
        validateKey("_foo");
        validateKey("nx-foo");
        validateKey("nx.foo");
        assertThrows(InvalidUserPreferencesKey.class, () -> validateKey("<script>alert('XSS')</script>"));
        assertThrows(InvalidUserPreferencesKey.class, () -> validateKey("foo@bar"));
    }

}
