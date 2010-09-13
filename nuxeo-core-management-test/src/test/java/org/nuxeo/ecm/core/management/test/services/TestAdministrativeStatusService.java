package org.nuxeo.ecm.core.management.test.services;

import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.core.management.api.AdministrativeStatus;
import org.nuxeo.ecm.core.management.api.AdministrativeStatusManager;
import org.nuxeo.ecm.core.management.api.GlobalAdministrativeStatusManager;
import org.nuxeo.ecm.core.management.api.ProbeManager;
import org.nuxeo.runtime.api.Framework;

public class TestAdministrativeStatusService extends SQLRepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.runtime.management");
        deployBundle("org.nuxeo.ecm.core.management");
        deployBundle("org.nuxeo.ecm.core.management.test");
        super.fireFrameworkStarted();
        openSession();
    }


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

    public void testInstanceStatus() {

        AdministrativeStatusManager localManager = Framework.getLocalService(AdministrativeStatusManager.class);

        AdministrativeStatus status = localManager.getNuxeoInstanceStatus();
        assertTrue(status.isActive());

        status = localManager.deactivateNuxeoInstance("Nuxeo Server is down for maintenance", "system");
        assertTrue(status.isPassive());

        status = localManager.getNuxeoInstanceStatus();
        assertTrue(status.isPassive());

    }

    public void testMiscStatusWithDefaultValue() {

        final String serviceId = "org.nuxeo.ecm.administrator.message";
        AdministrativeStatusManager localManager = Framework.getLocalService(AdministrativeStatusManager.class);

        AdministrativeStatus status = localManager.getStatus(serviceId);
        assertTrue(status.isPassive());

        status = localManager.activate(serviceId, "Hi Nuxeo Users from Admin", "Administrator");
        assertTrue(status.isActive());

        status = localManager.deactivate(serviceId, "", "Administrator");
        assertTrue(status.isPassive());

    }


}
