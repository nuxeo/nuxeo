package org.nuxeo.opensocial.service.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.apache.shindig.social.opensocial.spi.ActivityService;
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
public class ActivityServiceTest {

    @Inject
    public ActivityServiceTest(TestRuntimeHarness harness, UserManager um)
            throws Exception {

        assertNotNull(Framework.getService(UserManager.class));
        harness.deployBundle("org.nuxeo.opensocial.service");
    }


    @Test
    public void iCanGetTheActivityService() throws Exception {
        assertNotNull(Framework.getService(ActivityService.class));
    }

    @Test
    public void testname() throws Exception {
        ActivityService nuxeoService = Framework
                .getService(ActivityService.class);
        Injector guice = Guice.createInjector(new NuxeoServiceModule());
        ActivityService guiceService = guice.getInstance(ActivityService.class);

        assertSame(nuxeoService, guiceService);

    }

}
