package org.nuxeo.runtime.test.runner.contribs;

import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.SimpleFeature;

public class ContributedFeature extends SimpleFeature  {

    @Override
    public void initialize(FeaturesRunner runner) {
        runner.getFeature(BaseFeature.class).enable();
    }
}
