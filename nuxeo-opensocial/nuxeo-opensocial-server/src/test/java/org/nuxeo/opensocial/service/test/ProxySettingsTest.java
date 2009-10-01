package org.nuxeo.opensocial.service.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.net.Proxy;

import org.apache.shindig.gadgets.DefaultGuiceModule;
import org.apache.shindig.gadgets.oauth.OAuthModule;
import org.apache.shindig.social.core.config.SocialApiGuiceModule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.opensocial.services.NuxeoServiceModule;
import org.nuxeo.opensocial.shindig.NuxeoPropertiesModule;
import org.nuxeo.opensocial.shindig.ProxyModule;
import org.nuxeo.opensocial.shindig.gadgets.NXGadgetSpecFactoryModule;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.NuxeoRunner;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.TestRuntimeHarness;

@RunWith(NuxeoRunner.class)
public class ProxySettingsTest {

    private OpenSocialService service;
    private Injector injector;

    @Inject
    public ProxySettingsTest(TestRuntimeHarness harness, UserManager ul)
            throws Exception {

        harness.deployBundle("org.nuxeo.opensocial.service");
        harness.loadProperties(OpenSocialServiceTest.class.getClassLoader()
                .getResourceAsStream("shindig.properties"));
        service = Framework.getService(OpenSocialService.class);
        assertNotNull(service);
        injector = Guice.createInjector(new NuxeoPropertiesModule(),
                new DefaultGuiceModule(), new NuxeoServiceModule(),
                new SocialApiGuiceModule(), new NXGadgetSpecFactoryModule(),
                new OAuthModule(), new ProxyModule());
        service.setInjector(injector);
        harness.deployContrib("org.nuxeo.opensocial.service.test",
                "OSGI-INF/opensocial-service-contrib.xml");

        OSGiRuntimeService service = (OSGiRuntimeService) Framework
                .getRuntime();

        service.setProperty("shindig.proxy.proxyPort", "8080");
        service.setProperty("shindig.proxy.proxyHost", "proxyHost");
        service.setProperty("shindig.proxy.proxySet", "true");
        service.setProperty("shindig.proxy.user", "user");
        service.setProperty("shindig.proxy.passworkd", "pass");


    }

    @Test
    public void onRecupereNotreGadgetSpecFactory() {
        Proxy NXproxy = service.getProxySettings();
        assertNotNull(NXproxy);
        assertEquals(Proxy.Type.HTTP, NXproxy.type());
        Proxy guiceProxy = injector.getInstance(Proxy.class);
        assertSame(NXproxy, guiceProxy);

    }

}
