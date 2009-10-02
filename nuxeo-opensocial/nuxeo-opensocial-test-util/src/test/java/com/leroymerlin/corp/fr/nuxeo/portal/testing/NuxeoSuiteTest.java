package com.leroymerlin.corp.fr.nuxeo.portal.testing;

import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

import com.leroymerlin.corp.fr.nuxeo.portal.testing.annotation.Repos;

@RunWith(MultiRepoNuxeoRunner.class)
@SuiteClasses({SimpleSession.class})
@Repos({RepoType.H2, RepoType.JCR, RepoType.POSTGRES})
public class NuxeoSuiteTest {
}
