package org.nuxeo.ecm.platform.web.requestcontroller;

import org.nuxeo.ecm.platform.web.common.requestcontroller.service.RequestControllerManager;
import org.nuxeo.ecm.platform.web.common.requestcontroller.service.RequestControllerService;
import org.nuxeo.ecm.platform.web.common.requestcontroller.service.RequestFilterConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestService extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.web.common",
                "OSGI-INF/web-request-controller-framework.xml");
    }

    public void testServiceRegistration() {
        RequestControllerManager rcm = Framework
                .getLocalService(RequestControllerManager.class);
        assertNotNull(rcm);
    }

    public void testServiceContrib() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.web.common",
                "OSGI-INF/web-request-controller-contrib-test.xml");

        RequestControllerManager rcm = Framework
                .getLocalService(RequestControllerManager.class);
        assertNotNull(rcm);

        RequestControllerService rcmTest = (RequestControllerService) rcm;

        String uri = "";
        RequestFilterConfig config;

        uri = "/SyncNoTx/test";
        config = rcmTest.computeConfigForRequest(uri);
        assertTrue(config.needSynchronization());
        assertFalse(config.needTransaction());

        uri = "/NoSyncTx/test";
        config = rcmTest.computeConfigForRequest(uri);
        assertFalse(config.needSynchronization());
        assertTrue(config.needTransaction());

        uri = "/SyncTx/test";
        config = rcmTest.computeConfigForRequest(uri);
        assertTrue(config.needSynchronization());
        assertTrue(config.needTransaction());

        uri = "/SyncTx/deny";
        config = rcmTest.computeConfigForRequest(uri);
        assertFalse(config.needSynchronization());
        assertFalse(config.needTransaction());

        uri = "/whatever";
        config = rcmTest.computeConfigForRequest(uri);
        assertFalse(config.needSynchronization());
        assertFalse(config.needTransaction());

        uri = "/nuxeo/TestServlet/";
        config = rcmTest.computeConfigForRequest(uri);
        assertTrue(config.needSynchronization());
        assertTrue(config.needTransaction());

        uri = "/nuxeo/TestServlet";
        config = rcmTest.computeConfigForRequest(uri);
        assertTrue(config.needSynchronization());
        assertTrue(config.needTransaction());

        uri = "/nuxeo/TestServlet/toto";
        config = rcmTest.computeConfigForRequest(uri);
        assertTrue(config.needSynchronization());
        assertTrue(config.needTransaction());

    }

}
