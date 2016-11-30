/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Estelle Giuly <egiuly@nuxeo.com>
 */

package org.nuxeo.ecm.platform.task.providers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.platform.task.providers.UserTaskPageProvider;

public class UserTaskPageProviderTest {

    protected Map<String, Serializable> properties = new HashMap<String, Serializable>();

    protected UserTaskPageProvider userTaskPageProviver = new UserTaskPageProvider();

    protected Locale defaultLocale = new Locale("en_US");

    @Test
    public void testGetLocaleWhenOnlyLanguage() {
        properties.put("locale", "it");
        userTaskPageProviver.setProperties(properties);
        assertEquals(new Locale("it"), userTaskPageProviver.getLocale());
    }

    @Test
    public void testGetLocaleWhenLanguageAndCountry() {
        properties.put("locale", "fr_FR");
        userTaskPageProviver.setProperties(properties);
        assertEquals(new Locale("fr", "FR"), userTaskPageProviver.getLocale());
    }

    @Test
    public void testGetLocaleWhenLanguageCountryAndVariant() {
        properties.put("locale", "th_TH_TH");
        userTaskPageProviver.setProperties(properties);
        assertEquals(new Locale("th", "TH", "TH"), userTaskPageProviver.getLocale());
    }

    @Test
    public void testGetLocaleWhenNull() {
        Locale.setDefault(defaultLocale);
        properties.put("locale", null);
        userTaskPageProviver.setProperties(properties);
        assertEquals(defaultLocale, userTaskPageProviver.getLocale());
    }

    @Test
    public void testGetLocaleWhenEmpty() {
        Locale.setDefault(defaultLocale);
        properties.put("locale", "");
        userTaskPageProviver.setProperties(properties);
        assertEquals(defaultLocale, userTaskPageProviver.getLocale());
    }

    @Test
    public void testGetLocaleWhenInvalidFormat() {
        properties.put("locale", "en-US");
        userTaskPageProviver.setProperties(properties);
        try {
            userTaskPageProviver.getLocale();
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }

        properties.put("locale", "FRE_FRA");
        userTaskPageProviver.setProperties(properties);
        try {
            userTaskPageProviver.getLocale();
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }
}
