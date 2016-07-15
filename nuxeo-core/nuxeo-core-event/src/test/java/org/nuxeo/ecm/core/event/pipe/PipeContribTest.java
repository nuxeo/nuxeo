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
package org.nuxeo.ecm.core.event.pipe;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.SimplePrincipal;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.UnboundEventContext;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy({ "org.nuxeo.ecm.core.event" })
@LocalDeploy("org.nuxeo.ecm.core.event.test:test-DummyPipes.xml")
public class PipeContribTest {

    @Inject
    EventService eventService;

    @Test
    public void testDummyDispatcher() {

        // check that pipes were contributed
        Assert.assertNotNull(DummyDispatcher.pipeDescriptors);
        // check that the 2 contrib on the same pipe were merged
        Assert.assertEquals(2, DummyDispatcher.pipeDescriptors.size());

        // check first pipe
        EventPipeDescriptor desc1 = DummyDispatcher.pipeDescriptors.get(0);
        Assert.assertEquals("dummyPipe1", desc1.getName());

        // check second pipe
        EventPipeDescriptor desc2 = DummyDispatcher.pipeDescriptors.get(1);
        Assert.assertEquals("dummyPipe2", desc2.getName());

        // check that params were merged
        Assert.assertEquals(2, desc2.getParameters().size());

        // check that priority was overridden
        Assert.assertEquals(new Integer(10), desc2.getPriority());


        UnboundEventContext ctx = new UnboundEventContext(new SimplePrincipal("titi"), null);

        eventService.fireEvent(ctx.newEvent("Test1"));
        eventService.fireEvent(ctx.newEvent("Test2"));

        Assert.assertEquals(4, DummyPipe.receivedEvents.size());

    }

}
