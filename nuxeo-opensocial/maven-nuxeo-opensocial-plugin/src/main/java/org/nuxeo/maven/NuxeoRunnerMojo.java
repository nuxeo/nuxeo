package org.nuxeo.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;

import com.leroymerlin.corp.fr.nuxeo.portal.testing.TestRuntimeHarness;

/**
 * Base mojo for starting/stopping nuxeo runtime server. This runner mojo starts
 * a nuxeo server and deploys a set of bundles in the newly created server.
 * 
 * @author nono
 * @goal run
 * @requiresDependencyResolution runtime
 */
public class NuxeoRunnerMojo extends AbstractMojo {

	/**
	 * The list of supplementary bundles to load into this nuxeo runner. Note
	 * the bundles' definition must be accessible from the classpath, which
	 * implies they should be defined as (runtime or test) dependencies.
	 * 
	 * @parameter expression="${nuxeo.bundles}"
	 */
	private ArrayList<String> bundles = new ArrayList<String>();

	/**
	 * List of supplementary jar files that are added to this runner resources
	 * for loading components and bundles. These jar files must exists and are
	 * added to the classpath. Note that they are not automatically deployed,
	 * their bundles should be added to the 'bundles' parameter for deploying.
	 * 
	 * @parameter expression="${nuxeo.jars}"
	 */
	private File[] jars = new File[0];

	/** @parameter expression="${project}" */
	private MavenProject project;

	/**
	 * Define additional properties to be loaded into the Nuxeo Runtime
	 * framework.
	 * 
	 * @parameter
	 */
	private TreeMap<String, String> properties = new TreeMap<String, String>();

	private TestRuntimeHarness harness;

	public void execute() throws MojoExecutionException, MojoFailureException {
		setHarness(new TestRuntimeHarness());
		try {
			for (File jar : jars)
				getHarness().addJar(jar);
			for (File dependency : getRuntimeDependencies())
				getHarness().addJar(dependency);
			getHarness().start();
			setAdditionalProperties();
			for (String bundle : bundles)
				getHarness().deployBundle(bundle);
		} catch (Exception e) {
			throw new MojoExecutionException(
					"Error while starting nuxeo Runtime", e);
		}
	}

	private void setAdditionalProperties() {
		OSGiRuntimeService runtime = (OSGiRuntimeService) Framework
				.getRuntime();
		for (Map.Entry<String, String> property : properties.entrySet())
			runtime.setProperty(property.getKey(), property.getValue());
	}

	@SuppressWarnings( { "unchecked" })
	private List<File> getRuntimeDependencies() {
		List<File> artifactFiles = new ArrayList<File>();
		for (Artifact artifact : (List<Artifact>) project.getRuntimeArtifacts())
			artifactFiles.add(new File(artifact.getFile().getPath()));
		return artifactFiles;
	}

	public void setProject(MavenProject project) {
		this.project = project;
	}

	public MavenProject getProject() {
		return project;
	}

	public void addBundle(String bundle) {
		bundles.add(bundle);
	}

	public void setHarness(TestRuntimeHarness harness) {
		this.harness = harness;
	}

	public TestRuntimeHarness getHarness() {
		return harness;
	}

}
