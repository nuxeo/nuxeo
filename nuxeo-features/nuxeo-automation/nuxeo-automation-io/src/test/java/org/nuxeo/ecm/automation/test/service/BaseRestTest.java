/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nelson Silva <nelson.silva@inevo.pt>
 */
package org.nuxeo.ecm.automation.test.service;

import com.google.inject.Inject;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.ecm.automation.io.services.enricher.ContentEnricherService;
import org.nuxeo.ecm.automation.io.services.enricher.ContentEnricherServiceImpl;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JsonDocumentListWriter;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JsonDocumentWriter;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.skyscreamer.jsonassert.JSONAssert;

import javax.ws.rs.core.HttpHeaders;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 5.9.6
 */
public class BaseRestTest {
    protected static final String[] NO_SCHEMA = new String[] {};

    private static final String[] ALL_SCHEMAS = new String[] { "*" };

    @Inject
    protected CoreSession session;

    @Inject
    ContentEnricherService rcs;

    @Inject
    JsonFactory factory;

    protected void assertEqualsJson(String expected, String actual) throws Exception {
        JSONAssert.assertEquals(expected, actual, true);
    }

    /**
     * Parses a JSON string into a JsonNode
     *
     * @param json
     * @return
     * @throws java.io.IOException
     * @throws org.codehaus.jackson.JsonProcessingException
     *
     */
    protected JsonNode parseJson(String json) throws JsonProcessingException,
        IOException {
        ObjectMapper m = new ObjectMapper();
        return m.readTree(json);
    }

    protected JsonNode parseJson(ByteArrayOutputStream out)
            throws JsonProcessingException, IOException {
        return parseJson(out.toString());
    }

    /**
     * Returns the JSON representation of the document with all schemas. A
     * category may be passed to have impact on the Content Enrichers.
     */
    protected String getFullDocumentAsJson(DocumentModel doc, String category)
            throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator jg = getJsonGenerator(out);
        // When it is written as Json with appropriate headers
        JsonDocumentWriter.writeDocument(jg, doc, ALL_SCHEMAS, null,
            getFakeHeaders(category), null);
        jg.flush();
        return out.toString();
    }

    /**
     * Returns the JSON representation of the document. A category may be passed
     * to have impact on the Content Enrichers
     */
    protected String getDocumentAsJson(DocumentModel doc, String category)
            throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator jg = getJsonGenerator(out);
        // When it is written as Json with appropriate headers
        JsonDocumentWriter.writeDocument(jg, doc, NO_SCHEMA,
                new HashMap<String, String>(), getFakeHeaders(category), null);
        jg.flush();
        return out.toString();
    }

    /**
     * Returns the JSON representation of these docs. A category may be passed
     * to have impact on the Content Enrichers
     */
    protected String getDocumentsAsJson(List<DocumentModel> docs,
            String category) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator jg = getJsonGenerator(out);
        // When it is written as Json with appropriate headers
        JsonDocumentListWriter.writeDocuments(jg, docs, NO_SCHEMA,
            getFakeHeaders(category), null);
        jg.flush();
        return out.toString();
    }

    /**
     * Returns the JSON representation of the document.
     */
    protected String getDocumentAsJson(DocumentModel doc) throws Exception {
        return getDocumentAsJson(doc, null);
    }

    protected JsonGenerator getJsonGenerator(OutputStream out) throws IOException {
        return factory.createJsonGenerator(out);
    }

    protected HttpHeaders getFakeHeaders() {
        return getFakeHeaders(null);
    }

    protected HttpHeaders getFakeHeaders(String category) {
        HttpHeaders headers = mock(HttpHeaders.class);

        when(
                headers.getRequestHeader(JsonDocumentWriter.DOCUMENT_PROPERTIES_HEADER)).thenReturn(
                Arrays.asList(NO_SCHEMA));

        when(
                headers.getRequestHeader(ContentEnricherServiceImpl.NXCONTENT_CATEGORY_HEADER)).thenReturn(
                Arrays.asList(new String[] { category == null ? "test"
                        : category }));
        return headers;
    }
}
