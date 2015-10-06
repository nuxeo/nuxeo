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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.platform.rendition.Constants.FILES_FILES_PROPERTY;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_FACET;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_SOURCE_ID_PROPERTY;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@RunWith(FeaturesRunner.class)
@Features(RenditionFeature.class)
@LocalDeploy({
        "org.nuxeo.ecm.platform.rendition.core:test-rendition-contrib.xml",
        "org.nuxeo.ecm.platform.rendition.core:test-lazy-rendition-contrib.xml" })
public class TestRenditionService {

    public static final String RENDITION_CORE = "org.nuxeo.ecm.platform.rendition.core";

    private static final String RENDITION_FILTERS_COMPONENT_LOCATION = "test-rendition-filters-contrib.xml";

    private static final String RENDITION_DEFINITION_PROVIDERS_COMPONENT_LOCATION = "test-rendition-definition-providers-contrib.xml";

    public static final String PDF_RENDITION_DEFINITION = "pdf";

    public static final String ZIP_TREE_EXPORT_RENDITION_DEFINITION = "zipTreeExport";

    @Inject
    protected RuntimeHarness runtimeHarness;

    @Inject
    protected CoreFeature coreFeature;

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
        assertEquals(8, renditionDefinitions.size());

        RenditionDefinition rd = renditionDefinitions.stream().filter(
                renditionDefinition -> PDF_RENDITION_DEFINITION.equals(renditionDefinition.getName())).findFirst().get();
        assertNotNull(rd);
        assertEquals(PDF_RENDITION_DEFINITION, rd.getName());
        assertEquals("blobToPDF", rd.getOperationChain());
        assertEquals("label.rendition.pdf", rd.getLabel());
        assertTrue(rd.isEnabled());

        rd = renditionDefinitions.stream().filter(
                renditionDefinition -> "renditionDefinitionWithCustomOperationChain".equals(renditionDefinition.getName())).findFirst().get();
        assertNotNull(rd);
        assertEquals("renditionDefinitionWithCustomOperationChain", rd.getName());
        assertEquals("Dummy", rd.getOperationChain());
    }

    @Test
    public void testAvailableRenditionDefinitions() throws Exception {
        DocumentModel file = session.createDocumentModel("/", "file", "File");
        file.setPropertyValue("dc:title", "TestFile");
        file = session.createDocument(file);

        List<RenditionDefinition> renditionDefinitions = renditionService.getAvailableRenditionDefinitions(file);
        assertEquals(6, renditionDefinitions.size());

        // add a blob
        Blob blob = Blobs.createBlob("I am a Blob");
        file.setPropertyValue("file:content", (Serializable) blob);
        file = session.saveDocument(file);

        // rendition should be available now
        renditionDefinitions = renditionService.getAvailableRenditionDefinitions(file);
        assertEquals(7, renditionDefinitions.size());

    }

    @Test
    public void doPDFRendition() {
        DocumentModel file = createBlobFile();

        DocumentRef renditionDocumentRef = renditionService.storeRendition(file, PDF_RENDITION_DEFINITION);
        DocumentModel renditionDocument = session.getDocument(renditionDocumentRef);

        assertNotNull(renditionDocument);
        assertTrue(renditionDocument.hasFacet(RENDITION_FACET));
        assertEquals(file.getId(), renditionDocument.getPropertyValue(RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY));

        DocumentModel lastVersion = session.getLastDocumentVersion(file.getRef());
        assertEquals(lastVersion.getId(), renditionDocument.getPropertyValue(RENDITION_SOURCE_ID_PROPERTY));

        BlobHolder bh = renditionDocument.getAdapter(BlobHolder.class);
        Blob renditionBlob = bh.getBlob();
        assertNotNull(renditionBlob);
        assertEquals("application/pdf", renditionBlob.getMimeType());
        assertEquals("dummy.txt.pdf", renditionBlob.getFilename());

        // now refetch the rendition
        Rendition rendition = renditionService.getRendition(file, PDF_RENDITION_DEFINITION);
        assertNotNull(rendition);
        assertTrue(rendition.isStored());
        assertEquals(renditionDocument.getRef(), rendition.getHostDocument().getRef());
        assertEquals("/icons/pdf.png", renditionDocument.getPropertyValue("common:icon"));
        assertEquals(renditionBlob.getLength(), renditionDocument.getPropertyValue("common:size"));

        // now update the document
        file.setPropertyValue("dc:description", "I have been updated");
        file = session.saveDocument(file);
        rendition = renditionService.getRendition(file, PDF_RENDITION_DEFINITION);
        assertNotNull(rendition);
        assertFalse(rendition.isStored());

    }

    @Test
    public void doRenditionVersioning() {
        DocumentModel file = createBlobFile();

        assertEquals("project", file.getCurrentLifeCycleState());
        file.followTransition("approve");
        assertEquals("approved", file.getCurrentLifeCycleState());

        // create a version of the document
        file.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);
        file = session.saveDocument(file);
        session.save();
        eventService.waitForAsyncCompletion();
        assertEquals("0.1", file.getVersionLabel());

        // make a rendition on the document
        DocumentRef renditionDocumentRef = renditionService.storeRendition(file, PDF_RENDITION_DEFINITION);
        DocumentModel renditionDocument = session.getDocument(renditionDocumentRef);
        assertNotNull(renditionDocument);
        assertEquals(file.getId(), renditionDocument.getPropertyValue(RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY));
        DocumentModel lastVersion = session.getLastDocumentVersion(file.getRef());
        assertEquals(lastVersion.getId(), renditionDocument.getPropertyValue(RENDITION_SOURCE_ID_PROPERTY));

        // check that the redition is a version
        assertTrue(renditionDocument.isVersion());
        // check same life-cycle state
        assertEquals(file.getCurrentLifeCycleState(), renditionDocument.getCurrentLifeCycleState());
        // check that version label of the rendition is the same as the source
        assertEquals(file.getVersionLabel(), renditionDocument.getVersionLabel());

        // fetch the rendition to check we have the same DocumentModel
        Rendition rendition = renditionService.getRendition(file, PDF_RENDITION_DEFINITION);
        assertNotNull(rendition);
        assertTrue(rendition.isStored());
        assertEquals(renditionDocument.getRef(), rendition.getHostDocument().getRef());

        // update the source Document
        file.setPropertyValue("dc:description", "I have been updated");
        file = session.saveDocument(file);
        assertEquals("0.1+", file.getVersionLabel());

        // get the rendition from checkedout doc
        rendition = renditionService.getRendition(file, PDF_RENDITION_DEFINITION);
        assertNotNull(rendition);
        // rendition should be live
        assertFalse(rendition.isStored());
        // Live Rendition should point to the live doc
        assertTrue(rendition.getHostDocument().getRef().equals(file.getRef()));

        // needed for MySQL otherwise version order could be random
        coreFeature.getStorageConfiguration().maybeSleepToNextSecond();

        // now store rendition for version 0.2
        rendition = renditionService.getRendition(file, PDF_RENDITION_DEFINITION, true);
        assertEquals("0.2", rendition.getHostDocument().getVersionLabel());
        assertTrue(rendition.isStored());

        assertTrue(rendition.getHostDocument().isVersion());
        System.out.println(rendition.getHostDocument().getACP());

        // check that version 0.2 of file was created
        List<DocumentModel> versions = session.getVersions(file.getRef());
        assertEquals(2, versions.size());

        // check retrieval
        Rendition rendition2 = renditionService.getRendition(file, PDF_RENDITION_DEFINITION, false);
        assertTrue(rendition2.isStored());
        assertEquals(rendition.getHostDocument().getRef(), rendition2.getHostDocument().getRef());

    }

    @Test
    public void doZipTreeExportRendition() throws Exception {
        doZipTreeExportRendition(false);
    }

    @Test
    public void doZipTreeExportLazyRendition() throws Exception {
        doZipTreeExportRendition(true);
    }

    protected void doZipTreeExportRendition(boolean isLazy) throws Exception {

        DocumentModel folder = createFolderWithChildren();
        String renditionName = ZIP_TREE_EXPORT_RENDITION_DEFINITION;
        if (isLazy) {
            renditionName += "Lazily";
        }
        Rendition rendition = getRendition(folder, renditionName, true, isLazy);
        assertTrue(rendition.isStored());
        assertTrue(rendition.isCompleted());
        assertEquals(rendition.getHostDocument().getPropertyValue("dc:modified"), rendition.getModificationDate());

        DocumentModel renditionDocument = session.getDocument(rendition.getHostDocument().getRef());

        assertTrue(renditionDocument.hasFacet(RENDITION_FACET));
        assertNull(renditionDocument.getPropertyValue(RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY));
        assertEquals(folder.getId(), renditionDocument.getPropertyValue(RENDITION_SOURCE_ID_PROPERTY));

        BlobHolder bh = renditionDocument.getAdapter(BlobHolder.class);
        Blob renditionBlob = bh.getBlob();
        assertNotNull(renditionBlob);
        assertEquals("application/zip", renditionBlob.getMimeType());
        assertEquals("export.zip", renditionBlob.getFilename());

        // now refetch the rendition
        rendition = renditionService.getRendition(folder, renditionName);
        assertNotNull(rendition);
        assertTrue(rendition.isStored());
        assertEquals(renditionDocument.getRef(), rendition.getHostDocument().getRef());
        assertEquals("/icons/zip.png", renditionDocument.getPropertyValue("common:icon"));
        assertEquals(renditionBlob.getLength(), renditionDocument.getPropertyValue("common:size"));

        // now get a different rendition as a different user
        try (CoreSession userSession = coreFeature.openCoreSession("toto")) {
            folder = userSession.getDocument(folder.getRef());
            Rendition totoRendition = getRendition(folder, renditionName, true, isLazy);
            assertTrue(totoRendition.isStored());
            assertNotEquals(renditionDocument.getRef(), totoRendition.getHostDocument().getRef());
        }

        // now "update" the folder
        folder = session.getDocument(folder.getRef());
        folder.setPropertyValue("dc:description", "I have been updated");
        folder = session.saveDocument(folder);
        rendition = getRendition(folder, renditionName, false, isLazy);
        assertFalse(rendition.isStored());
        assertTrue(rendition.isCompleted());
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(rendition.getBlob().getFile().lastModified());
        assertEquals(cal, rendition.getModificationDate());
        if (isLazy) {
            rendition = renditionService.getRendition(folder, renditionName, false);
            assertEquals(cal, rendition.getModificationDate());
        }
    }

    protected Rendition getRendition(DocumentModel doc, String renditionName, boolean store, boolean isLazy) {
        Rendition rendition = renditionService.getRendition(doc, renditionName, store);
        assertNotNull(rendition);
        if (isLazy) {
            assertFalse(rendition.isStored());
            assertFalse(rendition.isCompleted());
            assertNull(rendition.getModificationDate());
            Blob blob = rendition.getBlob();
            assertEquals(0, blob.getLength());
            assertTrue(blob.getMimeType().contains("empty=true"));
            assertTrue(blob.getFilename().equals("inprogress"));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("interrupted", e);
            }
            eventService.waitForAsyncCompletion(5000);
            rendition = renditionService.getRendition(doc, renditionName, store);
        }
        return rendition;
    }

    protected DocumentModel createBlobFile() {
        Blob blob = createTextBlob("Dummy text", "dummy.txt");
        DocumentModel file = createFileWithBlob("/", blob, "dummy-file");
        assertNotNull(file);
        return file;
    }

    protected DocumentModel createFileWithBlob(String parentPath, Blob blob, String name) {
        DocumentModel file = session.createDocumentModel(parentPath, name, "File");
        BlobHolder bh = file.getAdapter(BlobHolder.class);
        bh.setBlob(blob);
        file = session.createDocument(file);
        return file;
    }

    protected Blob createTextBlob(String content, String filename) {
        Blob blob = Blobs.createBlob(content);
        blob.setFilename(filename);
        return blob;
    }

    protected DocumentModel createFolderWithChildren() {
        DocumentModel folder = session.createDocumentModel("/", "dummy", "Folder");
        folder = session.createDocument(folder);
        ACP acp = new ACPImpl();
        ACL acl = ACPImpl.newACL(ACL.LOCAL_ACL);
        acl.add(new ACE("toto", SecurityConstants.READ, true));
        acp.addACL(acl);
        session.setACP(folder.getRef(), acp, true);

        DocumentModel file1 = createFileWithBlob("/dummy", createTextBlob("Dummy1 text", "dummy1.txt"), "dummy1-file");
        DocumentModel file2 = createFileWithBlob("/dummy", createTextBlob("Dummy2 text", "dummy2.txt"), "dummy2-file");
        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
        TransactionHelper.startTransaction();
        folder = session.getDocument(folder.getRef());
        return folder;
    }


    @Test
    public void testRenderAProxyDocument() {
        DocumentModel file = createBlobFile();

        DocumentModel proxy = session.createProxy(file.getRef(), new PathRef("/"));
        DocumentRef renditionRef = renditionService.storeRendition(proxy, PDF_RENDITION_DEFINITION);
        DocumentModel rendition = session.getDocument(renditionRef);
        assertTrue(rendition.isVersion());
        assertEquals(null, rendition.getParentRef()); // placeless
    }

    @Test
    public void shouldNotCreateANewVersionForACheckedInDocument() {
        DocumentModel file = createBlobFile();

        DocumentRef versionRef = file.checkIn(VersioningOption.MINOR, null);
        file.refresh(DocumentModel.REFRESH_STATE, null);
        DocumentModel version = session.getDocument(versionRef);

        DocumentRef renditionDocumentRef = renditionService.storeRendition(version, "pdf");
        DocumentModel renditionDocument = session.getDocument(renditionDocumentRef);

        assertEquals(version.getId(), renditionDocument.getPropertyValue(RENDITION_SOURCE_ID_PROPERTY));

        List<DocumentModel> versions = session.getVersions(file.getRef());
        assertFalse(versions.isEmpty());
        assertEquals(1, versions.size());

        DocumentModel lastVersion = session.getLastDocumentVersion(file.getRef());
        assertEquals(version.getRef(), lastVersion.getRef());
    }

    @Test
    public void shouldNotRenderAnEmptyDocument() {
        DocumentModel file = session.createDocumentModel("/", "dummy", "File");
        file = session.createDocument(file);
        try {
            renditionService.storeRendition(file, PDF_RENDITION_DEFINITION);
            fail();
        } catch (NuxeoException e) {
            assertTrue(e.getMessage(), e.getMessage().startsWith("Rendition pdf not available"));
        }
    }

    @Test
    public void shouldNotRenderWithAnUndefinedRenditionDefinition() {
        DocumentModel file = session.createDocumentModel("/", "dummy", "File");
        file = session.createDocument(file);
        try {
            renditionService.storeRendition(file, "undefinedRenditionDefinition");
            fail();
        } catch (NuxeoException e) {
            assertEquals(e.getMessage(), "The rendition definition 'undefinedRenditionDefinition' is not registered");
        }
    }

    @Test
    public void shouldNotRenderWithAnUndefinedOperationChain() {
        DocumentModel file = session.createDocumentModel("/", "dummy", "File");
        file = session.createDocument(file);
        try {
            renditionService.storeRendition(file, "renditionDefinitionWithUnknownOperationChain");
            fail();
        } catch (NuxeoException e) {
            assertTrue(e.getMessage(),
                    e.getMessage().startsWith("Rendition renditionDefinitionWithUnknownOperationChain not available"));
        }
    }

    @Test
    public void shouldRenderOnFolder() throws Exception {
        DocumentModel folder = session.createDocumentModel("/", "dummy", "Folder");
        folder = session.createDocument(folder);
        Rendition rendition = renditionService.getRendition(folder, "renditionDefinitionWithCustomOperationChain");
        assertNotNull(rendition);
        assertNotNull(rendition.getBlob());
        assertEquals(rendition.getBlob().getString(), "dummy");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldRemoveFilesBlobsOnARendition() {
        DocumentModel fileDocument = createBlobFile();

        Blob firstAttachedBlob = createTextBlob("first attached blob", "first");
        Blob secondAttachedBlob = createTextBlob("second attached blob", "second");

        List<Map<String, Serializable>> files = new ArrayList<Map<String, Serializable>>();
        Map<String, Serializable> file = new HashMap<String, Serializable>();
        file.put("file", (Serializable) firstAttachedBlob);
        file.put("filename", firstAttachedBlob.getFilename());
        files.add(file);
        file = new HashMap<String, Serializable>();
        file.put("file", (Serializable) secondAttachedBlob);
        file.put("filename", secondAttachedBlob.getFilename());
        files.add(file);

        fileDocument.setPropertyValue(FILES_FILES_PROPERTY, (Serializable) files);

        DocumentRef renditionDocumentRef = renditionService.storeRendition(fileDocument, PDF_RENDITION_DEFINITION);
        DocumentModel renditionDocument = session.getDocument(renditionDocumentRef);

        BlobHolder bh = renditionDocument.getAdapter(BlobHolder.class);
        Blob renditionBlob = bh.getBlob();
        assertNotNull(renditionBlob);
        assertEquals("application/pdf", renditionBlob.getMimeType());
        List<Map<String, Serializable>> renditionFiles = (List<Map<String, Serializable>>) renditionDocument.getPropertyValue(FILES_FILES_PROPERTY);
        assertTrue(renditionFiles.isEmpty());
    }

    @Test
    public void shouldNotRenderADocumentWithoutBlobHolder() {
        DocumentModel folder = session.createDocumentModel("/", "dummy-folder", "Folder");
        folder = session.createDocument(folder);
        try {
            renditionService.storeRendition(folder, PDF_RENDITION_DEFINITION);
            fail();
        } catch (NuxeoException e) {
            assertTrue(e.getMessage(),
                    e.getMessage().startsWith("Rendition pdf not available"));
        }
    }

    @Test
    public void shouldNotStoreRenditionByDefault() {
        DocumentModel folder = createFolderWithChildren();
        String renditionName = ZIP_TREE_EXPORT_RENDITION_DEFINITION;
        Rendition rendition = renditionService.getRendition(folder, renditionName);
        assertNotNull(rendition);
        assertFalse(rendition.isStored());
    }

    @Test
    public void shouldStoreRenditionByDefault() {
        DocumentModel folder = createFolderWithChildren();
        String renditionName = ZIP_TREE_EXPORT_RENDITION_DEFINITION + "Lazily";
        Rendition rendition = renditionService.getRendition(folder, renditionName);
        assertNotNull(rendition);
        assertFalse(rendition.isStored());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted", e);
        }
        eventService.waitForAsyncCompletion(5000);
        rendition = renditionService.getRendition(folder, renditionName);
        assertNotNull(rendition);
        assertTrue(rendition.isStored());
    }

    @Test
    public void shouldFilterRenditionDefinitions() throws Exception {
        runtimeHarness.deployContrib(RENDITION_CORE, RENDITION_FILTERS_COMPONENT_LOCATION);

        DocumentModel doc = session.createDocumentModel("/", "note", "Note");
        doc = session.createDocument(doc);
        List<RenditionDefinition> availableRenditionDefinitions = renditionService.getAvailableRenditionDefinitions(doc);
        assertEquals(7, availableRenditionDefinitions.size());

        doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        availableRenditionDefinitions = renditionService.getAvailableRenditionDefinitions(doc);
        assertEquals(7, availableRenditionDefinitions.size());

        doc.setPropertyValue("dc:rights", "Unauthorized");
        session.saveDocument(doc);
        availableRenditionDefinitions = renditionService.getAvailableRenditionDefinitions(doc);
        assertEquals(6, availableRenditionDefinitions.size());

        runtimeHarness.undeployContrib(RENDITION_CORE, RENDITION_FILTERS_COMPONENT_LOCATION);
    }

    @Test
    public void shouldFilterRenditionDefinitionProviders() throws Exception {
        runtimeHarness.deployContrib(RENDITION_CORE, RENDITION_DEFINITION_PROVIDERS_COMPONENT_LOCATION);

        DocumentModel doc = session.createDocumentModel("/", "note", "Note");
        doc = session.createDocument(doc);
        List<RenditionDefinition> availableRenditionDefinitions = renditionService.getAvailableRenditionDefinitions(doc);
        assertEquals(8, availableRenditionDefinitions.size());

        doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        availableRenditionDefinitions = renditionService.getAvailableRenditionDefinitions(doc);
        assertEquals(8, availableRenditionDefinitions.size());

        doc.setPropertyValue("dc:rights", "Unauthorized");
        session.saveDocument(doc);
        availableRenditionDefinitions = renditionService.getAvailableRenditionDefinitions(doc);
        assertEquals(6, availableRenditionDefinitions.size());

        runtimeHarness.undeployContrib(RENDITION_CORE, RENDITION_DEFINITION_PROVIDERS_COMPONENT_LOCATION);
    }

}
