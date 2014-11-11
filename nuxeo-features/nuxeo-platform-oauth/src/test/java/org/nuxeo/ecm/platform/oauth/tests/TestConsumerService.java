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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.oauth.consumers.NuxeoOAuthConsumer;
import org.nuxeo.ecm.platform.oauth.consumers.OAuthConsumerRegistry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(OAuthFeature.class)
public class TestConsumerService  {

    @Inject
    OAuthConsumerRegistry consumerRegistry = Framework.getLocalService(OAuthConsumerRegistry.class);

    @Test
    public void testServiceRW() throws Exception {


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
