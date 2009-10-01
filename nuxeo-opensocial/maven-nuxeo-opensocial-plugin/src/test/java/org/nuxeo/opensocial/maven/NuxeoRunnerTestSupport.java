package org.nuxeo.opensocial.maven;

import java.util.Collections;
import java.util.List;

import junit.framework.AssertionFailedError;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

import com.leroymerlin.corp.fr.nuxeo.portal.testing.LogCapture;

public abstract class NuxeoRunnerTestSupport extends AbstractMojoTestCase {

	public class MavenProjectWithoutArtifacts extends MavenProjectStub {

		@SuppressWarnings( { "unchecked" })
		@Override
		public List getRuntimeArtifacts() {
			return Collections.EMPTY_LIST;
		}

	}

	private LogCapture logCapture;

	public NuxeoRunnerTestSupport() {
		super();
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		logCapture = new LogCapture().debug();
	}

	protected void assertLogContains(String string) {
		String logContent = logCapture.content();
		if (!logContent.contains(string))
			throw new AssertionFailedError("Log content does not contains '"
					+ string + "' (size=" + logContent.length() + ")");
	}

	protected String logContent() {
		return logCapture.content();
	}

}