package org.nuxeo.ecm.core.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(DetectThreadDeadlocksFeature.class)
public class CanDetectThreadDeadlocksTest {

    protected Log log = LogFactory.getLog(CanDetectThreadDeadlocksTest.class);

    @Test
    public void runIntoDeadlocks()  {

        new ThreadDeadlocksRunner().run();

    }

}
