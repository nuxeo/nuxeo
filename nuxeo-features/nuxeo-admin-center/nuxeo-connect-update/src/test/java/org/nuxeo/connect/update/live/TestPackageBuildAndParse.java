package org.nuxeo.connect.update.live;

import java.io.IOException;

import javax.inject.Inject;

import org.junit.runner.RunWith;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy({ "org.nuxeo.connect.client", "org.nuxeo.connect.client.wrapper", "org.nuxeo.connect.update",
        "org.nuxeo.runtime.reload" })
public class TestPackageBuildAndParse extends org.nuxeo.connect.update.standalone.TestPackageBuildAndParse {

    @Inject
    PackageUpdateService injectedService;

    @Override
    protected void setupService() throws IOException, PackageException {
        service = injectedService;
    }

}
