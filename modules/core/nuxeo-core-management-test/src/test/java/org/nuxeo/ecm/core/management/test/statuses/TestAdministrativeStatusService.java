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

package org.nuxeo.ecm.core.management.test.statuses;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.management.api.AdministrativeStatus;
import org.nuxeo.ecm.core.management.api.AdministrativeStatusManager;
import org.nuxeo.ecm.core.management.api.GlobalAdministrativeStatusManager;
import org.nuxeo.ecm.core.management.api.ProbeManager;
import org.nuxeo.ecm.core.management.statuses.AdministrableServiceDescriptor;
import org.nuxeo.ecm.core.management.storage.DocumentStoreSessionRunner;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.runtime.management")
@Deploy("org.nuxeo.ecm.core.management")
@Deploy("org.nuxeo.ecm.core.management.test")
public class TestAdministrativeStatusService {

    @Inject
    protected CoreFeature coreFeature;

    @Before
    public void setUp() throws Exception {
        DocumentStoreSessionRunner.setRepositoryName(coreFeature.getRepositoryName());
    }

    @After
    public void tearDown() {
        // clear for next test
        AdministrativeStatusChangeListener.init();
        RuntimeListener.init();
    }

    @Test
    public void testServiceLookups() {
        // local manager lookup
        AdministrativeStatusManager localManager = Framework.getService(AdministrativeStatusManager.class);
        assertNotNull(localManager);

        // global manager lookup
        GlobalAdministrativeStatusManager globalManager = Framework.getService(GlobalAdministrativeStatusManager.class);
        assertNotNull(globalManager);

        // ensure that local manager is a singleton
        AdministrativeStatusManager localManager2 = globalManager.getStatusManager(globalManager.getLocalNuxeoInstanceIdentifier());
        assertEquals(localManager, localManager2);

        ProbeManager pm = Framework.getService(ProbeManager.class);
        assertNotNull(pm);
    }

    @Test
    public void testInstanceStatus() {
        AdministrativeStatusManager localManager = Framework.getService(AdministrativeStatusManager.class);

        AdministrativeStatus status = localManager.getNuxeoInstanceStatus();
        assertTrue(status.isActive());

        assertTrue(AdministrativeStatusChangeListener.isServerActivatedEventTriggered());
        assertFalse(AdministrativeStatusChangeListener.isServerPassivatedEventTriggered());

        assertTrue(RuntimeListener.isServerActivatedEventTriggered());
        assertFalse(RuntimeListener.isServerPassivatedEventTriggered());

        status = localManager.deactivateNuxeoInstance("Nuxeo Server is down for maintenance",
                SecurityConstants.SYSTEM_USERNAME);
        assertTrue(status.isPassive());
        assertTrue(AdministrativeStatusChangeListener.isServerPassivatedEventTriggered());
        assertTrue(RuntimeListener.isServerPassivatedEventTriggered());

        status = localManager.getNuxeoInstanceStatus();
        assertTrue(status.isPassive());
    }

    @Test
    public void testMiscStatusWithDefaultValue() {
        final String serviceId = "org.nuxeo.ecm.administrator.message";
        AdministrativeStatusManager localManager = Framework.getService(AdministrativeStatusManager.class);

        AdministrativeStatus status = localManager.getStatus(serviceId);
        assertTrue(status.isPassive());

        status = localManager.activate(serviceId, "Hi Nuxeo Users from Admin", "Administrator");
        assertTrue(status.isActive());

        status = localManager.deactivate(serviceId, "", "Administrator");
        assertTrue(status.isPassive());
    }

    @Test
    public void testNonExistingStatus() {
        AdministrativeStatusManager localManager = Framework.getService(AdministrativeStatusManager.class);
        AdministrativeStatus nonExistingStatus = localManager.getStatus("org.nawak");
        assertNull(nonExistingStatus);
    }

    @Test
    public void testServiceListing() {
        AdministrativeStatusManager localManager = Framework.getService(AdministrativeStatusManager.class);
        List<AdministrativeStatus> statuses = localManager.getAllStatuses();
        assertNotNull(statuses);
        assertEquals(3, statuses.size());
    }

    @Test
    public void testGlobalManager() {
        final String serviceId = "org.nuxeo.ecm.administrator.message";

        GlobalAdministrativeStatusManager globalManager = Framework.getService(GlobalAdministrativeStatusManager.class);
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

        AdministrativeStatus status = sm.deactivateNuxeoInstance("ClusterNode2 is deactivated for now",
                SecurityConstants.SYSTEM_USERNAME);
        assertNotNull(status);

        // check that we now have 2 instances
        instances = globalManager.listInstanceIds();
        assertEquals(2, instances.size());

        // update status on the same service on both nodes
        globalManager.setStatus(serviceId, AdministrativeStatus.ACTIVE, "Yo Man", SecurityConstants.SYSTEM_USERNAME);

        AdministrativeStatus statusNode1 = globalManager.getStatusManager(
                globalManager.getLocalNuxeoInstanceIdentifier()).getStatus(serviceId);
        assertNotNull(statusNode1);
        assertEquals("Yo Man", statusNode1.getMessage());

        AdministrativeStatus statusNode2 = sm.getStatus(serviceId);
        assertNotNull(statusNode2);
        assertEquals("Yo Man", statusNode2.getMessage());
    }

}
