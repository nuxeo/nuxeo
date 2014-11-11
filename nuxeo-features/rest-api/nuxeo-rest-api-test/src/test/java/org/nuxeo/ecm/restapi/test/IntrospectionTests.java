package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;

import com.sun.jersey.api.client.ClientResponse;

@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@Jetty(port = 18090)
@RepositoryConfig(init = RestServerInit.class, cleanup = Granularity.METHOD)
public class IntrospectionTests extends BaseTest {

    @Test
    public void itCanFetchSchemas() throws Exception {
        ClientResponse response = getResponse(RequestType.GET,
                "/config/schemas");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        Assert.assertTrue(node.size() > 0);
        boolean dcFound = false;
        for (int i = 0; i < node.size(); i++) {
            if ("dublincore".equals(node.get(i).get("name").getValueAsText())) {
                dcFound = true;
                break;
            }
        }
        Assert.assertTrue(dcFound);
    }

    @Test
    public void itCanFetchASchema() throws Exception {
        ClientResponse response = getResponse(RequestType.GET,
                "/config/schemas/dublincore");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());

        Assert.assertEquals("dublincore", node.get("name").getValueAsText());
        Assert.assertEquals("dc", node.get("@prefix").getValueAsText());
        Assert.assertEquals("string",
                node.get("fields").get("creator").getValueAsText());
        Assert.assertEquals("string[]",
                node.get("fields").get("contributors").getValueAsText());
    }

    @Test
    public void itCanFetchFacets() throws Exception {
        ClientResponse response = getResponse(RequestType.GET, "/config/facets");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        Assert.assertTrue(node.size() > 0);

        boolean found = false;
        for (int i = 0; i < node.size(); i++) {
            if ("HasRelatedText".equals(node.get(i).get("name").getValueAsText())) {
                found = true;
                break;
            }
        }
        Assert.assertTrue(found);
    }

    @Test
    public void itCanFetchAFacet() throws Exception {
        ClientResponse response = getResponse(RequestType.GET,
                "/config/facets/HasRelatedText");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());

        Assert.assertEquals("HasRelatedText", node.get("name").getValueAsText());
        Assert.assertEquals("relatedtext",
                node.get("schemas").get(0).get("name").getValueAsText());
    }

    @Test
    public void itCanFetchTypes() throws Exception {
        ClientResponse response = getResponse(RequestType.GET, "/config/types");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());

        // the export is done as a compound object rather than an array !

        Assert.assertTrue(node.has("doctypes"));
        Assert.assertTrue(node.has("schemas"));

        Assert.assertTrue(node.get("doctypes").has("File"));
        Assert.assertTrue(node.get("schemas").has("dublincore"));
    }

    @Test
    public void itCanFetchAType() throws Exception {
        ClientResponse response = getResponse(RequestType.GET,
                "/config/types/File");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());

        // the export is done as a compound object rather than an array !

        Assert.assertEquals("Document", node.get("parent").getValueAsText());

        boolean dcFound = false;
        JsonNode schemas = node.get("schemas");
        for (int i = 0; i < schemas.size(); i++) {
            if ("dublincore".equals(schemas.get(i).get("name").getValueAsText())) {
                dcFound = true;
                break;
            }
        }
        Assert.assertTrue(dcFound);
    }

}
