package org.nuxeo.opensocial.service.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.apache.shindig.social.opensocial.spi.AppDataService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.opensocial.services.NuxeoServiceModule;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.NuxeoRunner;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.TestRuntimeHarness;

@RunWith(NuxeoRunner.class)
public class AppDataServiceTest {

    @Inject
    public AppDataServiceTest(TestRuntimeHarness harness, UserManager um) throws Exception {
        assertNotNull(um);
        harness.deployBundle("org.nuxeo.opensocial.service");
    }

    @Test
    public void iCanGetTheAppDataService() throws Exception {
        assertNotNull(Framework.getService(AppDataService.class));
    }

    @Test
    public void injectedDataServiceIsTheSameAsNuxeoComponent() throws Exception {
        Injector guice = Guice.createInjector(new NuxeoServiceModule());
        assertSame(Framework.getService(AppDataService.class), guice
                .getInstance(AppDataService.class));
    }
}
