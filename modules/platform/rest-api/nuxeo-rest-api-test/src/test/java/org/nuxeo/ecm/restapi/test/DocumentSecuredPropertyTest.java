/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.ecm.restapi.test;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ_WRITE;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response.StatusType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.schema.PropertyCharacteristicHandler;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Tests the secured property handled by {@link PropertyCharacteristicHandler}.
 *
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(RestServerFeature.class)
@Deploy("org.nuxeo.ecm.core.api.tests:OSGI-INF/test-documentmodel-secured-types-contrib.xml")
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class DocumentSecuredPropertyTest extends BaseTest {

    public static final String USER_1 = "user1";

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected CoreSession session;

    @Before
    @Override
    public void doBefore() throws Exception {
        super.doBefore();
        ACE ace = ACE.builder(USER_1, READ_WRITE).creator(session.getPrincipal().getName()).isGranted(true).build();
        ACP acp = new ACPImpl();
        acp.addACE(ACL.LOCAL_ACL, ace);
        session.setACP(new PathRef("/folder_2"), acp, false);
        session.save();
        txFeature.nextTransaction();
    }

    @Test
    public void testAdministratorCanCreate() {
        try (CloseableClientResponse response = getResponse(RequestType.POST, "path/folder_2",
                instantiateDocumentBody())) {
            StatusType status = response.getStatusInfo();
            assertEquals("HTTP Reason: " + status.getReasonPhrase(), SC_CREATED, status.getStatusCode());
        }
    }

    @Test
    public void testUserCanNotCreate() throws IOException {
        this.service = getServiceFor(USER_1, USER_1);
        try (CloseableClientResponse response = getResponse(RequestType.POST, "path/folder_2",
                instantiateDocumentBody())) {
            StatusType status = response.getStatusInfo();
            assertEquals("HTTP Reason: " + status.getReasonPhrase(), SC_BAD_REQUEST, status.getStatusCode());
            // not necessary to close stream, it will be done by response close
            JsonNode root = mapper.readTree(response.getEntityInputStream());
            assertEquals("Cannot set the value of property: scalar since it is readonly", getErrorMessage(root));
        }
    }

    @Test
    public void testUserCanUseRepositoryUsingEmptyWithDefaultAdapter() throws IOException {
        this.service = getServiceFor(USER_1, USER_1);
        String docModel;
        try (CloseableClientResponse response = getResponse(RequestType.GET, "path/folder_2/@emptyWithDefault",
                multiOf("type", "Secured", "properties", "*"))) {
            StatusType status = response.getStatusInfo();
            assertEquals("HTTP Reason: " + status.getReasonPhrase(), SC_OK, status.getStatusCode());

            // edit response for next call
            var root = new JSONDocumentNode(response.getEntityInputStream());
            root.node.remove("title");
            root.node.put("name", "file");
            var unsecureComplex = (ObjectNode) root.getPropertyAsJsonNode("secured:unsecureComplex");
            unsecureComplex.put("scalar2", "I can");
            root.setPropertyValue("secured:unsecureComplex", unsecureComplex);
            docModel = root.asJson();
        }
        try (CloseableClientResponse response = getResponse(RequestType.POST, "path/folder_2", docModel)) {
            StatusType status = response.getStatusInfo();
            assertEquals("HTTP Reason: " + status.getReasonPhrase(), SC_CREATED, status.getStatusCode());
        }
        // edit response for next call
        var root = new JSONDocumentNode(docModel);
        var unsecureComplex = (ObjectNode) root.getPropertyAsJsonNode("secured:unsecureComplex");
        root.node.with("properties").removeAll();
        unsecureComplex.put("scalar2", "I still can!");
        root.setPropertyValue("secured:unsecureComplex", unsecureComplex);
        docModel = root.asJson();
        try (CloseableClientResponse response = getResponse(RequestType.POST, "path/folder_2", docModel)) {
            StatusType status = response.getStatusInfo();
            assertEquals("HTTP Reason: " + status.getReasonPhrase(), SC_CREATED, status.getStatusCode());
        }
    }

    @NotNull
    protected String instantiateDocumentBody() {
        try {
            return mapper.writeValueAsString( //
                    Map.of("entity-type", "document", //
                            "name", "secured_document", //
                            "type", "Secured", //
                            "properties", Map.of("secured:scalar", "I'm secured !") //
                    ));
        } catch (JsonProcessingException e) {
            throw new AssertionError("Unable to serialize document body", e);
        }
    }

}
