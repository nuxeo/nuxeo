package com.leroymerlin.corp.fr.nuxeo.portal.testing;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;

import org.junit.Before;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;

public class RuntimeJettyFixture {

	protected LogCapture log;
	protected TestRuntimeHarness harness;
	protected URLChecker urlChecker;

	@Before
	public void setUp() throws Exception {
		log = new LogCapture().debug();
		harness = new TestRuntimeHarness();
		harness.start();
		((OSGiRuntimeService) Framework.getRuntime()).setProperty(
				"gadget.deployer.baseUrl", "http://localhost:11111/");
		harness.addConfigurationFromResource("/jetty.xml");
		harness.deployBundle("org.nuxeo.runtime.jetty");
		urlChecker = new URLChecker();
	}

	protected void assertUrlContentContains(URL url, String content)
			throws IOException {
		assertTrue("Unexpected content", urlChecker.getContentOf(url).contains(
				content));
	}

}
