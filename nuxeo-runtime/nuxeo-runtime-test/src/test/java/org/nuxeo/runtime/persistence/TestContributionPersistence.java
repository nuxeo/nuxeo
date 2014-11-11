/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.runtime.persistence;

import org.nuxeo.runtime.DummyContribution;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.persistence.Contribution;
import org.nuxeo.runtime.model.persistence.ContributionBuilder;
import org.nuxeo.runtime.model.persistence.ContributionPersistenceManager;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventService;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TestContributionPersistence extends NXRuntimeTestCase {

    protected ContributionPersistenceManager mgr;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.runtime.test.tests", "BaseXPoint.xml");

        fireFrameworkStarted();

        mgr = Framework.getLocalService(ContributionPersistenceManager.class);
    }

    public void test1() throws Exception {
        assertTrue(mgr.getContributions().isEmpty());
        // contribute something (an event listener)
        ContributionBuilder cb = new ContributionBuilder("contrib1");
        cb.setDescription("My first contribution");

        String xml = "<listener class=\"org.nuxeo.runtime.persistence.MyListener\"><topic>test</topic></listener>";
        cb.addXmlExtension("org.nuxeo.runtime.EventService", "listeners", xml);
        cb.addExtension("BaseXPoint", "xp", new DummyContribution("dummy1"),
                new DummyContribution("dummy2"));

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

        assertTrue(mgr.isPersisted(c1));
        assertTrue(mgr.isInstalled(c1));

        // fire an event.
        EventService es = Framework.getLocalService(EventService.class);
        assertEquals(0, MyListener.getCounter());
        es.sendEvent(new Event("test", "a test", null, null));
        assertEquals(1, MyListener.getCounter());
        es.sendEvent(new Event("test", "a test", null, null));
        assertEquals(2, MyListener.getCounter());

        mgr.uninstallContribution(c1);

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
