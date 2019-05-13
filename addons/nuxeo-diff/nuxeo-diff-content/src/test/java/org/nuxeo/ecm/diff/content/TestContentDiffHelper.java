/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.diff.content.adapter.base.ContentDiffConversionType;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests the {@link ContentDiffHelper} class.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.6
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.diff.content:OSGI-INF/content-diff-adapter-framework.xml")
@Deploy("org.nuxeo.diff.content:OSGI-INF/content-diff-adapter-contrib.xml")
public class TestContentDiffHelper {

    @Inject
    protected CoreSession session;

    /**
     * Tests
     * {@link ContentDiffHelper#getContentDiffURL(DocumentModel, DocumentModel, String, ContentDiffConversionType)}
     */
    @Test
    public void testGetContentDiffURL() {
        DocumentModel leftDoc = createDoc(session, "leftDoc", "File", "Left doc");
        DocumentModel rightDoc = createDoc(session, "rightDoc", "File", "Right doc");
        String contentDiffURL = ContentDiffHelper.getContentDiffURL(leftDoc, rightDoc, "file:content",
                ContentDiffConversionType.html.name(), "en_GB");
        StringBuilder sb = new StringBuilder("restAPI/contentDiff/test/");
        sb.append(leftDoc.getId());
        sb.append("/");
        sb.append(rightDoc.getId());
        sb.append("/file:content/?conversionType=html&locale=en_GB");
        assertEquals(sb.toString(), contentDiffURL);
    }

    /**
     * Tests {@link ContentDiffHelper#isDisplayHtmlConversion(Serializable)}.
     */
    @Test
    public void testIsDisplayHtmlConversion() {

        // Non content property => OK
        String strProp = "A string property";
        assertTrue(ContentDiffHelper.isDisplayHtmlConversion(strProp));

        // Non blacklisted content property (OpenDocument) => OK
        Blob blob = Blobs.createBlob("A non blacklisted blob", "application/vnd.oasis.opendocument.text");
        assertTrue(ContentDiffHelper.isDisplayHtmlConversion((Serializable) blob));

        // Blacklisted content property (pdf) => KO
        blob = Blobs.createBlob("A blacklisted blob", "application/pdf");
        assertFalse(ContentDiffHelper.isDisplayHtmlConversion((Serializable) blob));
    }

    /**
     * Tests {@link ContentDiffHelper#isDisplayTextConversion(Serializable)}.
     */
    @Test
    public void testIsDisplayTextConversion() {

        // Non content property => KO
        String strProp = "A string property";
        assertFalse(ContentDiffHelper.isDisplayTextConversion(strProp));

        // Content property with a mime type associated to a content differ
        // (HTML) => KO
        Blob blob = Blobs.createBlob("An HTML blob", "text/html");
        assertFalse(ContentDiffHelper.isDisplayTextConversion((Serializable) blob));

        // Content property with no mime type associated to a content differ
        // (OpenDocument) => OK
        blob = Blobs.createBlob("An OpenDocument blob", "application/vnd.oasis.opendocument.text");
        assertTrue(ContentDiffHelper.isDisplayTextConversion((Serializable) blob));
    }

    /**
     * Tests {@link ContentDiffHelper#isContentProperty(Serializable)}
     */
    @Test
    public void testIsContentProperty() {

        // Non content property => KO
        String strProp = "A string property";
        assertFalse(ContentDiffHelper.isContentProperty(strProp));

        // Content property => OK
        Blob blob = Blobs.createBlob("A content property");
        assertTrue(ContentDiffHelper.isContentProperty((Serializable) blob));
    }

    /**
     * Creates a document given the specified id, type and title.
     *
     * @param session the session
     * @param id the document id
     * @param type the document type
     * @param title the document title
     * @return the document model
     */
    protected DocumentModel createDoc(CoreSession session, String id, String type, String title) {

        DocumentModel doc = session.createDocumentModel("/", id, type);
        doc.setPropertyValue("dc:title", title);
        return session.createDocument(doc);
    }
}
