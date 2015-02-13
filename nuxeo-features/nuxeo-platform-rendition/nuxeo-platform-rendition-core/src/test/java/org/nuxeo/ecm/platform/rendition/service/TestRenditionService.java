/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.rendition.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.rendition.RenditionException;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.rendition.Constants.FILES_FILES_PROPERTY;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_FACET;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_SOURCE_ID_PROPERTY;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(type = BackendType.H2, init = DefaultRepositoryInit.class, user = "Administrator", cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.core.convert.api", "org.nuxeo.ecm.core.convert",
        "org.nuxeo.ecm.core.convert.plugins", "org.nuxeo.ecm.platform.convert",
        "org.nuxeo.ecm.platform.rendition.api",
        "org.nuxeo.ecm.platform.rendition.core",
        "org.nuxeo.ecm.automation.core" })
@LocalDeploy("org.nuxeo.ecm.platform.rendition.core:test-rendition-contrib.xml")
public class TestRenditionService {

    public static final String PDF_RENDITION_DEFINITION = "pdf";

    @Inject
    protected CoreSession session;

    @Inject
    protected RenditionService renditionService;

    @Test
    public void serviceRegistration() {
        assertNotNull(renditionService);
    }

    @Test
    public void testAvailableRenditionDefinitions() {
        List<RenditionDefinition> renditionDefinitions = renditionService.getAvailableRenditionDefinitions();
        assertFalse(renditionDefinitions.isEmpty());
        assertEquals(2, renditionDefinitions.size());

        RenditionServiceImpl renditionServiceImpl = (RenditionServiceImpl) renditionService;
        assertTrue(renditionServiceImpl.renditionDefinitions.containsKey(PDF_RENDITION_DEFINITION));
        RenditionDefinition rd = renditionServiceImpl.renditionDefinitions.get(PDF_RENDITION_DEFINITION);
        assertNotNull(rd);
        assertEquals(PDF_RENDITION_DEFINITION, rd.getName());
        assertEquals("blobToPDF", rd.getOperationChain());
        assertEquals("label.rendition.pdf", rd.getLabel());
        assertTrue(rd.isEnabled());

        assertTrue(renditionServiceImpl.renditionDefinitions.containsKey("renditionDefinitionWithUndefinedOperationChain"));
        rd = renditionServiceImpl.renditionDefinitions.get("renditionDefinitionWithUndefinedOperationChain");
        assertNotNull(rd);
        assertEquals("renditionDefinitionWithUndefinedOperationChain", rd.getName());
        assertEquals("undefinedOperationChain", rd.getOperationChain());
    }

    @Test
    public void doPDFRendition() throws ClientException {
        DocumentModel file = createBlobFile();

        DocumentRef renditionDocumentRef = renditionService.render(file, PDF_RENDITION_DEFINITION);
        DocumentModel renditionDocument = session.getDocument(renditionDocumentRef);

        assertNotNull(renditionDocument);
        assertTrue(renditionDocument.hasFacet(RENDITION_FACET));
        assertEquals(
                file.getId(),
                renditionDocument.getPropertyValue(RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY));

        DocumentModel lastVersion = session.getLastDocumentVersion(file.getRef());
        assertEquals(
                lastVersion.getId(),
                renditionDocument.getPropertyValue(RENDITION_SOURCE_ID_PROPERTY));

        BlobHolder bh = renditionDocument.getAdapter(BlobHolder.class);
        Blob renditionBlob = bh.getBlob();
        assertNotNull(renditionBlob);
        assertEquals("application/pdf", renditionBlob.getMimeType());
        assertEquals("dummy.txt.pdf", renditionBlob.getFilename());
    }

    protected DocumentModel createBlobFile() throws ClientException {
        Blob blob = createTextBlob("Dummy text", "dummy.txt");
        DocumentModel file = createFileWithBlob(blob, "dummy-file");
        assertNotNull(file);
        return file;
    }

    protected DocumentModel createFileWithBlob(Blob blob, String name)
            throws ClientException {
        DocumentModel file = session.createDocumentModel("/", name, "File");
        BlobHolder bh = file.getAdapter(BlobHolder.class);
        bh.setBlob(blob);
        file = session.createDocument(file);
        return file;
    }

    protected Blob createTextBlob(String content, String filename) {
        Blob blob = new StringBlob(content, "text/plain");
        blob.setFilename(filename);
        return blob;
    }

    @Test(expected = RenditionException.class)
    public void shouldNotRenderAProxyDocument() throws ClientException {
        DocumentModel file = createBlobFile();

        DocumentModel proxy = session.createProxy(file.getRef(), new PathRef(
                "/"));
        renditionService.render(proxy, PDF_RENDITION_DEFINITION);
    }

    @Test
    public void shouldNotCreateANewVersionForACheckedInDocument()
            throws ClientException {
        DocumentModel file = createBlobFile();

        DocumentRef versionRef = file.checkIn(VersioningOption.MINOR, null);
        file.refresh(DocumentModel.REFRESH_STATE, null);
        DocumentModel version = session.getDocument(versionRef);

        DocumentRef renditionDocumentRef = renditionService.render(version,
                "pdf");
        DocumentModel renditionDocument = session.getDocument(renditionDocumentRef);

        assertEquals(
                version.getId(),
                renditionDocument.getPropertyValue(RENDITION_SOURCE_ID_PROPERTY));

        List<DocumentModel> versions = session.getVersions(file.getRef());
        assertFalse(versions.isEmpty());
        assertEquals(1, versions.size());

        DocumentModel lastVersion = session.getLastDocumentVersion(file.getRef());
        assertEquals(version.getRef(), lastVersion.getRef());
    }

    @Test(expected = RenditionException.class)
    public void shouldNotRenderAnEmptyDocument() throws ClientException {
        DocumentModel file = session.createDocumentModel("/", "dummy", "File");
        file = session.createDocument(file);
        renditionService.render(file, PDF_RENDITION_DEFINITION);
    }

    @Test(expected = RenditionException.class)
    public void shouldNotRenderWithAnUndefinedRenditionDefinition()
            throws ClientException {
        DocumentModel file = session.createDocumentModel("/", "dummy", "File");
        file = session.createDocument(file);
        renditionService.render(file, "undefinedRenditionDefinition");
    }

    @Test(expected = RenditionException.class)
    public void shouldNotRenderWithAnUndefinedOperationChain()
            throws ClientException {
        DocumentModel file = session.createDocumentModel("/", "dummy", "File");
        file = session.createDocument(file);
        renditionService.render(file, "renditionDefinitionWithUndefinedOperationChain");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldRemoveFilesBlobsOnARendition() throws ClientException {
        DocumentModel fileDocument = createBlobFile();

        Blob firstAttachedBlob = createTextBlob("first attached blob", "first");
        Blob secondAttachedBlob = createTextBlob("second attached blob",
                "second");

        List<Map<String, Serializable>> files = new ArrayList<Map<String, Serializable>>();
        Map<String, Serializable> file = new HashMap<String, Serializable>();
        file.put("file", (Serializable) firstAttachedBlob);
        file.put("filename", firstAttachedBlob.getFilename());
        files.add(file);
        file = new HashMap<String, Serializable>();
        file.put("file", (Serializable) secondAttachedBlob);
        file.put("filename", secondAttachedBlob.getFilename());
        files.add(file);

        fileDocument.setPropertyValue(FILES_FILES_PROPERTY,
                (Serializable) files);

        DocumentRef renditionDocumentRef = renditionService.render(
                fileDocument, PDF_RENDITION_DEFINITION);
        DocumentModel renditionDocument = session.getDocument(renditionDocumentRef);

        BlobHolder bh = renditionDocument.getAdapter(BlobHolder.class);
        Blob renditionBlob = bh.getBlob();
        assertNotNull(renditionBlob);
        assertEquals("application/pdf", renditionBlob.getMimeType());
        List<Map<String, Serializable>> renditionFiles = (List<Map<String, Serializable>>) renditionDocument.getPropertyValue(FILES_FILES_PROPERTY);
        assertTrue(renditionFiles.isEmpty());
    }

    @Test(expected = RenditionException.class)
    public void shouldNotRenderADocumentWithoutBlobHolder()
            throws ClientException {
        DocumentModel folder = session.createDocumentModel("/", "dummy-folder",
                "Folder");
        folder = session.createDocument(folder);
        renditionService.render(folder, PDF_RENDITION_DEFINITION);
    }

}
