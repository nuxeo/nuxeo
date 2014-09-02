package org.nuxeo.ecm.core.cache;

import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;
import org.nuxeo.runtime.test.runner.ContributableFeaturesRunner;
import org.nuxeo.runtime.test.runner.Features;

@RunWith(ContributableFeaturesRunner. class)
@SuiteClasses(CacheComplianceFixture.class)
@Features(InMemoryCacheFeature.class)
public class TestCacheInMemory {

}
