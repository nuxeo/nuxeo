/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     mcedica
 */
package org.nuxeo.ecm.core.management.statuses;

import java.util.Collection;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;

public class TestStatuses extends SQLRepositoryTestCase {

    protected static boolean serverActivatedEventTriggered = false;

    protected static boolean serverPassivatedEventTriggered = false;

    public static class Listener implements EventListener {

        public void handleEvent(Event event) throws ClientException {
            String eventId = event.getName();
            if (eventId.equals(AdministrativeStatus.ACTIVATED_EVENT)) {
                serverActivatedEventTriggered = true;
            }
            if (eventId.equals(AdministrativeStatus.PASSIVATED_EVENT)) {
                serverPassivatedEventTriggered = true;
            }
        }


    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.runtime.management");
        deployBundle("org.nuxeo.ecm.core.management");
        deployBundle("org.nuxeo.ecm.core.management.test");
        super.fireFrameworkStarted();
        openSession();
    }

    public void testServerAdministrativeStatus() throws Exception {
        assertEquals("wrong default status, not active", AdministrativeStatus.ACTIVE,
                getAdministrativeStatus().getValue());
        getAdministrativeStatus().setPassive();
        assertEquals("cannot passivate", AdministrativeStatus.PASSIVE,
                getAdministrativeStatus().getValue());
        assertTrue("passivate event not received", serverPassivatedEventTriggered);
        getAdministrativeStatus().setActive();
        assertEquals("cannot activate", AdministrativeStatus.ACTIVE,
                getAdministrativeStatus().getValue());
        assertTrue("activate event not received", serverActivatedEventTriggered);
    }

    public void testProbe() {
        ProbeRunner runner = Framework.getLocalService(ProbeRunner.class);
        ProbeInfo info = runner.getProbeInfo(AdministrativeStatusProbe.class);
        assertNotNull("admin status probe not registred", info);
        Collection<String> names = runner.getProbeNames();
        assertTrue("admin status shortcut not listed", names.contains("administrativeStatus"));
        assertNotNull("admin status probe not published", info.qualifiedName);
        assertEquals(0, info.runnedCount);
        assertTrue(info.lastStatus.info.equals("not yet runned"));
        assertTrue(info.lastFailureStatus.info.equals("not yet failed"));
        assertTrue(info.lastSuccesStatus.info.equals("not yet succeed"));
        runner.run();
        assertEquals(1, info.runnedCount);
    }

    private AdministrativeStatus getAdministrativeStatus()
            throws Exception {
        return Framework.getService(AdministrativeStatus.class);
    }

}
