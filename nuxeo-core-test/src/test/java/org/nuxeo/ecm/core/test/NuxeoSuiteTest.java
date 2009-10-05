package org.nuxeo.ecm.core.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;
import org.nuxeo.ecm.core.test.annotations.Repos;

@RunWith(MultiNuxeoCoreRunner.class)
@SuiteClasses({SimpleSession.class})
@Repos({RepoType.H2, RepoType.JCR, RepoType.POSTGRES})
public class NuxeoSuiteTest {
}
