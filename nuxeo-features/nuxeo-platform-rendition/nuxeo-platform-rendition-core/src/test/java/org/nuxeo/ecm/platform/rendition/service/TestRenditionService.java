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
import org.mvel2.ast.AssertNode;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.rendition.Rendition;
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
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.core.convert.api", "org.nuxeo.ecm.core.convert",
        "org.nuxeo.ecm.core.convert.plugins", "org.nuxeo.ecm.platform.convert",
        "org.nuxeo.ecm.platform.rendition.api",
        "org.nuxeo.ecm.platform.rendition.core",
        "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.platform.commandline.executor" })
@LocalDeploy("org.nuxeo.ecm.platform.rendition.core:test-rendition-contrib.xml")
public class TestRenditionService {

    public static final String PDF_RENDITION_DEFINITION = "pdf";

    @Inject
    protected CoreSession session;

    @Inject
    protected EventService eventService;

    @Inject
    protected RenditionService renditionService;

    @Test
    public void serviceRegistration() {
        assertNotNull(renditionService);
    }

    @Test
    public void testDeclaredRenditionDefinitions() {
        List<RenditionDefinition> renditionDefinitions = renditionService.getDeclaredRenditionDefinitions();
        assertFalse(renditionDefinitions.isEmpty());
        assertEquals(3, renditionDefinitions.size());

        RenditionServiceImpl renditionServiceImpl = (RenditionServiceImpl) renditionService;
        assertTrue(renditionServiceImpl.renditionDefinitions.containsKey(PDF_RENDITION_DEFINITION));
        RenditionDefinition rd = renditionServiceImpl.renditionDefinitions.get(PDF_RENDITION_DEFINITION);
        assertNotNull(rd);
        assertEquals(PDF_RENDITION_DEFINITION, rd.getName());
        assertEquals("blobToPDF", rd.getOperationChain());
        assertEquals("label.rendition.pdf", rd.getLabel());
        assertTrue(rd.isEnabled());

        assertTrue(renditionServiceImpl.renditionDefinitions.containsKey("renditionDefinitionWithCustomOperationChain"));
        rd = renditionServiceImpl.renditionDefinitions.get("renditionDefinitionWithCustomOperationChain");
        assertNotNull(rd);
        assertEquals("renditionDefinitionWithCustomOperationChain",
                rd.getName());
        assertEquals("Dummy", rd.getOperationChain());
    }

    @Test
    public void testAvailableRenditionDefinitions() throws Exception {

        DocumentModel file = session.createDocumentModel("/", "file", "File");
        file.setPropertyValue("dc:title", "TestFile");
        file = session.createDocument(file);

        List<RenditionDefinition> renditionDefinitions = renditionService.getAvailableRenditionDefinitions(file);
        assertEquals(1, renditionDefinitions.size());

        // add a blob
        StringBlob blob = new StringBlob("I am a Blob");
        file.setPropertyValue("file:content", blob);
        file = session.saveDocument(file);

        // rendition should be available now
        renditionDefinitions = renditionService.getAvailableRenditionDefinitions(file);
        assertEquals(2, renditionDefinitions.size());

    }

    @Test
    public void doPDFRendition() throws ClientException {
        DocumentModel file = createBlobFile();

        DocumentRef renditionDocumentRef = renditionService.storeRendition(
                file, PDF_RENDITION_DEFINITION);
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

        // now refetch the rendition
        Rendition rendition = renditionService.getRendition(file,
                PDF_RENDITION_DEFINITION);
        assertNotNull(rendition);
        assertTrue(rendition.isStored());
        assertEquals(renditionDocument.getRef(),
                rendition.getHostDocument().getRef());

        // now update the document
        file.setPropertyValue("dc:description", "I have been updated");
        file = session.saveDocument(file);
        rendition = renditionService.getRendition(file,
                PDF_RENDITION_DEFINITION);
        assertNotNull(rendition);
        assertFalse(rendition.isStored());

    }

    @Test
    public void doRenditionVersioning() throws ClientException {
        DocumentModel file = createBlobFile();

        assertEquals("project", file.getCurrentLifeCycleState());
        file.followTransition("approve");
        assertEquals("approved", file.getCurrentLifeCycleState());

        // create a version of the document
        file.putContextData(VersioningService.VERSIONING_OPTION,
                VersioningOption.MINOR);
        file = session.saveDocument(file);
        session.save();
        eventService.waitForAsyncCompletion();
        assertEquals("0.1", file.getVersionLabel());

        // make a rendition on the document
        DocumentRef renditionDocumentRef = renditionService.storeRendition(
                file, PDF_RENDITION_DEFINITION);
        DocumentModel renditionDocument = session.getDocument(renditionDocumentRef);
        assertNotNull(renditionDocument);
        assertEquals(
                file.getId(),
                renditionDocument.getPropertyValue(RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY));
        DocumentModel lastVersion = session.getLastDocumentVersion(file.getRef());
        assertEquals(
                lastVersion.getId(),
                renditionDocument.getPropertyValue(RENDITION_SOURCE_ID_PROPERTY));

        // check that the redition is a version
        assertTrue(renditionDocument.isVersion());
        // check same life-cycle state
        assertEquals(file.getCurrentLifeCycleState(), renditionDocument.getCurrentLifeCycleState());
        // check that version label of the rendition is the same as the source
        assertEquals(file.getVersionLabel(),
                renditionDocument.getVersionLabel());

        // fetch the rendition to check we have the same DocumentModel
        Rendition rendition = renditionService.getRendition(file,
                PDF_RENDITION_DEFINITION);
        assertNotNull(rendition);
        assertTrue(rendition.isStored());
        assertEquals(renditionDocument.getRef(),
                rendition.getHostDocument().getRef());

        // update the source Document
        file.setPropertyValue("dc:description", "I have been updated");
        file = session.saveDocument(file);
        assertEquals("0.1+", file.getVersionLabel());

        // get the rendition from checkedout doc
        rendition = renditionService.getRendition(file,
                PDF_RENDITION_DEFINITION);
        assertNotNull(rendition);
        // rendition should be live
        assertFalse(rendition.isStored());
        // Live Rendition should point to the live doc
        assertTrue(rendition.getHostDocument().getRef().equals(file.getRef()));

        // needed for MySQL otherwise version order could be random
        DatabaseHelper.DATABASE.maybeSleepToNextSecond();

        // now store rendition for version 0.2
        rendition = renditionService.getRendition(file,
                PDF_RENDITION_DEFINITION, true);
        assertEquals("0.2", rendition.getHostDocument().getVersionLabel());
        assertTrue(rendition.isStored());

        assertTrue(rendition.getHostDocument().isVersion());
        System.out.println(rendition.getHostDocument().getACP());

        // check that version 0.2 of file was created
        List<DocumentModel> versions = session.getVersions(file.getRef());
        assertEquals(2, versions.size());

        // check retrieval
        Rendition rendition2 = renditionService.getRendition(file,
                PDF_RENDITION_DEFINITION, false);
        assertTrue(rendition2.isStored());
        assertEquals(rendition.getHostDocument().getRef(),
                rendition2.getHostDocument().getRef());

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
        renditionService.storeRendition(proxy, PDF_RENDITION_DEFINITION);
    }

    @Test
    public void shouldNotCreateANewVersionForACheckedInDocument()
            throws ClientException {
        DocumentModel file = createBlobFile();

        DocumentRef versionRef = file.checkIn(VersioningOption.MINOR, null);
        file.refresh(DocumentModel.REFRESH_STATE, null);
        DocumentModel version = session.getDocument(versionRef);

        DocumentRef renditionDocumentRef = renditionService.storeRendition(
                version, "pdf");
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
        renditionService.storeRendition(file, PDF_RENDITION_DEFINITION);
    }

    @Test(expected = RenditionException.class)
    public void shouldNotRenderWithAnUndefinedRenditionDefinition()
            throws ClientException {
        DocumentModel file = session.createDocumentModel("/", "dummy", "File");
        file = session.createDocument(file);
        renditionService.storeRendition(file, "undefinedRenditionDefinition");
    }

    @Test(expected = RenditionException.class)
    public void shouldNotRenderWithAnUndefinedOperationChain()
            throws ClientException {
        DocumentModel file = session.createDocumentModel("/", "dummy", "File");
        file = session.createDocument(file);
        renditionService.storeRendition(file,
                "renditionDefinitionWithUnknownOperationChain");
    }

    @Test
    public void shouldRenderOnFolder()
            throws Exception {
        DocumentModel folder = session.createDocumentModel("/", "dummy", "Folder");        
        folder = session.createDocument(folder);
        Rendition rendition = renditionService.getRendition(folder, "renditionDefinitionWithCustomOperationChain");
        assertNotNull(rendition);
        assertNotNull(rendition.getBlob());
        assertEquals(rendition.getBlob().getString(), "dummy");
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

        DocumentRef renditionDocumentRef = renditionService.storeRendition(
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
        renditionService.storeRendition(folder, PDF_RENDITION_DEFINITION);
    }

}
