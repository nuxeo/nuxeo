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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.mail.service.MailService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @author Alexandre Russel
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.platform.mail")
@Deploy("org.nuxeo.ecm.platform.mail.test")
public class TestPropertiesFetcher {

    private static final String VALUE2 = "value2";

    private static final String VALUE1 = "value1";

    private static final String KEY2 = "key2";

    private static final String KEY1 = "key1";

    @Inject
    public MailService propertiesFetcherService;

    @Test
    public void testService() throws Exception {
        assertNotNull(propertiesFetcherService);
        PropertiesFetcher fetcher = propertiesFetcherService.getFetcher("testFactory");
        assertNotNull(fetcher);
        Map<String, String> configuration = new HashMap<>();
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
