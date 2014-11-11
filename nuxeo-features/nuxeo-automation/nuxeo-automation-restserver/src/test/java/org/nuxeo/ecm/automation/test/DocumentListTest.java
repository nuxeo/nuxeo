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

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonNode;
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

import com.sun.jersey.api.client.ClientResponse;

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

}
