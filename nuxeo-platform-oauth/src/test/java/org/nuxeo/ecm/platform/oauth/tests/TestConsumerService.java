/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.oauth.tests;

import java.util.List;

import org.nuxeo.ecm.platform.oauth.consumers.NuxeoOAuthConsumer;
import org.nuxeo.ecm.platform.oauth.consumers.OAuthConsumerRegistry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestConsumerService extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.directory.api");
        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.directory.sql");
        deployBundle("org.nuxeo.ecm.platform.oauth");
        deployContrib("org.nuxeo.ecm.platform.oauth.test", "OSGI-INF/directory-test-config.xml");
    }


    public void testServiceLookup() throws Exception {
        OAuthConsumerRegistry consumerRegistry = Framework.getLocalService(OAuthConsumerRegistry.class);
        assertNotNull(consumerRegistry);
    }

    public void testServiceRW() throws Exception {

        OAuthConsumerRegistry consumerRegistry = Framework.getLocalService(OAuthConsumerRegistry.class);
        assertNotNull(consumerRegistry);


        NuxeoOAuthConsumer consumer = new NuxeoOAuthConsumer(null, "foo", "bar", null);

        consumerRegistry.storeConsumer(consumer);

        NuxeoOAuthConsumer foundConsumer = consumerRegistry.getConsumer("foo");
        assertNotNull(foundConsumer);

        assertEquals("foo",foundConsumer.consumerKey);
        assertEquals("bar",foundConsumer.consumerSecret);
        assertNull(foundConsumer.callbackURL);
        assertNull(foundConsumer.serviceProvider);

        NuxeoOAuthConsumer consumer2 = new NuxeoOAuthConsumer(null, "foo2", "bar2", null);
        consumerRegistry.storeConsumer(consumer2);

        List<NuxeoOAuthConsumer> consumers = consumerRegistry.listConsumers();
        assertEquals(2, consumers.size());

        consumerRegistry.deleteConsumer("foo");

        consumers = consumerRegistry.listConsumers();
        assertEquals(1, consumers.size());

        assertNull(consumerRegistry.getConsumer("foo"));
        assertNotNull(consumerRegistry.getConsumer("foo2"));

    }
}
