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

package org.nuxeo.ecm.core.management.test.statuses;

import java.util.List;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.management.api.AdministrativeStatus;
import org.nuxeo.ecm.core.management.api.AdministrativeStatusManager;
import org.nuxeo.ecm.core.management.api.GlobalAdministrativeStatusManager;
import org.nuxeo.ecm.core.management.api.ProbeManager;
import org.nuxeo.ecm.core.management.statuses.AdministrableServiceDescriptor;
import org.nuxeo.ecm.core.management.storage.DocumentStoreSessionRunner;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;

public class TestAdministrativeStatusService extends SQLRepositoryTestCase {

    @Before
    public void setUp() throws Exception {
        AdministrativeStatusChangeListener.init();
        RuntimeListener.init();
        super.setUp();
        deployBundle("org.nuxeo.runtime.management");
        deployBundle("org.nuxeo.ecm.core.management");
        deployBundle("org.nuxeo.ecm.core.management.test");
        DocumentStoreSessionRunner.setRepositoryName(REPOSITORY_NAME);
        fireFrameworkStarted();
        openSession();
    }

    @After
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    @Test
    public void testServiceLookups() {
        // local manager lookup
        AdministrativeStatusManager localManager = Framework.getLocalService(AdministrativeStatusManager.class);
        assertNotNull(localManager);

        // global manager lookup
        GlobalAdministrativeStatusManager globalManager = Framework.getLocalService(GlobalAdministrativeStatusManager.class);
        assertNotNull(globalManager);

        // ensure that local manager is a singleton
        AdministrativeStatusManager localManager2 = globalManager.getStatusManager(globalManager.getLocalNuxeoInstanceIdentifier());
        assertEquals(localManager, localManager2);

        ProbeManager pm = Framework.getLocalService(ProbeManager.class);
        assertNotNull(pm);
    }

    @Test
    public void testInstanceStatus() {
        AdministrativeStatusManager localManager = Framework.getLocalService(AdministrativeStatusManager.class);

        AdministrativeStatus status = localManager.getNuxeoInstanceStatus();
        assertTrue(status.isActive());

        assertTrue(AdministrativeStatusChangeListener.isServerActivatedEventTriggered());
        assertFalse(AdministrativeStatusChangeListener.isServerPassivatedEventTriggered());

        assertTrue(RuntimeListener.isServerActivatedEventTriggered());
        assertFalse(RuntimeListener.isServerPassivatedEventTriggered());

        status = localManager.deactivateNuxeoInstance(
                "Nuxeo Server is down for maintenance", "system");
        assertTrue(status.isPassive());
        assertTrue(AdministrativeStatusChangeListener.isServerPassivatedEventTriggered());
        assertTrue(RuntimeListener.isServerPassivatedEventTriggered());

        status = localManager.getNuxeoInstanceStatus();
        assertTrue(status.isPassive());
    }

    @Test
    public void testMiscStatusWithDefaultValue() {
        final String serviceId = "org.nuxeo.ecm.administrator.message";
        AdministrativeStatusManager localManager = Framework.getLocalService(AdministrativeStatusManager.class);

        AdministrativeStatus status = localManager.getStatus(serviceId);
        assertTrue(status.isPassive());

        status = localManager.activate(serviceId, "Hi Nuxeo Users from Admin",
                "Administrator");
        assertTrue(status.isActive());

        status = localManager.deactivate(serviceId, "", "Administrator");
        assertTrue(status.isPassive());
    }

    @Test
    public void testNonExistingStatus() {
        AdministrativeStatusManager localManager = Framework.getLocalService(AdministrativeStatusManager.class);
        AdministrativeStatus nonExistingStatus = localManager.getStatus("org.nawak");
        assertNull(nonExistingStatus);
    }

    @Test
    public void testServiceListing() {
        AdministrativeStatusManager localManager = Framework.getLocalService(AdministrativeStatusManager.class);
        List<AdministrativeStatus> statuses = localManager.getAllStatuses();
        assertNotNull(statuses);
        assertEquals(3, statuses.size());
    }

    @Test
    public void testGlobalManager() {
        final String serviceId = "org.nuxeo.ecm.administrator.message";

        GlobalAdministrativeStatusManager globalManager = Framework.getLocalService(GlobalAdministrativeStatusManager.class);
        assertNotNull(globalManager);

        // check that we only have on Nuxeo instance for now
        List<String> instances = globalManager.listInstanceIds();
        assertNotNull(instances);
        assertEquals(1, instances.size());

        // check that we have 3 declared services
        List<AdministrableServiceDescriptor> descs = globalManager.listRegistredServices();
        assertNotNull(descs);
        assertEquals(3, descs.size());

        // for creation of a second instance
        AdministrativeStatusManager sm = globalManager.getStatusManager("MyClusterNode2");
        assertNotNull(sm);

        AdministrativeStatus status = sm.deactivateNuxeoInstance(
                "ClusterNode2 is deactivated for now", "system");
        assertNotNull(status);

        // check that we now have 2 instances
        instances = globalManager.listInstanceIds();
        assertEquals(2, instances.size());

        // update status on the same service on both nodes
        globalManager.setStatus(serviceId, AdministrativeStatus.ACTIVE,
                "Yo Man", "system");

        AdministrativeStatus statusNode1 = globalManager.getStatusManager(
                globalManager.getLocalNuxeoInstanceIdentifier()).getStatus(
                serviceId);
        assertNotNull(statusNode1);
        assertEquals("Yo Man", statusNode1.getMessage());

        AdministrativeStatus statusNode2 = sm.getStatus(serviceId);
        assertNotNull(statusNode2);
        assertEquals("Yo Man", statusNode2.getMessage());
    }

}
