package org.nuxeo.targetplatforms.jaxrs;

import static com.sun.jersey.api.client.Client.create;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.DetectThreadDeadlocksFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.webengine.test.WebEngineFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

@RunWith(FeaturesRunner.class)
@Features({ DetectThreadDeadlocksFeature.class, TransactionalFeature.class, WebEngineFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Jetty(port = 18090)
@Deploy({ "org.nuxeo.targetplatforms.core", "org.nuxeo.targetplatforms.core.test", "org.nuxeo.targetplatforms.jaxrs" })
@LocalDeploy({ "org.nuxeo.targetplatforms.core:OSGI-INF/test-datasource-contrib.xml",
        "org.nuxeo.targetplatforms.core:OSGI-INF/test-targetplatforms-contrib.xml" })
public class TargetPlatformServiceTest {

    private static final int TIMEOUT = 2000;

    private static final String URL = "http://localhost:18090/target-platforms";

    @Ignore("NXP-17108")
    @Test
    public void ping() throws IOException {
        WebResource resource = getServiceFor("Administrator", "Administrator");
        ClientResponse response = resource.path("/platforms").accept(APPLICATION_JSON).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String result = IOUtils.toString(response.getEntityInputStream());
        assertTrue(result.contains("nuxeo-dm-5.8"));
    }

    private WebResource getServiceFor(String user, String password) {
        ClientConfig config = new DefaultClientConfig();
        Client client = create(config);
        client.setConnectTimeout(TIMEOUT);
        client.setReadTimeout(TIMEOUT);
        client.addFilter(new HTTPBasicAuthFilter(user, password));
        return client.resource(URL);
    }

}
