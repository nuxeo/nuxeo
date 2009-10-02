package com.leroymerlin.corp.fr.nuxeo.portal.testing.guice;

import static org.junit.Assert.assertNotNull;

import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.TestRuntimeHarness;

public class UserManagerProvider implements Provider<UserManager> {

    private final TestRuntimeHarness harness;
    private final DirectoryService service;


    @Inject
    public UserManagerProvider(TestRuntimeHarness harness, DirectoryService service) throws Exception {

        this.harness = harness;
        this.service = service;
    }

    public UserManager get() {

        try {
            assertNotNull(service);

            // Deploy UserManager
            harness.deployContrib("org.nuxeo.ecm.platform.usermanager",
                    "OSGI-INF/UserService.xml");

            harness.deployContrib("org.nuxeo.test.util",
                    "test-usermanagerimpl/userservice-config.xml");

            return Framework.getService(UserManager.class);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
