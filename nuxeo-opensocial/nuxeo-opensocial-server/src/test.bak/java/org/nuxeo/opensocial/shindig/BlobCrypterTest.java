package org.nuxeo.opensocial.shindig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;

import org.apache.shindig.auth.BlobCrypterSecurityToken;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.auth.SecurityTokenDecoder;
import org.apache.shindig.common.ContainerConfig;
import org.apache.shindig.common.ContainerConfigException;
import org.apache.shindig.common.JsonContainerConfig;
import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.util.CharsetUtil;
import org.apache.shindig.common.util.FakeTimeSource;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.opensocial.services.NuxeoCryptoModule;
import org.nuxeo.opensocial.shindig.crypto.NXSecurityTokenDecoder;
import org.nuxeo.runtime.api.Framework;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.NuxeoRunner;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.TestRuntimeHarness;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.UserManagerHarness;

@RunWith(NuxeoRunner.class)
public class BlobCrypterTest {

    private Injector injector;

    private final FakeTimeSource timeSource = new FakeTimeSource();

    private static TestRuntimeHarness harness;

    @Inject
    public BlobCrypterTest(TestRuntimeHarness harness, UserManager um) throws Exception {
        assertNotNull(um);
        harness.deployBundle("org.nuxeo.opensocial.service");
    }

    private static class FakeContainerConfig extends JsonContainerConfig {
        private final String tokenType;

        public FakeContainerConfig(String tokenType)
                throws ContainerConfigException {
            super(null);
            this.tokenType = tokenType;
        }

        @Override
        public String get(String container, String parameter) {
            if ("gadgets.securityTokenType".equals(parameter)) {
                if ("default".equals(container)) {
                    return tokenType;
                }
            } else if ("gadgets.securityTokenKeyFile".equals(parameter)) {
                return "container key file: " + container;
            } else if ("gadgets.signedFetchDomain".equals(parameter)){
                return "container.com";

            }
            return null;
        }

        @Override
        public Collection<String> getContainers() {
            return Lists.newArrayList("default");
        }
    }

    @Before
    public void setUp() {
        injector = Guice.createInjector(new NuxeoCryptoModule(),
                new AbstractModule() {

                    @Override
                    protected void configure() {

                        try {
                            bind(ContainerConfig.class).toInstance(
                                    new FakeContainerConfig("secure"));
                        } catch (ContainerConfigException e) {
                            e.printStackTrace();
                        }
                    }

                });

    }

    @Test
    public void iCanGetTheKeyForAContainer() throws Exception {

        OpenSocialService os = Framework.getService(OpenSocialService.class);
        assertNotNull(os.getKeyForContainer("default"));
    }

    @Test
    public void mySecurityTokenDecoderIsANuxeoOne() throws Exception {
        SecurityTokenDecoder instance = getSecurityTokenDecoder();
        assertEquals(NXSecurityTokenDecoder.class, instance.getClass());
    }


    @Test
    public void testCreateToken() throws Exception {
        OpenSocialService os = Framework.getService(OpenSocialService.class);

      BlobCrypterSecurityToken t = new BlobCrypterSecurityToken(
          getBlobCrypter(os.getKeyForContainer("default")), "default", "container.com");
      t.setAppUrl("http://www.example.com/gadget.xml");
      t.setModuleId(12345L);
      t.setOwnerId("owner");
      t.setViewerId("viewer");
      t.setTrustedJson("trusted");
      String encrypted = t.encrypt();

      SecurityToken t2 = getSecurityTokenDecoder().createToken(
          ImmutableMap.of(SecurityTokenDecoder.SECURITY_TOKEN_NAME, encrypted));

      assertEquals("http://www.example.com/gadget.xml", t2.getAppId());
      assertEquals("http://www.example.com/gadget.xml", t2.getAppUrl());
      assertEquals("container.com", t2.getDomain());
      assertEquals(12345L, t2.getModuleId());
      assertEquals("owner", t2.getOwnerId());
      assertEquals("viewer", t2.getViewerId());
      assertEquals("trusted", t2.getTrustedJson());
    }


    private BlobCrypter getBlobCrypter(String fileName) {
        BasicBlobCrypter c = new BasicBlobCrypter(CharsetUtil.getUtf8Bytes(fileName));
        c.timeSource = timeSource;
        return c;
      }


    private SecurityTokenDecoder getSecurityTokenDecoder() {
        SecurityTokenDecoder instance = injector
                .getInstance(SecurityTokenDecoder.class);
        return instance;
    }

}
