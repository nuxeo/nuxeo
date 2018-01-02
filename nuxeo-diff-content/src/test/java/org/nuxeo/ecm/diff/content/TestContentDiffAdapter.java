/*
 * (C) Copyright 2012-2018 Nuxeo (http://nuxeo.com/) and others.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import org.apache.commons.lang.SystemUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.convert.api.ConverterNotRegistered;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.diff.content.adapter.base.ContentDiffConversionType;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests the {@link ContentDiffAdapter}.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.6
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = ContentDiffRepositoryInit.class)
@Deploy({ "org.nuxeo.ecm.platform.convert:OSGI-INF/convert-service-contrib.xml", "org.nuxeo.diff.content" })
public class TestContentDiffAdapter {

    @Inject
    protected CoreSession session;

    /**
     * Tests {@link ContentDiffAdapter#getFileContentDiffBlobs(DocumentModel, ContentDiffConversionType, Locale)} on
     * plain text files.
     */
    @Test
    public void testPlainTextFilesContentDiff() {

        // Get left and right plain text docs
        DocumentModel leftDoc = session.getDocument(new PathRef(ContentDiffRepositoryInit.getLeftPlainTextDocPath()));
        DocumentModel rightDoc = session.getDocument(new PathRef(ContentDiffRepositoryInit.getRightPlainTextDocPath()));

        // Get content diff adapter for left doc
        ContentDiffAdapter contentDiffAdapter = leftDoc.getAdapter(ContentDiffAdapter.class);
        assertNotNull(contentDiffAdapter);

        // Get content diff blobs
        List<Blob> contentDiffBlobs = contentDiffAdapter.getFileContentDiffBlobs(rightDoc, null, Locale.ENGLISH);
        assertNotNull(contentDiffBlobs);
        assertEquals(1, contentDiffBlobs.size());

        Blob contentDiffBlob = contentDiffBlobs.get(0);
        assertNotNull(contentDiffBlob);

        // Check content diff
        checkContentDiff("plain_text_content_diff.html", contentDiffBlob);
    }

    /**
     * Tests {@link ContentDiffAdapter#getFileContentDiffBlobs(DocumentModel, ContentDiffConversionType, Locale)} on
     * HTML files.
     */
    @Test
    public void testHTMLFilesContentDiff() {

        // Get left and right HTML docs
        DocumentModel leftDoc = session.getDocument(new PathRef(ContentDiffRepositoryInit.getLeftHTMLDocPath()));
        DocumentModel rightDoc = session.getDocument(new PathRef(ContentDiffRepositoryInit.getRightHTMLDocPath()));

        // Get content diff adapter for left doc
        ContentDiffAdapter contentDiffAdapter = leftDoc.getAdapter(ContentDiffAdapter.class);
        assertNotNull(contentDiffAdapter);

        // Get content diff blobs using html conversion
        List<Blob> contentDiffBlobs = contentDiffAdapter.getFileContentDiffBlobs(rightDoc,
                ContentDiffConversionType.html, Locale.ENGLISH);
        assertNotNull(contentDiffBlobs);
        assertEquals(1, contentDiffBlobs.size());

        Blob contentDiffBlob = contentDiffBlobs.get(0);
        assertNotNull(contentDiffBlob);

        // Check content diff
        checkContentDiff("html_content_diff.html", contentDiffBlob);
    }

    /**
     * Tests {@link ContentDiffAdapter#getFileContentDiffBlobs(DocumentModel, ContentDiffConversionType, Locale)} on
     * JSON files.
     */
    @Test
    public void testJSONFilesContentDiff() {

        // Get left and right HTML docs
        DocumentModel leftDoc = session.getDocument(new PathRef(ContentDiffRepositoryInit.getLeftJSONDocPath()));
        DocumentModel rightDoc = session.getDocument(new PathRef(ContentDiffRepositoryInit.getRightJSONDocPath()));

        // Get content diff adapter for left doc
        ContentDiffAdapter contentDiffAdapter = leftDoc.getAdapter(ContentDiffAdapter.class);
        assertNotNull(contentDiffAdapter);

        // Get content diff blobs using html conversion
        List<Blob> contentDiffBlobs = contentDiffAdapter.getFileContentDiffBlobs(rightDoc,
                ContentDiffConversionType.html, Locale.ENGLISH);
        assertNotNull(contentDiffBlobs);
        assertEquals(1, contentDiffBlobs.size());

        Blob contentDiffBlob = contentDiffBlobs.get(0);
        assertNotNull(contentDiffBlob);

        // Check content diff
        checkContentDiff("json_content_diff.html", contentDiffBlob);
    }

    /**
     * Tests {@link ContentDiffAdapter#getFileContentDiffBlobs(DocumentModel, ContentDiffConversionType, Locale)} on
     * Office files using a text conversion.
     */
    @Test
    public void testOfficeFilesTextConversionContentDiff() {

        // Get left and right Office docs
        DocumentModel leftDoc = session.getDocument(new PathRef(ContentDiffRepositoryInit.getLeftOfficeDocPath()));
        DocumentModel rightDoc = session.getDocument(new PathRef(ContentDiffRepositoryInit.getRightOfficeDocPath()));

        // Get content diff adapter for left doc
        ContentDiffAdapter contentDiffAdapter = leftDoc.getAdapter(ContentDiffAdapter.class);
        assertNotNull(contentDiffAdapter);

        // Get content diff blobs using text conversion
        List<Blob> contentDiffBlobs = contentDiffAdapter.getFileContentDiffBlobs(rightDoc,
                ContentDiffConversionType.text, Locale.ENGLISH);
        assertNotNull(contentDiffBlobs);
        assertEquals(1, contentDiffBlobs.size());

        Blob contentDiffBlob = contentDiffBlobs.get(0);
        assertNotNull(contentDiffBlob);

        // Check content diff
        checkContentDiff("office_text_conversion_content_diff.html", contentDiffBlob);
    }

    /**
     * Tests {@link ContentDiffAdapter#getFileContentDiffBlobs(DocumentModel, ContentDiffConversionType, Locale)} on
     * files that don't have any "2text" or "2html" converter registered (images).
     */
    @Test
    public void testImageFilesContentDiff() {
        // Get left and right image docs
        DocumentModel leftDoc = session.getDocument(new PathRef(ContentDiffRepositoryInit.getLeftImageDocPath()));
        DocumentModel rightDoc = session.getDocument(new PathRef(ContentDiffRepositoryInit.getRightImageDocPath()));

        // Get content diff adapter for left doc
        ContentDiffAdapter contentDiffAdapter = leftDoc.getAdapter(ContentDiffAdapter.class);
        assertNotNull(contentDiffAdapter);

        // Try to get content diff blobs using text conversion
        try {
            contentDiffAdapter.getFileContentDiffBlobs(rightDoc, ContentDiffConversionType.text, Locale.ENGLISH);
            fail("No png2text converter is registered, call should have thrown a ConverterNotRegistered exception.");
        } catch (ConverterNotRegistered cnr) {
            assertEquals("Converter for sourceMimeType = image/png, destinationMimeType = text/plain is not registered",
                    cnr.getMessage());
        }

        // Try to get content diff blobs using html conversion
        try {
            contentDiffAdapter.getFileContentDiffBlobs(rightDoc, ContentDiffConversionType.html, Locale.ENGLISH);
            fail("No png2html converter is registered, call should have thrown a ConverterNotRegistered exception.");
        } catch (ConverterNotRegistered cnr) {
            assertEquals("Converter for sourceMimeType = image/png, destinationMimeType = text/html is not registered",
                    cnr.getMessage());
        }
    }

    protected void checkContentDiff(String expectedBlobPath, Blob contentDiffBlob) {
        try {
            Blob expectedblob = Blobs.createBlob(FileUtils.getResourceFileFromContext(expectedBlobPath));
            String expected = expectedblob.getString();
            String actual = contentDiffBlob.getString();
            if (SystemUtils.IS_OS_WINDOWS) {
                // make tests pass under Windows
                expected = expected.trim();
                expected = expected.replace("\n", "");
                expected = expected.replace("\r", "");
                actual = actual.trim();
                actual = actual.replace("\n", "");
                actual = actual.replace("\r", "");
            }
            assertEquals(expected, actual);
        } catch (IOException ioe) {
            fail("Error while getting content diff blob strings");
        }
    }
}
