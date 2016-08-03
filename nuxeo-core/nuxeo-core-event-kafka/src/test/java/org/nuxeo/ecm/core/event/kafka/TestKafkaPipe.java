/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     tiry
 */
package org.nuxeo.ecm.core.event.kafka;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.SimplePrincipal;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.UnboundEventContext;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * @since TODO
 */

@RunWith(FeaturesRunner.class)
@Features({ KafkaEventBusFeature.class })
@LocalDeploy({"org.nuxeo.ecm.core.event.kafka.test:test-KafkaPipes.xml", "org.nuxeo.ecm.core.event.kafka.test:test-async-listeners.xml"})
public class TestKafkaPipe {

    @Inject
    EventService eventService;

    @Test
    public void sendEventviaKafka() {

        UnboundEventContext ctx = new UnboundEventContext(new SimplePrincipal("titi"), null);

        eventService.fireEvent(ctx.newEvent("Test1"));
        eventService.fireEvent(ctx.newEvent("Test2"));
        eventService.waitForAsyncCompletion();

        Assert.assertEquals(2, DummyEventListener.events.size());
    }

}
