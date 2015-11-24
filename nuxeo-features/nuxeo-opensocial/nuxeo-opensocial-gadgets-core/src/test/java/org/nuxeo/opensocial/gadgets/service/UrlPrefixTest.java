package org.nuxeo.opensocial.gadgets.service;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.launcher.config.Environment.NUXEO_LOOPBACK_URL;
import static org.nuxeo.launcher.config.Environment.OPENSOCIAL_GADGETS_EMBEDDED_SERVER;
import static org.nuxeo.launcher.config.Environment.OPENSOCIAL_GADGETS_HOST;
import static org.nuxeo.launcher.config.Environment.OPENSOCIAL_GADGETS_PORT;
import static org.nuxeo.launcher.config.Environment.OPENSOCIAL_GADGETS_PATH;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @author Stéphane Fourrier
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class UrlPrefixTest {

    @Test
    public void urlIsCorrectlyGeneratedWithAnEmbeddedServer() throws Exception {
        InternalGadgetDescriptor gadget = new InternalGadgetDescriptor();
        OSGiRuntimeService service = (OSGiRuntimeService) Framework.getRuntime();

        service.setProperty(NUXEO_LOOPBACK_URL, "http://localhost:8080/nuxeo");
        service.setProperty(OPENSOCIAL_GADGETS_EMBEDDED_SERVER, "true");
        service.setProperty(OPENSOCIAL_GADGETS_PATH, "/site/gadgets");

        assertEquals("http://localhost:8080/nuxeo/site/gadgets", gadget.getUrlPrefix(false).toString());
    }

    @Test
    public void urlIsCorrectlyGeneratedWithANonEmbeddedServer()
            throws Exception {
        InternalGadgetDescriptor gadget = new InternalGadgetDescriptor();
        OSGiRuntimeService service = (OSGiRuntimeService) Framework.getRuntime();

        service.setProperty(OPENSOCIAL_GADGETS_EMBEDDED_SERVER, "false");
        service.setProperty(OPENSOCIAL_GADGETS_HOST, "mywebsite");
        service.setProperty(OPENSOCIAL_GADGETS_PORT, "8081");
        service.setProperty(OPENSOCIAL_GADGETS_PATH, "/xxx/yyy");

        assertEquals("http://mywebsite:8081/xxx/yyy", gadget.getUrlPrefix(false).toString());

    }
}
