package org.nuxeo.ecm.webdav;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import org.junit.Test;

import javax.ws.rs.core.Response;


public class JerseyClientTest extends AbstractServerTest {

    /**
     * Simple test using the Jersey HTTP client.
     * Only standard HTTP methods are supported, so we're only testing GET, PUT and DELETE.
     */
    @Test
    public void simpleTest() {
        Client client = Client.create();
        WebResource r = client.resource(ROOT_URI);

        String e1 = r.path("").get(String.class);
        assertTrue(e1.length() > 0);

        // Create / remove file

        r.path("file").put("some content");
        String e2 = r.path("file").get(String.class);
        assertEquals("some content", e2);

        r.path("file").put("different content");
        String e3 = r.path("file").get(String.class);
        assertEquals("different content", e3);

        r.path("file").delete();
        try {
            String e4 = r.path("file").get(String.class);
            fail("Should have raise a 'doc not found' exception");
        } catch (Exception e) {
            // OK.
        }
    }

    @Test
    public void multipleTest() {
        for (int i = 0; i < 100; i++) {
            simpleTest();
        }
    }

}