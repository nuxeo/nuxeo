package org.nuxeo.runtime.test.runner.contribs;

import javax.inject.Inject;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(BaseFeature.class)
public class BaseTest {

    @Inject FeaturesRunner runner;

    @Test
    public void something() {
        Assert.assertTrue(runner.getFeature(BaseFeature.class).enabled);
    }

}
