/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;

import javax.ws.rs.core.MultivaluedMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @since 7.3
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@ServletContainer(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
@Deploy("org.nuxeo.ecm.core.cache")
@Deploy("org.nuxeo.ecm.platform.convert")
public class ConverterTest extends BaseTest {

    @Inject
    protected EventService eventService;

    @Test
    public void shouldConvertBlobUsingNamedConverter() {
        doSynchronousConversion("converter", "any2pdf", false);
    }

    protected void doSynchronousConversion(String paramName, String paramValue, boolean convertDocument) {
        DocumentModel doc = createDummyDocument();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle(paramName, paramValue);
        String path = "path" + doc.getPathAsString() + "/";
        if (!convertDocument) {
            path += "@blob/file:content/";
        }
        path += "@convert";
        try (CloseableClientResponse response = getResponse(RequestType.GET, path, queryParams)) {
            assertEquals(200, response.getStatus());
        }
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
        doSynchronousConversion("type", "application/pdf", false);
    }

    @Test
    public void shouldConvertBlobUsingFormat() {
        doSynchronousConversion("format", "pdf", false);
    }

    @Test
    public void shouldConvertDocument() {
        doSynchronousConversion("converter", "any2pdf", true);
    }

    @Test
    public void shouldScheduleAsynchronousConversionUsingNamedConverter() throws IOException {
        doAsynchronousConversion("converter", "any2pdf");
    }

    public void doAsynchronousConversion(String paramName, String paramValue) throws IOException {
        DocumentModel doc = createDummyDocument();

        String pollingURL;
        String computedResultURL;

        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add(paramName, paramValue);
        formData.add("async", "true");
        WebResource wr = service.path("path" + doc.getPathAsString() + "/@convert");
        try (CloseableClientResponse response = CloseableClientResponse.of(
                wr.getRequestBuilder().post(ClientResponse.class, formData))) {
            assertEquals(202, response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertNotNull(node);
            assertEquals("conversionScheduled", node.get("entity-type").textValue());
            String id = node.get("conversionId").textValue();
            assertNotNull(id);
            pollingURL = node.get("pollingURL").textValue();
            String computedPollingURL = String.format("http://localhost:18090/api/v1/conversions/%s/poll", id);
            assertEquals(computedPollingURL, pollingURL);
            String resultURL = node.get("resultURL").textValue();
            computedResultURL = String.format("http://localhost:18090/api/v1/conversions/%s/result", id);
            assertEquals(computedResultURL, resultURL);
        }

        wr = client.resource(pollingURL);
        try (CloseableClientResponse response = CloseableClientResponse.of(wr.get(ClientResponse.class))) {
            assertEquals(200, response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertNotNull(node);
            assertEquals("conversionStatus", node.get("entity-type").textValue());
            String id = node.get("conversionId").textValue();
            assertNotNull(id);
            String resultURL = node.get("resultURL").textValue();
            assertEquals(computedResultURL, resultURL);
            String status = node.get("status").textValue();
            assertTrue(status.equals("running") || status.equals("completed"));
        }

        // wait for the conversion to finish
        eventService.waitForAsyncCompletion();

        // polling URL should redirect to the result URL when done
        String resultURL;
        wr = client.resource(pollingURL);
        try (CloseableClientResponse response = CloseableClientResponse.of(wr.get(ClientResponse.class))) {
            assertEquals(200, response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            resultURL = node.get("resultURL").textValue();
            assertEquals(computedResultURL, resultURL);
        }

        // retrieve the converted blob
        wr = client.resource(resultURL);
        try (CloseableClientResponse response = CloseableClientResponse.of(wr.get(ClientResponse.class))) {
            assertEquals(200, response.getStatus());
        }
    }

    @Test
    public void shouldScheduleAsynchronousConversionUsingMimeType() throws IOException {
        doAsynchronousConversion("type", "application/pdf");
    }

    @Test
    public void shouldScheduleAsynchronousConversionUsingFormat() throws IOException {
        doAsynchronousConversion("format", "pdf");
    }

    @Test
    public void shouldAllowSynchronousConversionUsingPOST() {
        DocumentModel doc = createDummyDocument();

        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("converter", "any2pdf");
        WebResource wr = service.path("path" + doc.getPathAsString() + "/@convert");
        try (CloseableClientResponse response = CloseableClientResponse.of(
                wr.getRequestBuilder().post(ClientResponse.class, formData))) {
            assertEquals(200, response.getStatus());
        }
    }

}
