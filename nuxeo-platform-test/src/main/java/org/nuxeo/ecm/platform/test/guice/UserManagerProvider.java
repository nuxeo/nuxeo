package org.nuxeo.ecm.platform.test.guice;

import static org.junit.Assert.assertNotNull;

import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class UserManagerProvider implements Provider<UserManager> {

    private final RuntimeHarness harness;
    private final DirectoryService service;


    @Inject
    public UserManagerProvider(RuntimeHarness harness, DirectoryService service) throws Exception {

        this.harness = harness;
        this.service = service;
    }

    public UserManager get() {

        try {
            assertNotNull(service);

            // Deploy UserManager
            harness.deployContrib("org.nuxeo.ecm.platform.usermanager",
                    "OSGI-INF/UserService.xml");

            harness.deployContrib("org.nuxeo.ecm.platform.test",
                    "test-usermanagerimpl/userservice-config.xml");

            return Framework.getService(UserManager.class);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
