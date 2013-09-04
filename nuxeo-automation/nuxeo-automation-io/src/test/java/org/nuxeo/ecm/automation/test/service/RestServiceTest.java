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
package org.nuxeo.ecm.automation.test.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.io.services.contributor.HeaderDocEvaluationContext;
import org.nuxeo.ecm.automation.io.services.contributor.RestContributor;
import org.nuxeo.ecm.automation.io.services.contributor.RestContributorService;
import org.nuxeo.ecm.automation.io.services.contributor.RestContributorServiceImpl;
import org.nuxeo.ecm.automation.io.services.contributor.RestEvaluationContext;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JsonDocumentWriter;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 *
 *
 * @since 5.7.3
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.automation.io" })
@LocalDeploy("org.nuxeo.ecm.automation.io:testrestcontrib.xml")
public class RestServiceTest {

    /**
     *
     */
    private static final String[] NO_SCHEMA = new String[] {};

    @Inject
    RestContributorService rcs;

    @Inject
    CoreSession session;

    @Inject
    JsonFactory factory;

    @Before
    public void doBefore() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "folder1",
                "Folder");
        session.createDocument(doc);

        for (int i = 0; i < 3; i++) {
            doc = session.createDocumentModel("/folder1", "doc" + i, "Note");
            session.createDocument(doc);
        }
    }

    @Test
    public void itCanGetTheRestContributorService() throws Exception {
        assertNotNull(rcs);
    }

    @Test
    public void itCanGetContributorsFromTheService() throws Exception {
        List<RestContributor> cts = rcs.getContributors("test", null);
        assertEquals(1, cts.size());
    }

    @Test
    public void itCanFilterContributorsByCategory() throws Exception {
        List<RestContributor> cts = rcs.getContributors("anothertest", null);
        assertEquals(2, cts.size());
    }

    @Test
    public void itCanWriteToContext() throws Exception {

        // Given some input context (header + doc)
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator jg = getJsonGenerator(out);
        DocumentModel folder = session.getDocument(new PathRef("/folder1"));
        RestEvaluationContext ec = new HeaderDocEvaluationContext(folder,
                getFakeHeaders());

        // When the service write to the context
        jg.writeStartObject();
        rcs.writeContext(jg, ec);
        jg.writeEndObject();
        jg.flush();

        // Then it is filled with children contributor
        JsonNode node = parseJson(out);
        assertEquals("documents",
                node.get("children").get("entity-type").getValueAsText());

    }

    @Test
    public void documentWriterUsesTheRestConributorService() throws Exception {
        // Given a document
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator jg = getJsonGenerator(out);
        DocumentModel folder = session.getDocument(new PathRef("/folder1"));

        // When it is written as Json with appropriate headers
        JsonDocumentWriter.writeDocument(jg, folder, NO_SCHEMA,
                new HashMap<String, String>(), getFakeHeaders());
        jg.flush();

        // Then it contains contextParameters with contributor
        JsonNode node = parseJson(out);
        assertNotNull(node.get("contextParameters").get("children"));

        // When it is written as Json with empty headers
        out = new ByteArrayOutputStream();
        jg = getJsonGenerator(out);
        JsonDocumentWriter.writeDocument(jg, folder, NO_SCHEMA,
                new HashMap<String, String>(), null);
        jg.flush();

        // Then it contains contextParameters with contributor
        node = parseJson(out);
        assertNull(node.get("contextParameters").get("children"));

    }

    @Test
    public void itCanContributeWithBreadcrumb() throws Exception {
        // Given a document
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator jg = getJsonGenerator(out);
        DocumentModel folder = session.getDocument(new PathRef("/folder1/doc0"));

        // When it is written as Json with breadcrumb context category
        JsonDocumentWriter.writeDocument(jg, folder, NO_SCHEMA,
                new HashMap<String, String>(), getFakeHeaders("breadcrumb"));
        jg.flush();

        // Then it contains the breadcrumb in contextParameters
        JsonNode node = parseJson(out);
        JsonNode breadCrumbEntries = node.get("contextParameters").get(
                "breadcrumb").get("entries");
        assertEquals("/folder1",
                breadCrumbEntries.get(0).get("path").getValueAsText());
        assertEquals("/folder1/doc0",
                breadCrumbEntries.get(1).get("path").getValueAsText());

    }

    /**
     * @param out
     * @return
     * @throws IOException
     * @throws JsonProcessingException
     *
     */
    private JsonNode parseJson(ByteArrayOutputStream out)
            throws JsonProcessingException, IOException {
        String json = out.toString();
        System.out.println(json);
        ObjectMapper m = new ObjectMapper();
        return m.readTree(json);
    }

    private JsonGenerator getJsonGenerator(OutputStream out) throws IOException {
        return factory.createJsonGenerator(out);
    }

    private HttpHeaders getFakeHeaders() {
        return getFakeHeaders("test");
    }

    private HttpHeaders getFakeHeaders(String category) {
        HttpHeaders headers = mock(HttpHeaders.class);

        when(
                headers.getRequestHeader(JsonDocumentWriter.DOCUMENT_PROPERTIES_HEADER)).thenReturn(
                Arrays.asList(NO_SCHEMA));

        when(
                headers.getRequestHeader(RestContributorServiceImpl.NXCONTENT_CATEGORY_HEADER)).thenReturn(
                Arrays.asList(new String[] { category }));
        return headers;
    }

}
