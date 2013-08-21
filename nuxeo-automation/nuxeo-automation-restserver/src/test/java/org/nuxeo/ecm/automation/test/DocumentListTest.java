/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.test;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonNode;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.rest.jaxrs.adapters.ChildrenAdapter;
import org.nuxeo.ecm.automation.rest.jaxrs.adapters.PageProviderAdapter;
import org.nuxeo.ecm.automation.rest.jaxrs.adapters.SearchAdapter;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.common.base.Joiner;
import com.sun.jersey.api.client.ClientResponse;

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * Test the various ways to query for document lists
 *
 * @since 5.7.2
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@Jetty(port = 18090)
@LocalDeploy("nuxeo-automation-restserver:pageprovider-test-contrib.xml")
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class DocumentListTest extends BaseTest {

    @Test
    public void iCanGetTheChildrenOfADocument() throws Exception {
        // Given a folder
        DocumentModel folder = RestServerInit.getFolder(1, session);

        // When I query for it children
        ClientResponse response = getResponse(RequestType.GET,
                "id/" + folder.getId() + "/@" + ChildrenAdapter.NAME);

        // Then I get its children as JSON
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(session.getChildren(folder.getRef()).size(),
                getEntries(node).size());
    }

    @Test
    @Ignore
    public void iCanSearchInFullTextForDocuments() throws Exception {
        // Given a note with "nuxeo" in its description
        DocumentModel note = RestServerInit.getNote(0, session);
        note.setPropertyValue("dc:description",
                "nuxeo one platform to rule them all");
        session.saveDocument(note);
        session.save();

        // When I search for "nuxeo"
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("fullText", "nuxeo");
        ClientResponse response = getResponse(RequestType.GET, "path/@"
                + SearchAdapter.NAME, queryParams);

        // Then I get the document in the result
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(1, getEntries(node).size());

    }

    @Test
    public void iCanUseAPageProvider() throws Exception {
        // Given a note with "nuxeo" in its description
        DocumentModel folder = RestServerInit.getFolder(1, session);

        // When I search for "nuxeo"
        ClientResponse response = getResponse(RequestType.GET,
                "path" + folder.getPathAsString() + "/@"
                        + PageProviderAdapter.NAME + "/TEST_PP");

        // Then I get the two document in the result
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(2, getEntries(node).size());

    }

    @Test
    public void iCanDeleteAListOfDocuments() throws Exception {
        // Given two notes
        DocumentModel note1 = RestServerInit.getNote(1, session);
        DocumentModel folder0 = RestServerInit.getFolder(0, session);

        // When i call a bulk delete
        String data = Joiner.on(";").join(Arrays.asList(new String[]{"id=" + note1.getId(), "id=" + folder0.getId()}));
        ClientResponse response = getResponse(RequestType.DELETE, "/bulk;"+data);

        // Then the documents are removed from repository
        dispose(session);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        assertFalse(session.exists(note1.getRef()));
        assertFalse(session.exists(folder0.getRef()));

    }

    @Test
    public void iCanUpdateDocumentLists() throws Exception {
        // Given two notes
        DocumentModel note1 = RestServerInit.getNote(1, session);
        DocumentModel note2 = RestServerInit.getNote(2, session);


        String data = "{\"entity-type\":\"document\","
                + "\"type\":\"Note\","
                + "\"properties\":{"
                + "    \"dc:description\":\"bulk description\""
                + "  }"
                + "}";

        // When i call a bulk update
        String ids = Joiner.on(";").join(Arrays.asList(new String[]{"id=" + note1.getId(), "id=" + note2.getId()}));
        ClientResponse response = getResponse(RequestType.PUT, "/bulk;"+ids, data );

        // Then the documents are updated accordingly

        dispose(session);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        for(int i : new Integer[]{1,2}) {
            note1 = RestServerInit.getNote(i, session);
            assertEquals("bulk description", note1.getPropertyValue("dc:description"));
        }


    }

}
