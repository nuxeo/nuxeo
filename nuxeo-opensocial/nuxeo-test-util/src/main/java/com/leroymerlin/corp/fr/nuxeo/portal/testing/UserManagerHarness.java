package com.leroymerlin.corp.fr.nuxeo.portal.testing;

import static org.junit.Assert.assertNotNull;

import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

public class UserManagerHarness {
	private TestRuntimeHarness harness;

	public UserManagerHarness(TestRuntimeHarness rtHarness) {
		this.harness = rtHarness;
	}

	public void start() throws Exception {
		harness.deployBundle("org.nuxeo.ecm.platform.api");

		//Deploy Directory Service
		harness.deployContrib("org.nuxeo.ecm.directory",
				"OSGI-INF/DirectoryService.xml");
		harness.deployContrib("org.nuxeo.ecm.directory.sql",
				"OSGI-INF/SQLDirectoryFactory.xml");

		//Deploy Shema Contrib
		harness.deployContrib("org.nuxeo.test.util",
				"test-usermanagerimpl/schemas-config.xml");
		harness.deployContrib("org.nuxeo.test.util",
				"test-usermanagerimpl/directory-config.xml");

		
		//Deploy UserManager
		harness.deployContrib("org.nuxeo.ecm.platform.usermanager",
				"OSGI-INF/UserService.xml");

		harness.deployContrib("org.nuxeo.test.util",
				"test-usermanagerimpl/userservice-config.xml");

		assertNotNull(Framework.getService(UserManager.class));

	}

}
