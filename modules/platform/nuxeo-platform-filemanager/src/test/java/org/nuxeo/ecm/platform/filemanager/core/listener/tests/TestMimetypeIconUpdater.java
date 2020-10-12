/*
 * (C) Copyright 2006-2019 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.filemanager.core.listener.tests;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry.PDF_EXTENSION;
import static org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry.PDF_MIMETYPE;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Testing the mime type icon updater listener. This listener should update mime type of a blob when this one is dirty
 * (updated). When the blob is about is on file:content, it also updates the common:icon field setting the right icon
 * according to the mime type
 *
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * @author Benjamin Jalon <bjalon@nuxeo.com>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.types.api")
@Deploy("org.nuxeo.ecm.platform.types.core")
@Deploy("org.nuxeo.ecm.platform.filemanager")
@Deploy("org.nuxeo.ecm.platform.filemanager:OSGI-INF/core-type-contrib.xml")
public class TestMimetypeIconUpdater {

    /**
     * @since 11.1
     */
    public static final String PSD_MIME_TYPE = "application/photoshop";

    /**
     * @since 11.1
     */
    public static final String PSD_EXTENSION = "psd";

    @Inject
    protected CoreSession coreSession;

    /**
     * Testing a mime type and icon update (only done on file:content)
     */
    @Test
    public void testMimeTypeUpdater() {
        DocumentModel file = createDocument("File", PDF_EXTENSION, null);
        Blob blob = (Blob) file.getProperty("file", "content");
        assertNotNull(blob);
        String mt = blob.getMimeType();
        assertNotNull(mt);
        assertEquals(PDF_MIMETYPE, mt);

        String icon = (String) file.getProperty("common", "icon");
        assertNotNull(icon);
        assertEquals("/icons/pdf.png", icon);

        // removing blob
        removeMainBlob(file);
        icon = (String) file.getProperty("common", "icon");
        assertNotNull(icon);
        assertEquals("/icons/pdf.png", icon);
    }

    /**
     * Testing mime type update with a schema without prefix. https://jira.nuxeo.org/browse/NXP-3972
     * <a href="https://jira.nuxeo.org/browse/NXP-3972">NXP-3972</a>
     */
    @Test
    public void testMimeTypeUpdaterWithoutPrefix() {
        DocumentModel doc = createDocument("WithoutPrefixDocument", PDF_EXTENSION, null);
        Blob blob = (Blob) doc.getProperty("wihtoutpref", "blob");
        assertNotNull(blob);
        String mt = blob.getMimeType();
        assertNotNull(mt);
        assertEquals(PDF_MIMETYPE, mt);
    }

    /**
     * Testing mime type update with a schema with prefix. https://jira.nuxeo.org/browse/NXP-3972
     * <a href="https://jira.nuxeo.org/browse/NXP-3972">NXP-3972</a>
     */
    @Test
    public void testMimeTypeUpdaterWithPrefix() {
        DocumentModel doc = createDocument("SimpleBlobDocument", PDF_EXTENSION, null);
        Blob blob = (Blob) doc.getProperty("simpleblob", "blob");
        assertNotNull(blob);
        String mt = blob.getMimeType();
        assertNotNull(mt);
        assertEquals(PDF_MIMETYPE, mt);
    }

    @Test
    public void testEmptyMimeTypeWithCharset() throws Exception {
        DocumentModel doc = createDocument("File", PDF_EXTENSION, "application/octet-stream; charset=UTF-8");
        Blob blob = (Blob) doc.getProperty("file", "content");
        assertNotNull(blob);
        assertEquals("application/pdf", blob.getMimeType());
    }

    @Test
    public void testMimeTypeUpdaterFolderish() {
        // Workspace is folderish and contains the file schema
        DocumentModel doc = createDocument("Workspace", PDF_EXTENSION, null);
        Blob blob = (Blob) doc.getProperty("file", "content");
        assertNotNull(blob);
        String mt = blob.getMimeType();
        assertNotNull(mt);
        assertEquals(PDF_MIMETYPE, mt);

        String icon = (String) doc.getProperty("common", "icon");
        assertNull(icon); // default icon, not overridden by mime type
    }

    /**
     * Ensures that the document blob mime type is normalized if possible, even if the current blob have a mime type.
     *
     * @since 11.1
     */
    @Test
    public void shouldNormalizeMimeTypedIfPossible() {
        DocumentModel documentModel = createDocument("File", PSD_EXTENSION, "image/photoshop");
        Blob blob = (Blob) documentModel.getProperty("file", "content");
        assertNotNull(blob);
        String mt = blob.getMimeType();
        assertNotNull(mt);
        assertEquals(PSD_MIME_TYPE, mt);

        blob.setMimeType("zz-winassoc-psd");
        documentModel.setProperty("file", "content", blob);
        coreSession.saveDocument(documentModel);

        blob = (Blob) documentModel.getProperty("file", "content");
        assertEquals(PSD_MIME_TYPE, blob.getMimeType());
    }

    /**
     * Ensures that if we are not able to normalize the document blob mime type (i.e mime type is unknown in Nuxeo), we should keep
     * the original one.
     *
     * @since 11.1
     */
    @Test
    public void shouldKeepTheOriginalMimeTypeIfWeCannotNormalize() {
        DocumentModel documentModel = createDocument("File", PSD_EXTENSION, "application/unknown");
        Blob blob = (Blob) documentModel.getProperty("file", "content");
        assertEquals("application/unknown", blob.getMimeType());
    }

    /**
     * Ensures that if we import a document with an attachment then we should set the mime type.
     *
     * @since 11.1
     */
    @Test
    public void shouldDefineMimeTypeWhenImportDocument() {
        DocumentModel documentModel = importDocument("1239876", "File", PSD_EXTENSION);
        Blob blob = (Blob) documentModel.getProperty("file", "content");
        assertEquals(PSD_MIME_TYPE, blob.getMimeType());

        documentModel = importDocument("1239890", "File", PDF_EXTENSION);
        blob = (Blob) documentModel.getProperty("file", "content");
        assertEquals(PDF_MIMETYPE, blob.getMimeType());
    }

    /**
     * Creates a document.
     *
     * @see #createDocumentModel(String, String, String)
     * @since 11.1
     */
    protected DocumentModel createDocument(String documentType, String extension, String mimeType) {
        DocumentModel documentModel = createDocumentModel(documentType, extension, mimeType);

        documentModel = coreSession.createDocument(documentModel);
        coreSession.saveDocument(documentModel);
        coreSession.save();

        return documentModel;
    }

    /**
     * Imports a document model for a given {@code id}, {@code documentType} and {@code extension}.
     *
     * @param documentId the document id to set, cannot be {@code null} or {@code empty}
     * @see #createDocumentModel(String, String, String)
     * @since 11.1
     */
    protected DocumentModel importDocument(String documentId, String documentType, String extension) {
        DocumentModel documentModel = createDocumentModel(documentType, extension, null);
        ((DocumentModelImpl) documentModel).setId(documentId);
        coreSession.importDocuments(List.of(documentModel));

        return documentModel;
    }

    /**
     * Creates a document model for a given {@code documentType}, {@code extension} and {@code mimeType}.
     *
     * @param documentType the document type to create, cannot be {@code null}
     * @param extension the file extension to set, cannot be {@code null}
     * @param mimeType the mime type to set, can be {@code null}
     * @return the document model
     * @throws NullPointerException if {@code documentType} or {@code extension} is {@code null}
     * @since 11.1
     */
    protected DocumentModel createDocumentModel(String documentType, String extension, String mimeType) {
        DocumentModel documentModel = coreSession.createDocumentModel("/", "testFile", requireNonNull(documentType));
        documentModel.setProperty("dublincore", "title", "TestFile");
        Blob blob = Blobs.createBlob("SOMEDUMMYDATA", null, null, String.format("test.%s", requireNonNull(extension)));
        blob.setMimeType(mimeType);

        switch (documentType) {
        case "File":
        case "Workspace":
            documentModel.setProperty("file", "content", blob);
            break;
        case "SimpleBlobDocument":
            documentModel.setProperty("simpleblob", "blob", blob);
            break;
        case "WithoutPrefixDocument":
            documentModel.setProperty("wihtoutpref", "blob", blob);
            break;
        default:
            throw new IllegalArgumentException(
                    String.format("Undefined behaviour for document type '%s'", documentType));

        }
        return documentModel;
    }

    protected DocumentModel removeMainBlob(DocumentModel doc) {
        doc.setPropertyValue("file:content", null);
        doc = coreSession.saveDocument(doc);
        coreSession.save();
        return doc;
    }

}
