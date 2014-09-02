package org.nuxeo.runtime.test.runner.contribs;

import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;
import org.nuxeo.runtime.test.runner.ContributableFeaturesRunner;
import org.nuxeo.runtime.test.runner.Features;

@RunWith(ContributableFeaturesRunner.class)
@SuiteClasses(BaseTest.class)
@Features(ContributedFeature.class)
public class RunContributedBaseTest {

}
