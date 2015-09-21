/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import com.google.inject.Inject;
import com.sun.jersey.api.client.WebResource;
import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @since 7.3
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@Jetty(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
@Deploy({ "org.nuxeo.ecm.core.cache", "org.nuxeo.ecm.platform.convert" })
public class ConverterTest extends BaseTest {

    @Inject
    protected EventService eventService;

    @Test
    public void shouldConvertBlobUsingNamedConverter() {
        DocumentModel doc = createDummyDocument();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("converter", "any2pdf");
        ClientResponse response = getResponse(RequestType.GET, "path" + doc.getPathAsString()
                + "/@blob/file:content/@convert", queryParams);
        assertEquals(200, response.getStatus());
    }

    protected DocumentModel createDummyDocument() {
        DocumentModel doc = session.createDocumentModel("/", "adoc", "File");
        Blob blob = Blobs.createBlob("Dummy txt", "text/plain", null, "dummy.txt");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        return doc;
    }

    @Test
    public void shouldConvertBlobUsingMimeType() {
        DocumentModel doc = createDummyDocument();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("type", "application/pdf");
        ClientResponse response = getResponse(RequestType.GET, "path" + doc.getPathAsString()
                + "/@blob/file:content/@convert", queryParams);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void shouldConvertBlobUsingFormat() {
        DocumentModel doc = createDummyDocument();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("format", "pdf");
        ClientResponse response = getResponse(RequestType.GET, "path" + doc.getPathAsString()
                + "/@blob/file:content/@convert", queryParams);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void shouldConvertDocument() {
        DocumentModel doc = createDummyDocument();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("converter", "any2pdf");
        ClientResponse response = getResponse(RequestType.GET, "path" + doc.getPathAsString() + "/@convert",
                queryParams);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void shouldScheduleAnAsyncConversion() throws IOException {
        DocumentModel doc = createDummyDocument();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("converter", "any2pdf");
        queryParams.putSingle("async", "true");
        ClientResponse response = getResponse(RequestType.POST, "path" + doc.getPathAsString() + "/@convert",
                queryParams);
        assertEquals(202, response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertNotNull(node);
        assertEquals("conversionScheduled", node.get("entity-type").getTextValue());
        String id = node.get("conversionId").getTextValue();
        assertNotNull(id, node.get("conversionId"));
        String pollingURL = node.get("pollingURL")
                .getTextValue();
        String computedPollingURL = String.format("http://localhost:18090/api/v1/conversions/%s/poll", id);
        assertNotNull(computedPollingURL, pollingURL);
        MultivaluedMap<String, String> headers = response.getHeaders();
        List<String> location = headers.get("Location");
        assertEquals(1, location.size());
        assertEquals(computedPollingURL, location.get(0));

        // wait for the conversion to finish
        eventService.waitForAsyncCompletion();

        // polling URL should redirect to the result URL when done
        client.setFollowRedirects(false);
        WebResource wr = client.resource(pollingURL);
        response = wr.get(ClientResponse.class);
        assertEquals(303, response.getStatus());
        headers = response.getHeaders();
        location = headers.get("Location");
        assertEquals(1, location.size());
        String resultURL = location.get(0);
        assertEquals(String.format("http://localhost:18090/api/v1/conversions/%s/result", id), resultURL);

        // retrieve the converted blob following redirect
        client.setFollowRedirects(true);
        wr = client.resource(pollingURL);
        response = wr.get(ClientResponse.class);
        assertEquals(200, response.getStatus());

        wr = client.resource(resultURL);
        response = wr.get(ClientResponse.class);
        assertEquals(200, response.getStatus());
    }

}
