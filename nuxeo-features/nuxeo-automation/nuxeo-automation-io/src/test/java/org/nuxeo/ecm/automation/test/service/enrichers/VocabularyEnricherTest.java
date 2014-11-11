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
package org.nuxeo.ecm.automation.test.service.enrichers;

import org.codehaus.jackson.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.io.services.enricher.VocabularyEnricher;
import org.nuxeo.ecm.automation.test.service.BaseRestTest;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @since 6.0
 */
@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.automation.io", "org.nuxeo.ecm.actions" })
@LocalDeploy("org.nuxeo.ecm.automation.io:test-vocabulary-enricher-contrib.xml")
public class VocabularyEnricherTest extends BaseRestTest {

    @Before
    public void doBefore() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "folder", "Folder");
        doc = session.createDocument(doc);

        // Empty values
        doc = session.createDocumentModel("/folder", "doc1", "Note");
        doc.setPropertyValue("dc:title", "Note1");
        session.createDocument(doc);

        // Multiple values
        doc = session.createDocumentModel("/folder", "doc2", "Note");
        doc.setPropertyValue("dc:title", "Note2");
        doc.setPropertyValue("dc:subjects", new String[]{
            "parent/subject1", "parent/subject2"
        });
        session.createDocument(doc);
        session.save();
    }

    @Test
    public void testNoParameters() throws Exception {
        // Given a document
        DocumentModel doc = session.getDocument(new PathRef("/folder/doc1"));

        // When it is written as Json with breadcrumb context category
        String jsonDoc = getDocumentAsJson(doc, "vocabularies_no_params");

        // Then it contains the breadcrumb in contextParameters
        JsonNode node = parseJson(jsonDoc);
        assertFalse(node.get("contextParameters").has("vocabularies_no_params"));
    }

    @Test
    public void testInvalidField() throws Exception {
        // Given a document
        DocumentModel doc = session.getDocument(new PathRef("/folder/doc1"));

        // When it is written as Json using a vocabulary enricher
        // with an invalid field parameter
        String jsonDoc = getDocumentAsJson(doc, "vocabularies_invalid_field");

        // Then the enricher's results in contextParameters is empty
        JsonNode node = parseJson(jsonDoc);
        JsonNode vocabs = node.get("contextParameters").get("vocabularies_invalid_field");
        assertEquals(0, vocabs.size());
    }

    @Test
    public void testNullProperty() throws Exception {
        // Given a document
        DocumentModel doc = session.getDocument(new PathRef("/folder/doc1"));

        // When it is written as Json using a vocabulary enricher
        // with a null property value
        String jsonDoc = getDocumentAsJson(doc, "vocabularies_null_property");

        // Then an empty list of labels is returned
        JsonNode node = parseJson(jsonDoc);
        JsonNode vocabs = node.get("contextParameters").get("vocabularies_null_property");
        JsonNode labels = vocabs.get("dc:coverage");
        assertEquals(0, labels.size());
    }

    @Test
    public void testInvalidDirectory() throws Exception {
        // Given a document
        DocumentModel doc = session.getDocument(new PathRef("/folder/doc1"));

        // When it is written as Json using a vocabulary enricher
        // with an inexistent directory
        String jsonDoc = getDocumentAsJson(doc, "vocabularies_invalid_directory");

        // Then the enricher's results in contextParameters is empty
        JsonNode node = parseJson(jsonDoc);
        JsonNode vocabs = node.get("contextParameters").get("vocabularies_invalid_directory");
        assertEquals(0, vocabs.size());
    }

    @Test
    public void testDbl0nVocabulary() throws Exception {

        // Given a document with l10nsubjects
        DocumentModel doc = session.getDocument(new PathRef("/folder/doc2"));

        // When it is written as Json with breadcrumb context category
        String jsonDoc = getDocumentAsJson(doc, "vocabularies");

        // Then i get a list of labels
        JsonNode node = parseJson(jsonDoc);
        JsonNode l10nsubjects = node.get("contextParameters").get(
            "l10nsubjects");
        assertNotNull(l10nsubjects);
        assertTrue(l10nsubjects.isObject());

        String[] languages = {"en", "fr"};
        JsonNode labels = l10nsubjects.get("dc:subjects");
        assertNotNull(labels);
        assertTrue(labels.isArray());
        assertEquals(2, labels.size());

        for (int i = 0; i < labels.size(); i++) {
            JsonNode entry = labels.get(i);
            String id = entry.get("id").getTextValue();

            String[] keyParts = id.split(VocabularyEnricher.KEY_SEPARATOR);
            String parentKey = keyParts[0], childKey = keyParts[1];

            for (String lang : languages) {
                String expected = parentKey + "_" + lang +
                    VocabularyEnricher.KEY_SEPARATOR + childKey + "_" + lang;
                String label = entry.get("label_" + lang).getTextValue();
                assertEquals(expected, label);
            }
        }
    }

}
