package org.nuxeo.ecm.platform.convert.tests;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.convert.ooolauncher.OOoConfigHelper;
import org.nuxeo.ecm.platform.convert.ooolauncher.OOoConnectionManager;
import org.nuxeo.ecm.platform.convert.ooolauncher.OOoLauncherComponent;
import org.nuxeo.ecm.platform.convert.ooolauncher.OOoLauncherDescriptor;
import org.nuxeo.ecm.platform.convert.ooolauncher.OOoLauncherService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

import com.artofsolving.jodconverter.openoffice.connection.SocketOpenOfficeConnection;

public class TestOOoLauncher extends NXRuntimeTestCase {

    protected static Log log = LogFactory.getLog(TestOOoLauncher.class);

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.convert",
                "OSGI-INF/ooo-launcher-framework.xml");

    }

    public void testServiceRegistration() throws Exception {
        OOoLauncherService ols = Framework.getLocalService(OOoLauncherService.class);
        assertNotNull(ols);

        OOoConnectionManager ocm = Framework.getLocalService(OOoConnectionManager.class);
        assertNotNull(ocm);

    }

    public void testLauncher() throws Exception {
        OOoLauncherService ols = Framework.getLocalService(OOoLauncherService.class);
        assertNotNull(ols);

        if (!ols.isConfigured()) {
            log.info("OOo not detected, skipping tests");
            return;
        }

        log.info("OOo detected, testing");


        assertFalse(ols.isOOoLaunched());

        log.debug("Starting");
        boolean started = ols.startOOo();
        assertTrue(started);

        assertTrue(ols.isOOoLaunched());

        log.debug("Wait till server is ready");
        assertTrue(ols.waitTillReady());

        log.debug("Should be ready, checking");
        assertTrue(ols.isOOoListening());

        log.debug("Server is accepting connections : ready");

        log.debug("Stopping Ooo process");
        ols.stopOooAndWait(60);

        log.debug("Stoped ? checking");

        assertFalse(ols.isOOoLaunched());
        assertFalse(ols.isOOoListening());

        log.debug("Server stoped ok");

        log.debug("Re start and wait till ready");
        started = ols.startOOoAndWaitTillReady();
        assertTrue(started);
        log.debug("started OK");
        assertTrue(ols.isOOoListening());
        log.debug("server is listening, shuting down");

        ols.stopOooAndWait(60);
        assertFalse(ols.isOOoLaunched());
        assertFalse(ols.isOOoListening());
        log.debug("server stoped");

    }

    public void testConnectionManager() throws Exception {

        OOoLauncherService ols = Framework.getLocalService(OOoLauncherService.class);
        assertNotNull(ols);

        if (!ols.isConfigured()) {
            log.info("OOo not detected, skipping tests");
            return;
        }

        OOoConnectionManager ocm = Framework.getLocalService(OOoConnectionManager.class);
        assertNotNull(ocm);

        SocketOpenOfficeConnection conn = ocm.getConnection();
        assertNotNull(conn);
        assertTrue(conn.isConnected());

    }


    public void testContribs() throws Exception {

        deployContrib("org.nuxeo.ecm.platform.convert.test", "ooo-launcher-test-contrib.xml");
        OOoLauncherService ols = Framework.getLocalService(OOoLauncherService.class);
        assertNotNull(ols);

        OOoLauncherComponent component = (OOoLauncherComponent) ols;
        assertNotNull(component);

        OOoLauncherDescriptor contrib = component.getDescriptor();

        assertEquals("192.168.1.5", contrib.getOooListenerIP());
        assertEquals(8200, contrib.getOooListenerPort());
        assertEquals(61, contrib.getOooStartupTimeOut());
        assertEquals("/opt/openoffice/program", contrib.getOooInstallationPath());
        assertTrue(contrib.getStartOOoAtServiceStartup());

    }
}
