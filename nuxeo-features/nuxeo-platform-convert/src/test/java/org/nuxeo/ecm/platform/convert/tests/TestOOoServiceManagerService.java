package org.nuxeo.ecm.platform.convert.tests;

import org.nuxeo.ecm.platform.convert.oooserver.OOoDaemonService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestOOoServiceManagerService extends NXRuntimeTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.convert",
                "OSGI-INF/ooo-server-daemon-framework.xml");
    }


    public void testServiceRegistration() throws Exception {
        OOoDaemonService ods = Framework.getLocalService(OOoDaemonService.class);
        assertNotNull(ods);
    }

    public void testServiceChecks() throws Exception {
        OOoDaemonService ods = Framework.getLocalService(OOoDaemonService.class);
        assertTrue(ods.isEnabled());
    }

    public void testServiceRun() throws Exception {
        OOoDaemonService ods = Framework.getLocalService(OOoDaemonService.class);

        if (!ods.isConfigured()) {
            return;
        }

        boolean started = ods.startDaemonAndWaitUntilReady();
        assertTrue(started);

        int workers = ods.getNbWorkers();
        assertTrue(workers>0);

        Thread.currentThread().sleep(1000);

        boolean stoped = ods.stopDaemonAndWaitForCompletion();
        assertTrue(stoped);

        workers = ods.getNbWorkers();
        assertTrue(workers==0);

    }

}
