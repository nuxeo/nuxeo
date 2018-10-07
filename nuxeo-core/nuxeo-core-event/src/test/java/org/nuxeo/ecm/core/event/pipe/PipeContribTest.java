/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     tiry
 */
package org.nuxeo.ecm.core.event.pipe;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.UnboundEventContext;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.core.event")
@Deploy("org.nuxeo.ecm.core.event.test:test-DummyPipes.xml")
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


        UnboundEventContext ctx = new UnboundEventContext(new UserPrincipal("titi", null, false, false), null);

        eventService.fireEvent(ctx.newEvent("Test1"));
        eventService.fireEvent(ctx.newEvent("Test2"));

        Assert.assertEquals(4, DummyPipe.receivedEvents.size());

    }

}
