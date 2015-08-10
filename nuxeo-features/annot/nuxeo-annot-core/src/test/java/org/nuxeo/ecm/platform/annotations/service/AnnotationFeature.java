package org.nuxeo.ecm.platform.annotations.service;

import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.SimpleFeature;

@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.relations.api", //
        "org.nuxeo.ecm.relations", //
        "org.nuxeo.ecm.relations.jena", //
        "org.nuxeo.ecm.annotations", //
        "org.nuxeo.ecm.annotations.contrib", //
        "org.nuxeo.runtime.datasource", //
})
@LocalDeploy({ "org.nuxeo.ecm.annotations:test-ann-contrib.xml", //
        "org.nuxeo.ecm.annotations:datasource-config.xml" })
public class AnnotationFeature extends SimpleFeature {

    @Override
    public void initialize(FeaturesRunner runner) {
        Framework.addListener(new AnnotationsJenaSetup());
    }
}
