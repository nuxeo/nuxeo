package org.nuxeo.runtime.test.runner.contribs;

import org.junit.Assume;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.SimpleFeature;

public class BaseFeature extends SimpleFeature {

    public BaseFeature() {
        super();
    }

    protected boolean enabled = false;

    public void enable() {
        enabled = true;
    }

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        Assume.assumeTrue(enabled);
    }

}
