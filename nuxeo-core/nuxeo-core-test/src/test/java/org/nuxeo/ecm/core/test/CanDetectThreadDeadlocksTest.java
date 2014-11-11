package org.nuxeo.ecm.core.test;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(DetectThreadDeadlocksFeature.class)
public class CanDetectThreadDeadlocksTest {

    @Ignore("does not stop")
    @Test
    public void runIntoDeadlocks()  {

        new ThreadDeadlocksRunner().run();

    }

}
