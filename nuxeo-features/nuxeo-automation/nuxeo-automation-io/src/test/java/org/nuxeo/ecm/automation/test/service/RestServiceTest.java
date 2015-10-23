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

import java.io.File;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @since 5.7.3
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.automation.io", "org.nuxeo.ecm.actions" })
@LocalDeploy("org.nuxeo.ecm.automation.io:testrestcontrib.xml")
public class RestServiceTest extends BaseRestTest {

    @Before
    public void doBefore() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "folder1", "Folder");
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
    public void itCanContributeWithBreadcrumb() throws Exception {
        // Given a document
        DocumentModel folder = session.getDocument(new PathRef("/folder1/doc0"));

        // When it is written as Json with breadcrumb context category
        String jsonFolder = getDocumentAsJson(folder, "breadcrumb");
        // Then it contains the breadcrumb in contextParameters
        JsonNode node = parseJson(jsonFolder);
        JsonNode breadCrumbEntries = node.get("contextParameters").get("breadcrumb").get("entries");
        assertEquals("/folder1", breadCrumbEntries.get(0).get("path").getValueAsText());
        assertEquals("/folder1/doc0", breadCrumbEntries.get(1).get("path").getValueAsText());
    }

    @Test
    public void itCanContributeWithBreadcrumbWhenExpectingAListOfDocs() throws Exception {
        // Given a list of docs
        DocumentModelList docs = session.query("SELECT * FROM Note ORDER BY ecm:name ASC");
        // When are written as Json with breadcrumb context category
        String docsJson = getDocumentsAsJson(docs, "breadcrumb");
        // Then it contains the breadcrumb in contextParameters
        JsonNode jsonDocs = parseJson(docsJson);
        ArrayNode nodes = (ArrayNode) jsonDocs.get("entries");
        int i = 0;
        for (JsonNode node : nodes) {
            JsonNode breadCrumbEntries = node.get("contextParameters").get("breadcrumb").get("entries");
            assertEquals("/folder1", breadCrumbEntries.get(0).get("path").getValueAsText());
            assertEquals("/folder1/doc" + i, breadCrumbEntries.get(1).get("path").getValueAsText());
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
        JsonAssert jsonAssert = JsonAssert.on(jsonNote);
        jsonAssert.has("contextParameters.children.entries").length(0);

        jsonAssert = JsonAssert.on(jsonFolder);
        jsonAssert.has("contextParameters.children.entries").length(3);
    }

}
