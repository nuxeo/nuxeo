package org.nuxeo.ecm.automation.server.test;

import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.test.RemoteAutomationServerFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(RemoteAutomationServerFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class RemoteAutomationClientTest extends AbstractAutomationClientTest {

}
