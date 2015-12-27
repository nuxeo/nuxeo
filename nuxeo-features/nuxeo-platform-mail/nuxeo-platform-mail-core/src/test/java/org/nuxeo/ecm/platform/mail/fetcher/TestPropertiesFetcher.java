/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.mail.fetcher;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.platform.mail.service.MailService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Alexandre Russel
 */
public class TestPropertiesFetcher extends NXRuntimeTestCase {

    private static final String VALUE2 = "value2";

    private static final String VALUE1 = "value1";

    private static final String KEY2 = "key2";

    private static final String KEY1 = "key1";

    MailService propertiesFetcherService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.webapp.base");
        deployBundle("org.nuxeo.ecm.platform.mail");
        deployBundle("org.nuxeo.ecm.platform.mail.test");
        propertiesFetcherService = Framework.getService(MailService.class);
    }

    @Test
    public void testService() throws Exception {
        assertNotNull(propertiesFetcherService);
        PropertiesFetcher fetcher = propertiesFetcherService.getFetcher("testFactory");
        assertNotNull(fetcher);
        Map<String, String> configuration = new HashMap<String, String>();
        configuration.put(KEY1, VALUE1);
        configuration.put(KEY2, VALUE2);
        fetcher.configureFetcher(configuration);
        Properties properties = fetcher.getProperties(null);
        String value1 = properties.getProperty(KEY1);
        String value2 = properties.getProperty(KEY2);
        assertEquals(VALUE1, value1);
        assertEquals(VALUE2, value2);
    }

}
