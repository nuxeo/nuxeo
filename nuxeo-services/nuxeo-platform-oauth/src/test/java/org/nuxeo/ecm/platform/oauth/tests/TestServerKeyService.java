package org.nuxeo.ecm.platform.oauth.tests;

import org.nuxeo.ecm.platform.oauth.keys.OAuthServerKeyManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestServerKeyService extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.directory.api");
        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.directory.sql");
        deployBundle("org.nuxeo.ecm.platform.oauth");
    }

    public void testServiceLookup() throws Exception {
        OAuthServerKeyManager skm = Framework.getLocalService(OAuthServerKeyManager.class);
        assertNotNull( skm);
        String pc = skm.getPublicKeyCertificate();
        assertNotNull(pc);
    }

}
