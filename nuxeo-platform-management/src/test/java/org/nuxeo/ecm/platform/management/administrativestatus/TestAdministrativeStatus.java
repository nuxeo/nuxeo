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
package org.nuxeo.ecm.platform.management.administrativestatus;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.management.administrativestatus.service.AdministrativeStatusService;
import org.nuxeo.runtime.api.Framework;

public class TestAdministrativeStatus extends SQLRepositoryTestCase {

    AdministrativeStatusService statusService;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.management");
        deployBundle("org.nuxeo.ecm.platform.management.test");
        openSession();
    }

    public void testServerAdministrativeStatus() throws ClientException {
        assertEquals("unlocked",
                getAdministrativeStatusService().getServerStatus(session));
        getAdministrativeStatusService().lockServer(session);
        assertEquals("locked",
                getAdministrativeStatusService().getServerStatus(session));
        assertTrue(AdministrativeStatusListener.isServerLockedEventTriggered());
        getAdministrativeStatusService().unlockServer(session);
        assertEquals("unlocked",
                getAdministrativeStatusService().getServerStatus(session));
        assertTrue(AdministrativeStatusListener.isServerUnlockedEventTriggered());
    }

    private AdministrativeStatusService getAdministrativeStatusService()
            throws ClientException {
        if (statusService == null) {
            try {
                statusService = Framework.getService(AdministrativeStatusService.class);
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
        return statusService;
    }

}
