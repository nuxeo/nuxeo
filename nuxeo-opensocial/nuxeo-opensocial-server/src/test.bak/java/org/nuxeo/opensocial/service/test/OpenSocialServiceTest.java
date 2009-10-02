package org.nuxeo.opensocial.service.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.shindig.gadgets.DefaultGuiceModule;
import org.apache.shindig.gadgets.oauth.OAuthModule;
import org.apache.shindig.social.core.config.SocialApiGuiceModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.opensocial.services.NuxeoServiceModule;
import org.nuxeo.opensocial.shindig.NuxeoPropertiesModule;
import org.nuxeo.opensocial.shindig.gadgets.NXGadgetSpecFactoryModule;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.NuxeoRunner;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.TestRuntimeHarness;

@RunWith(NuxeoRunner.class)
public class OpenSocialServiceTest {

    private OpenSocialService service;

    @Inject
    public OpenSocialServiceTest(TestRuntimeHarness harness, UserManager ul) throws Exception {

        harness.deployBundle("org.nuxeo.opensocial.service");
        harness.loadProperties(OpenSocialServiceTest.class.getClassLoader()
                .getResourceAsStream("shindig.properties"));
        service = Framework.getService(OpenSocialService.class);
        assertNotNull(service);
        service.setInjector(Guice.createInjector(new NuxeoPropertiesModule(),
                new DefaultGuiceModule(), new NuxeoServiceModule(),
                new SocialApiGuiceModule(), new NXGadgetSpecFactoryModule(),
                new OAuthModule()));
        harness.deployContrib("org.nuxeo.opensocial.service.test",
                "OSGI-INF/opensocial-service-contrib.xml");

    }


    @Test
    public void onRecupereNotreGadgetSpecFactory() {
        assertNotNull(service.getGadgetSpecFactory());
        assertEquals(
                "org.nuxeo.opensocial.shindig.gadgets.NXGadgetSpecFactory",
                service.getGadgetSpecFactory().getClass().getName());
    }



}
