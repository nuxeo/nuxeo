/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

import org.nuxeo.ecm.platform.mail.service.MailService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Alexandre Russel
 *
 */
public class TestPropertiesFetcher extends NXRuntimeTestCase {

    private static final String VALUE2 = "value2";
    private static final String VALUE1 = "value1";
    private static final String KEY2 = "key2";
    private static final String KEY1 = "key1";
    MailService propertiesFetcherService;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.webapp.base");
        deployBundle("org.nuxeo.ecm.platform.mail");
        deployBundle("org.nuxeo.ecm.platform.mail.test");
        propertiesFetcherService = Framework.getService(MailService.class);
    }

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
