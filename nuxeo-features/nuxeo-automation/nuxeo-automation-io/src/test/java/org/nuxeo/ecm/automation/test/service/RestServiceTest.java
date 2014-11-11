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

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.io.services.enricher.ContentEnricher;
import org.nuxeo.ecm.automation.io.services.enricher.HeaderDocEvaluationContext;
import org.nuxeo.ecm.automation.io.services.enricher.RestEvaluationContext;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JsonDocumentWriter;
import org.nuxeo.ecm.automation.test.service.enrichers.MockEnricher;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @since 5.7.3
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.automation.io", "org.nuxeo.ecm.actions" })
@LocalDeploy("org.nuxeo.ecm.automation.io:testrestcontrib.xml")
public class RestServiceTest extends BaseRestTest {

    @Before
    public void doBefore() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "folder1",
                "Folder");
        doc = session.createDocument(doc);
        for (int i = 0; i < 3; i++) {
            doc = session.createDocumentModel("/folder1", "doc" + i, "Note");
            doc.setPropertyValue("dc:title", "Note " + i);
            session.createDocument(doc);
        }
        session.save();
    }

    @Test
    public void testDocumentJson() throws Exception {
        DocumentModel doc = session.getDocument(new PathRef("/folder1/doc0"));
        String json = getFullDocumentAsJson(doc, null);
        json = json.replace(doc.getId(), "the-doc-id");
        json = json.replace(doc.getParentRef().toString(), "the-parent-id");
        File file = FileUtils.getResourceFileFromContext("test-expected-document1.json");
        String expected = FileUtils.readFile(file);
        assertEqualsJson(expected, json);
    }

    @Test
    public void testDocumentJsonWithNullArray() throws Exception {
        DocumentModel doc = session.getDocument(new PathRef("/folder1/doc0"));
        doc.setPropertyValue("dc:subjects", null); // set array to null
        String json = getFullDocumentAsJson(doc, null);
        json = json.replace(doc.getId(), "the-doc-id");
        json = json.replace(doc.getParentRef().toString(), "the-parent-id");
        File file = FileUtils.getResourceFileFromContext("test-expected-document1.json");
        String expected = FileUtils.readFile(file);
        assertEqualsJson(expected, json);
    }

    @Test
    public void itCanGetTheContentEnricherService() throws Exception {
        assertNotNull(rcs);
    }

    @Test
    public void itCanGetEnrichersFromTheService() throws Exception {
        List<ContentEnricher> cts = rcs.getEnrichers("test", null);
        assertEquals(1, cts.size());
    }

    @Test
    public void enrichersCanHaveParameters() throws Exception {
        // Given a content enricher
        List<ContentEnricher> cts = rcs.getEnrichers("parameters", null);
        assertEquals(1, cts.size());

        // When it has parameters
        MockEnricher mock = (MockEnricher) cts.get(0);
        Map<String, String> params = mock.getParameters();
        assertNotNull(params);
        assertFalse(params.isEmpty());

        // Then these should be made available to the enricher
        assertEquals("value1", params.get("param1"));
    }

    @Test
    public void itCanFilterEnrichersByCategory() throws Exception {
        List<ContentEnricher> cts = rcs.getEnrichers("anothertest", null);
        assertEquals(2, cts.size());
    }

    @Test
    public void itCanWriteToContext() throws Exception {

        // Given some input context (header + doc)
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator jg = getJsonGenerator(out);
        DocumentModel folder = session.getDocument(new PathRef("/folder1"));
        RestEvaluationContext ec = new HeaderDocEvaluationContext(folder,
                getFakeHeaders(), null);

        // When the service write to the context
        jg.writeStartObject();
        rcs.writeContext(jg, ec);
        jg.writeEndObject();
        jg.flush();

        // Then it is filled with children enricher
        String jsonFolder = out.toString();
        JsonNode node = parseJson(jsonFolder);
        assertEquals("documents",
                node.get("children").get("entity-type").getValueAsText());

    }

    @Test
    public void documentWriterUsesTheRestConributorService() throws Exception {
        // Given a document
        DocumentModel folder = session.getDocument(new PathRef("/folder1"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator jg = getJsonGenerator(out);

        // When it is written as Json with appropriate headers
        JsonDocumentWriter.writeDocument(jg, folder, NO_SCHEMA,
                new HashMap<String, String>(), getFakeHeaders(), null);
        jg.flush();

        // Then it contains contextParameters with contributor
        JsonNode node = parseJson(out);
        assertNotNull(node.get("contextParameters").get("children"));

        // When it is written as Json with empty headers
        out = new ByteArrayOutputStream();
        jg = getJsonGenerator(out);
        JsonDocumentWriter.writeDocument(jg, folder, NO_SCHEMA,
                new HashMap<String, String>(), null, null);
        jg.flush();

        // Then it contains contextParameters with contributor
        node = parseJson(out);
        assertNull(node.get("contextParameters").get("children"));

    }

    @Test
    public void itCanContributeWithBreadcrumb() throws Exception {
        // Given a document
        DocumentModel folder = session.getDocument(new PathRef("/folder1/doc0"));

        // When it is written as Json with breadcrumb context category
        String jsonFolder = getDocumentAsJson(folder, "breadcrumb");
        // Then it contains the breadcrumb in contextParameters
        JsonNode node = parseJson(jsonFolder);
        JsonNode breadCrumbEntries = node.get("contextParameters").get(
                "breadcrumb").get("entries");
        assertEquals("/folder1",
                breadCrumbEntries.get(0).get("path").getValueAsText());
        assertEquals("/folder1/doc0",
                breadCrumbEntries.get(1).get("path").getValueAsText());
    }

    @Test
    public void itCanContributeWithBreadcrumbWhenExpectingAListOfDocs()
            throws Exception {
        // Given a list of docs
        DocumentModelList docs = session.query("SELECT * FROM Note ORDER BY ecm:name ASC");
        // When are written as Json with breadcrumb context category
        String docsJson = getDocumentsAsJson(docs, "breadcrumb");
        // Then it contains the breadcrumb in contextParameters
        JsonNode jsonDocs = parseJson(docsJson);
        ArrayNode nodes = (ArrayNode) jsonDocs.get("entries");
        int i = 0;
        for (JsonNode node : nodes) {
            JsonNode breadCrumbEntries = node.get("contextParameters").get(
                    "breadcrumb").get("entries");
            assertEquals("/folder1",
                    breadCrumbEntries.get(0).get("path").getValueAsText());
            assertEquals("/folder1/doc" + i,
                    breadCrumbEntries.get(1).get("path").getValueAsText());
            i++;
        }
    }

    @Test
    public void itHasEnricherFilteredWithActionFilters() throws Exception {
        // Given a folder and a doc
        DocumentModel folder = session.getDocument(new PathRef("/folder1"));
        DocumentModel note = session.getDocument(new PathRef("/folder1/doc0"));

        // When it is written as Json whith test category
        String jsonFolder = getDocumentAsJson(folder);
        String jsonNote = getDocumentAsJson(note);

        // Then it contains the children in contextParameters if folderish
        JsonNode node = parseJson(jsonFolder);
        JsonNode children = node.get("contextParameters").get("children");
        assertNotNull(children);

        node = parseJson(jsonNote);
        children = node.get("contextParameters").get("children");
        assertNull(children);

    }

}
