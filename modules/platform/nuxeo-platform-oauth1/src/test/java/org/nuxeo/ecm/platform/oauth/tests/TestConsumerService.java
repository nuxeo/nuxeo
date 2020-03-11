/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
@Features(OAuth1Feature.class)
public class TestConsumerService {

    @Inject
    OAuthConsumerRegistry consumerRegistry = Framework.getService(OAuthConsumerRegistry.class);

    @Test
    public void testServiceRW() throws Exception {

        NuxeoOAuthConsumer consumer = new NuxeoOAuthConsumer(null, "foo", "bar", null);

        consumerRegistry.storeConsumer(consumer);

        NuxeoOAuthConsumer foundConsumer = consumerRegistry.getConsumer("foo");
        assertNotNull(foundConsumer);

        assertEquals("foo", foundConsumer.consumerKey);
        assertEquals("bar", foundConsumer.consumerSecret);
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
