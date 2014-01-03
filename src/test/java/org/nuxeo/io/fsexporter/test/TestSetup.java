package org.nuxeo.io.fsexporter.test;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.io.fsexporter.FSExporterService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy({ "nuxeo-fsexporter" })
public class TestSetup {

    @Test
    public void shouldDeclareService() {
        FSExporterService fes = Framework.getLocalService(FSExporterService.class);
        Assert.assertNotNull(fes);
    }

    // add here tests on contribution deployment
}
