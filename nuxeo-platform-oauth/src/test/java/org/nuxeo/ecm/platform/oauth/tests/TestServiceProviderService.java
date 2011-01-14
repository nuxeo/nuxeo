package org.nuxeo.ecm.platform.oauth.tests;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.oauth.consumers.NuxeoOAuthConsumer;
import org.nuxeo.ecm.platform.oauth.consumers.OAuthConsumerRegistry;
import org.nuxeo.ecm.platform.oauth.providers.NuxeoOAuthServiceProvider;
import org.nuxeo.ecm.platform.oauth.providers.OAuthServiceProviderRegistry;
import org.nuxeo.ecm.platform.oauth.providers.OAuthServiceProviderRegistryImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestServiceProviderService extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.directory.api");
        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.directory.sql");
        deployBundle("org.nuxeo.ecm.platform.oauth");
        deployContrib("org.nuxeo.ecm.platform.oauth.test", "OSGI-INF/directory-test-config.xml");
    }


    public void testServiceLookup() throws Exception {
        OAuthServiceProviderRegistry providerRegistry = Framework.getLocalService(OAuthServiceProviderRegistry.class);
        assertNotNull(providerRegistry);
    }

    public void testServiceRW() throws Exception {

        OAuthServiceProviderRegistry providerRegistry = Framework.getLocalService(OAuthServiceProviderRegistry.class);
        assertNotNull(providerRegistry);

        NuxeoOAuthServiceProvider p = providerRegistry.addReadOnlyProvider("a",null, "b", null, null);
        assertNotNull(p);
        assertNotNull(p.getGadgetUrl());
        assertNotNull(p.getId());

        NuxeoOAuthServiceProvider p2 = providerRegistry.getProvider(p.getGadgetUrl(),p.getServiceName());
        assertEquals(p.getId(), p2.getId());

        DirectoryService ds = Framework.getService(DirectoryService.class);
        Session session = ds.open(OAuthServiceProviderRegistryImpl.DIRECTORY_NAME);
        Map<String, Object> init = new HashMap<String, Object>();

        init.put("gadgetUrl", "url");
        init.put("serviceName", "name");
        init.put("consumerKey", "key");
        init.put("consumerSecret", "secret");
        init.put("publicKey", "pk");

        DocumentModel entry = session.createEntry(init);
        session.updateEntry(entry);
        session.commit();
        session.close();

        NuxeoOAuthServiceProvider p3 = providerRegistry.getProvider("url",null);
        assertNull(p3);

        p3 = providerRegistry.getProvider(null,"name");
        assertNull(p3);

        p3 = providerRegistry.getProvider("url","name");
        assertNotNull(p3);


        assertEquals(2, providerRegistry.listProviders().size());


    }


}
