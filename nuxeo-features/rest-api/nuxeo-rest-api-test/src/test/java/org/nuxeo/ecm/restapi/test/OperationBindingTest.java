/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.trace.Trace;
import org.nuxeo.ecm.automation.core.trace.TracerFactory;
import org.nuxeo.ecm.automation.core.util.PaginableDocumentModelList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.restapi.server.jaxrs.adapters.OperationAdapter;
import org.nuxeo.ecm.restapi.server.jaxrs.blob.BlobAdapter;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

/**
 * @since 5.7.2 - Test the Rest binding to run operations
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@LocalDeploy({ "org.nuxeo.ecm.platform.restapi.test:operation-contrib.xml" })
@Jetty(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class OperationBindingTest extends BaseTest {

    private static final Integer TIMEOUT = Integer.valueOf(1000 * 60 * 5); // 5min

    private static String PARAMS = "{\"params\":{\"one\":\"1\",\"two\": 2}}";

    @Inject
    protected TracerFactory factory;

    @Override
    @Before
    public void doBefore() throws Exception {
        super.doBefore();
        // Activate trace mode
        if (!factory.getRecordingState()) {
            factory.toggleRecording();
        }
    }

    @Test
    public void itCanRunAnOperationOnaDocument() throws Exception {

        // Given a document and an operation
        DocumentModel note = RestServerInit.getNote(0, session);

        // When i call the REST binding on the document resource
        ClientResponse response = getResponse(RequestType.POSTREQUEST, "id/" + note.getId() + "/@"
                + OperationAdapter.NAME + "/testOp", PARAMS);

        // Then the operation is called on the document
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        // Then the operation is called on all children documents
        Trace trace = factory.getTrace("testOp");
        assertEquals(1, trace.getCalls().size());

        Map<?, ?> parameters = (Map<?, ?>) trace.getCalls().get(0).getVariables().get(Constants.VAR_RUNTIME_CHAIN);

        assertEquals("1", parameters.get("one"));
        assertEquals(2, parameters.get("two"));
        assertEquals(note.getId(), ((DocumentModel) trace.getOutput()).getId());
    }

    @Test
    public void itCanRunAChainOnADocument() throws Exception {
        // Given a document and an operation
        DocumentModel note = RestServerInit.getNote(0, session);

        // When i call the REST binding on the document resource
        ClientResponse response = getResponse(RequestType.POSTREQUEST, "id/" + note.getId() + "/@"
                + OperationAdapter.NAME + "/testChain", "{}");

        // Then the operation is called twice on the document
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Then the operation is called on all children documents
        Trace trace = factory.getTrace("testChain");
        assertEquals(2, trace.getCalls().size());

        Map<?, ?> parameters = trace.getCalls().get(0).getParameters();

        assertEquals("One", parameters.get("one"));
        assertEquals(2L, parameters.get("two"));
        assertEquals(note.getId(), ((DocumentModel) trace.getOutput()).getId());

        parameters = trace.getCalls().get(1).getParameters();
        assertEquals(4L, parameters.get("two"));
        assertEquals("Two", parameters.get("one"));

    }

    @Test
    public void itCanRunAChainOnMutlipleDocuments() throws Exception {
        // Given a folder
        DocumentModel folder = RestServerInit.getFolder(1, session);

        // When i call the REST binding on the children resource

        getResponse(RequestType.POSTREQUEST, "id/" + folder.getId() + "/@children/@" + OperationAdapter.NAME
                + "/testOp", PARAMS);

        // Then the operation is called on all children documents
        Trace trace = factory.getTrace("testOp");
        assertEquals(1, trace.getCalls().size());
        assertEquals(session.getChildren(folder.getRef()).size(),
                ((PaginableDocumentModelList) trace.getOutput()).size());

    }

    @Test
    public void itCanRunAutomationWithBlob() throws Exception {
        // Given a file
        DocumentModel file = RestServerInit.getFile(1, session);

        // When i call the REST binding on the blob resource
        getResponse(RequestType.POSTREQUEST, "id/" + file.getId() + "/@" + BlobAdapter.NAME + "/file:content/@"
                + OperationAdapter.NAME + "/testOp", PARAMS);

        // Then the operation is called on a document blob
        Trace trace = factory.getTrace("testOp");
        assertTrue(trace.getOutput() instanceof Blob);
    }

    @Test
    public void automationResourceIsAlsoAvailableBehindAPIRoot() throws Exception {
        ClientConfig config = new DefaultClientConfig();
        Client client = Client.create(config);
        client.setConnectTimeout(TIMEOUT);
        client.setReadTimeout(TIMEOUT);
        client.addFilter(new HTTPBasicAuthFilter("Administrator", "Administrator"));
        WebResource wr = client.resource("http://localhost:18090/api/v1/automation/doc");

        Builder builder = wr.accept(MediaType.TEXT_HTML);
        ClientResponse response = builder.get(ClientResponse.class);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    /**
     * @since 7.1
     */
    @Test
    public void itShouldReturnCustomHttpStatusWhenSuccess()
            throws Exception {
        String param = "{\"params\":{\"isFailing\":\"false\"}}";
        ClientResponse response = getResponse(RequestType.POSTREQUEST,
                "@" + OperationAdapter.NAME + "/Test.HttpStatus", param);
        assertEquals(206, response.getStatus());
    }

    /**
     * @since 7.1
     */
    @Test
    public void itShouldReturnCustomHttpStatusWhenFailure()
            throws Exception {
        String param = "{\"params\":{\"isFailing\":\"true\"}}";
        ClientResponse response = getResponse(RequestType.POSTREQUEST,
                "@" + OperationAdapter.NAME + "/Test.HttpStatus", param);
        assertEquals(405, response.getStatus());
    }
}
