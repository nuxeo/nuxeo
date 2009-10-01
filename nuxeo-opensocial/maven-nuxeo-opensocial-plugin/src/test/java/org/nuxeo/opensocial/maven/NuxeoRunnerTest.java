package org.nuxeo.opensocial.maven;

import java.io.File;

import org.nuxeo.maven.NuxeoRunnerMojo;
import org.nuxeo.opensocial.maven.NuxeoRunnerTestSupport.MavenProjectWithoutArtifacts;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;

public class NuxeoRunnerTest extends NuxeoRunnerTestSupport {

	public void testMojoCanStartsNuxeo() throws Exception {
		NuxeoRunnerMojo mojo = new NuxeoRunnerMojo();
		mojo.setProject(new MavenProjectWithoutArtifacts());
		mojo.execute();
		RuntimeService runtime = Framework.getRuntime();
		assertNotNull("Nuxeo Runtime should be defined", runtime);
		assertTrue("Nuxeo Runtime should be started", runtime.isStarted());
		assertLogContains("!/OSGI-INF/DefaultJBossBindings.xml");
	}

	public void testMojoCanStartsNuxeoWithAddedBundles() throws Exception {
		NuxeoRunnerMojo mojo = (NuxeoRunnerMojo) lookupMojo("run", new File(
				getBasedir(),
				"/target/test-classes/poms/mojotest/testRunnerMojo.xml"));
		mojo.setProject(new MavenProjectWithoutArtifacts());
		mojo.execute();
		RuntimeService runtime = Framework.getRuntime();
		assertNotNull("Nuxeo Runtime should be defined", runtime);
		assertTrue("Nuxeo Runtime should be started", runtime.isStarted());
		assertLogContains("org.nuxeo.opensocial.test.component");
	}

	public void testMojoCanStartsNuxeoWithAddedJars() throws Exception {
		NuxeoRunnerMojo mojo = (NuxeoRunnerMojo) lookupMojo(
				"run",
				new File(getBasedir(),
						"/target/test-classes/poms/mojotest/testRunnerMojoWithJars.xml"));
		mojo.setProject(new MavenProjectWithoutArtifacts());
		mojo.execute();
		RuntimeService runtime = Framework.getRuntime();
		assertNotNull("Nuxeo Runtime should be defined", runtime);
		assertTrue("Nuxeo Runtime should be started", runtime.isStarted());
		assertLogContains("test-bundle.jar");
	}

	public void testMojoCanDefinePropertiesInRuntime() throws Exception {
		NuxeoRunnerMojo mojo = (NuxeoRunnerMojo) lookupMojo(
				"run",
				new File(getBasedir(),
						"/target/test-classes/poms/mojotest/testRunnerMojoDefineProperties.xml"));
		mojo.setProject(new MavenProjectWithoutArtifacts());
		mojo.execute();
		RuntimeService runtime = Framework.getRuntime();
		assertEquals("property undefined", "some.value", runtime
				.getProperty("some.property"));
		assertEquals("property undefined", "other.value", runtime
				.getProperty("other.property"));
	}

}
