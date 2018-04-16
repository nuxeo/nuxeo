/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.security.ACL.LOCAL_ACL;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.EVERYTHING;
import static org.nuxeo.ecm.restapi.test.BaseTest.RequestType.POST;
import static org.nuxeo.ecm.restapi.test.BaseTest.RequestType.PUT;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.restapi.jaxrs.io.RestConstants;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 9.1
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, RestServerFeature.class })
@ServletContainer(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class DocumentAutoVersioningTest extends BaseTest {

    @Test
    @Deploy("org.nuxeo.ecm.platform.restapi.test.test:source-based-versioning-contrib.xml")
    public void iCanUpdateDocumentWithSourceCondition() throws Exception {

        JSONDocumentNode jsonDoc;
        DocumentModel note = RestServerInit.getNote(0, session);
        try (CloseableClientResponse response = getResponse(RequestType.GET, "id/" + note.getId())) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            assertEquals("0.1", note.getVersionLabel());
            jsonDoc = new JSONDocumentNode(response.getEntityInputStream());
        }

        jsonDoc.setPropertyValue("dc:title", "New title !");
        Map<String, String> headers = new HashMap<>();
        headers.put(RestConstants.SOURCE, "REST");
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "id/" + note.getId(), jsonDoc.asJson(),
                headers)) {
            fetchInvalidations();

            note = RestServerInit.getNote(0, session);
            assertEquals("1.0", note.getVersionLabel());
        }
    }

    @Test
    public void iCanDoCollaborativeVersioning() throws IOException {
        // This test should check the default behaviour which is collaborative versioning
        // meaning the minor version should increment if the last contributor has changed

        String id;
        DocumentModel folder = RestServerInit.getFolder(0, session);

        String data = "{ " + "     \"entity-type\": \"document\"," + "     \"type\": \"File\","
                + "     \"name\":\"myFile\"," + "     \"properties\": {" + "         \"dc:title\":\"My title\""
                + "     }" + "}";

        try (CloseableClientResponse response = getResponse(POST, "path" + folder.getPathAsString(), data)) {
            fetchInvalidations();

            JsonNode node = mapper.readTree(response.getEntityInputStream());
            id = node.get("uid").asText();
            assertTrue(StringUtils.isNotBlank(id));
        }

        // Add 'Everything' permission for user0
        ACPImpl acp = new ACPImpl();
        ACLImpl acl = new ACLImpl(LOCAL_ACL);
        ACE ace = new ACE("user0", EVERYTHING, true);
        acl.add(ace);
        acp.addACL(acl);

        DocumentRef idRef = new IdRef(id);
        session.setACP(idRef, acp, true);
        fetchInvalidations();

        DocumentModel doc = session.getDocument(idRef);
        assertEquals("Administrator", doc.getPropertyValue("dc:lastContributor"));
        assertEquals("0.0", doc.getVersionLabel());

        String payload = "{  " + "         \"entity-type\": \"document\"," + "         \"name\": \"myFile\","
                + "         \"type\": \"File\"," + "         \"state\": \"project\","
                + "         \"title\": \"New title\"," + "         \"properties\": {"
                + "             \"dc:description\":\"myDesc\"" + "         }" + "     }";

        service = getServiceFor("user0", "user0");
        try (CloseableClientResponse response = getResponse(PUT, "id/" + doc.getId(), payload)) {
            fetchInvalidations();

            doc = session.getDocument(idRef);
            DocumentModel lastVersion = session.getLastDocumentVersion(idRef);
            assertFalse(lastVersion.isCheckedOut());
            assertEquals("0.1", lastVersion.getVersionLabel());
            assertTrue(doc.isCheckedOut());
            assertEquals("user0", doc.getPropertyValue("dc:lastContributor"));
            assertEquals("0.1+", doc.getVersionLabel());
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.restapi.test.test:time-based-versioning-contrib.xml")
    public void iCanDoTimeBasedVersioning() throws IOException, InterruptedException {

        String id;
        DocumentModel folder = RestServerInit.getFolder(0, session);

        String data = "{ " + "     \"entity-type\": \"document\"," + "     \"type\": \"File\","
                + "     \"name\":\"myFile\"," + "     \"properties\": {" + "         \"dc:title\":\"My title\""
                + "     }" + "}";

        try (CloseableClientResponse response = getResponse(POST, "path" + folder.getPathAsString(), data)) {
            fetchInvalidations();

            JsonNode node = mapper.readTree(response.getEntityInputStream());
            id = node.get("uid").asText();
            assertTrue(StringUtils.isNotBlank(id));
        }

        Thread.sleep(1000);

        String payload = "{  " + "         \"entity-type\": \"document\"," + "         \"name\": \"myFile\","
                + "         \"type\": \"File\"," + "         \"state\": \"project\","
                + "         \"title\": \"New title\"," + "         \"properties\": {"
                + "             \"dc:description\":\"myDesc\"" + "         }" + "     }";

        try (CloseableClientResponse response = getResponse(PUT, "id/" + id, payload)) {
            fetchInvalidations();

            DocumentRef idRef = new IdRef(id);
            DocumentModel doc = session.getDocument(idRef);
            assertEquals("1.0", doc.getVersionLabel());
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.restapi.test.test:lifecycle-based-versioning-contrib.xml")
    public void iCanDoLifeCycleBasedVersioning() throws IOException {

        DocumentModel folder = RestServerInit.getFolder(0, session);

        String data = "{ " + "     \"entity-type\": \"document\"," + "     \"type\": \"File\","
                + "     \"name\":\"myFile\"," + "     \"properties\": {" + "         \"dc:title\":\"My title\""
                + "     }" + "}";

        try (CloseableClientResponse response = getResponse(POST, "path" + folder.getPathAsString(), data)) {
            fetchInvalidations();

            JsonNode node = mapper.readTree(response.getEntityInputStream());
            String id = node.get("uid").asText();
            assertTrue(StringUtils.isNotBlank(id));

            DocumentRef idRef = new IdRef(id);
            DocumentModel doc = session.getDocument(idRef);
            doc.followTransition("approve");
            session.saveDocument(doc);
            fetchInvalidations();

            doc = session.getDocument(idRef);
            assertEquals("0.1", doc.getVersionLabel());
        }
    }
}
