package org.nuxeo.ecm.platform.annotations.repository.service;

import java.util.Properties;

import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.SimpleFeature;

@Features(PlatformFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.url.core", "org.nuxeo.ecm.relations.api",
        "org.nuxeo.ecm.relations", "org.nuxeo.ecm.relations.jena",
        "org.nuxeo.ecm.platform.types.api",
        "org.nuxeo.ecm.platform.types.core", "org.nuxeo.ecm.annotations",
        "org.nuxeo.ecm.annotations.contrib",
        "org.nuxeo.ecm.annotations.repository",
        "org.nuxeo.ecm.annotations.repository.test" })
@LocalDeploy({ "org.nuxeo.runtime.datasource:anno-ds.xml" })
public class AnnotationFeature extends SimpleFeature {

    @Override
    public void initialize(FeaturesRunner runner) {
        Framework.addListener(new RuntimeServiceListener() {

            @Override
            public void handleEvent(RuntimeServiceEvent event) {
                if (event.id == RuntimeServiceEvent.RUNTIME_ABOUT_TO_START) {
                    Framework.removeListener(this);
                }
                final Properties properties = Framework.getProperties();
                properties.setProperty("org.nuxeo.ecm.sql.jena.databaseType",
                        "HSQL");
                properties.setProperty(
                        "org.nuxeo.ecm.sql.jena.databaseTransactionEnabled",
                        "false");
            }
        });
    }
}
