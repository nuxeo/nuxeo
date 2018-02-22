/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.runtime.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.DummyContribution;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.persistence.Contribution;
import org.nuxeo.runtime.model.persistence.ContributionBuilder;
import org.nuxeo.runtime.model.persistence.ContributionPersistenceManager;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.runtime.test.tests:BaseXPoint.xml")
public class TestContributionPersistence {

    @Inject
    protected ContributionPersistenceManager mgr;

    @Test
    public void test1() throws Exception {
        assertTrue(mgr.getContributions().isEmpty());
        // contribute something (an event listener)
        ContributionBuilder cb = new ContributionBuilder("contrib1");
        cb.setDescription("My first contribution");

        String xml = "<listener class=\"org.nuxeo.runtime.persistence.MyListener\"><topic>test</topic></listener>";
        cb.addXmlExtension("org.nuxeo.runtime.EventService", "listeners", xml);
        cb.addExtension("BaseXPoint", "xp", new DummyContribution("dummy1"), new DummyContribution("dummy2"));

        Contribution c1 = mgr.addContribution(cb);
        // check the created contribution
        String content = c1.getContent();
        assertTrue(content.contains("</component>"));
        // System.out.println(content);
        assertEquals("My first contribution", c1.getDescription());
        assertFalse(c1.isDisabled());

        assertEquals(1, mgr.getContributions().size());
        assertTrue(mgr.isPersisted(c1));
        assertFalse(mgr.isInstalled(c1));

        mgr.installContribution(c1);
        Framework.getRuntime().getComponentManager().refresh();
        mgr = Framework.getService(ContributionPersistenceManager.class);

        assertTrue(mgr.isPersisted(c1));
        assertTrue(mgr.isInstalled(c1));

        // fire an event.
        EventService es = Framework.getService(EventService.class);
        assertEquals(0, MyListener.getCounter());
        es.sendEvent(new Event("test", "a test", null, null));
        assertEquals(1, MyListener.getCounter());
        es.sendEvent(new Event("test", "a test", null, null));
        assertEquals(2, MyListener.getCounter());

        mgr.uninstallContribution(c1);
        Framework.getRuntime().getComponentManager().refresh();
        mgr = Framework.getService(ContributionPersistenceManager.class);

        assertTrue(mgr.isPersisted(c1));
        assertFalse(mgr.isInstalled(c1));

        es.sendEvent(new Event("test", "a test", null, null));
        assertEquals(2, MyListener.getCounter());

        mgr.removeContribution(c1);
        assertEquals(0, mgr.getContributions().size());
        assertFalse(mgr.isPersisted(c1));
        assertFalse(mgr.isInstalled(c1));

    }
}
