/*
 * (C) Copyright 2010-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.StorageConfiguration;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.impl.LazyRendition;
import org.nuxeo.ecm.platform.rendition.lazy.AbstractRenditionBuilderWork;
import org.nuxeo.ecm.platform.usermanager.UserManager;
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
@LocalDeploy({ "org.nuxeo.ecm.platform.rendition.core:test-rendition-contrib.xml",
        "org.nuxeo.ecm.platform.rendition.core:test-lazy-rendition-contrib.xml" })
public class TestRenditionService {

    public static final String RENDITION_CORE = "org.nuxeo.ecm.platform.rendition.core";

    private static final String RENDITION_FILTERS_COMPONENT_LOCATION = "test-rendition-filters-contrib.xml";

    private static final String RENDITION_DEFINITION_PROVIDERS_COMPONENT_LOCATION = "test-rendition-definition-providers-contrib.xml";

    private static final String RENDITION_WORKMANAGER_COMPONENT_LOCATION = "test-rendition-multithreads-workmanager-contrib.xml";

    public static final String PDF_RENDITION_DEFINITION = "pdf";

    public static final String ZIP_TREE_EXPORT_RENDITION_DEFINITION = "zipTreeExport";

    public static CyclicBarrier[] CYCLIC_BARRIERS = new CyclicBarrier[] {
            new CyclicBarrier(2), new CyclicBarrier(2), new CyclicBarrier(2)};

    public static final String CYCLIC_BARRIER_DESCRIPTION = "cyclicBarrierDesc";

    public static final Log log = LogFactory.getLog(TestRenditionService.class);

    @Inject
    protected RuntimeHarness runtimeHarness;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected EventService eventService;

    @Inject
    protected WorkManager works;

    @Inject
    protected RenditionService renditionService;

    @Test
    public void serviceRegistration() {
        assertNotNull(renditionService);
    }

    @Test
    public void testDeclaredRenditionDefinitions() {
        List<RenditionDefinition> renditionDefinitions = renditionService.getDeclaredRenditionDefinitions();
        assertRenditionDefinitions(renditionDefinitions, PDF_RENDITION_DEFINITION,
                "renditionDefinitionWithUnknownOperationChain", "zipExport", "zipTreeExport", "zipTreeExportLazily");

        RenditionDefinition rd = renditionDefinitions.stream()
                                                     .filter(renditionDefinition -> PDF_RENDITION_DEFINITION.equals(
                                                             renditionDefinition.getName()))
                                                     .findFirst()
                                                     .get();
        assertNotNull(rd);
        assertEquals(PDF_RENDITION_DEFINITION, rd.getName());
        assertEquals("blobToPDF", rd.getOperationChain());
        assertEquals("label.rendition.pdf", rd.getLabel());
        assertTrue(rd.isEnabled());

        rd = renditionDefinitions.stream()
                                 .filter(renditionDefinition -> "renditionDefinitionWithCustomOperationChain".equals(
                                         renditionDefinition.getName()))
                                 .findFirst()
                                 .get();
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
        int availableRenditionDefinitionCount = renditionDefinitions.size();
        assertTrue(availableRenditionDefinitionCount > 0);

        // add a blob
        Blob blob = Blobs.createBlob("I am a Blob");
        file.setPropertyValue("file:content", (Serializable) blob);
        file = session.saveDocument(file);

        // rendition should be available now
        renditionDefinitions = renditionService.getAvailableRenditionDefinitions(file);
        assertEquals(availableRenditionDefinitionCount + 1, renditionDefinitions.size());

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

        // update the source Document
        file.setPropertyValue("dc:description", "I have been updated again");
        file = session.saveDocument(file);
        assertEquals("0.2+", file.getVersionLabel());

        // needed for MySQL otherwise version order could be random
        coreFeature.getStorageConfiguration().maybeSleepToNextSecond();

        // now store rendition for version 0.3
        rendition = renditionService.getRendition(file, PDF_RENDITION_DEFINITION, true);
        assertEquals("0.3", rendition.getHostDocument().getVersionLabel());
        assertTrue(rendition.isStored());

        assertTrue(rendition.getHostDocument().isVersion());

    }

    @Test
    public void doErrorRendition() {
        DocumentModel file = createBlobFile();
        session.save();
        nextTransaction();

        String renditionName = "delayedErrorAutomationRendition";
        Rendition rendition = renditionService.getRendition(file, renditionName);
        assertNotNull(rendition);
        try {
            rendition.getBlob();
            fail();
        } catch (NuxeoException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("DelayedError"));
        }
    }

    @Test
    public void doErrorLazyRendition() throws Exception {
        DocumentModel file = createBlobFile();
        Calendar issued = new GregorianCalendar(2010, Calendar.OCTOBER, 10, 10, 10, 10);
        file.setPropertyValue("dc:issued", (Serializable) issued.clone());
        session.saveDocument(file);
        session.save();
        nextTransaction();

        String renditionName = "lazyDelayedErrorAutomationRendition";
        for (int i = 0; i < 2; i++) {
            boolean store = i == 1;
            if (store) {
                issued.add(Calendar.SECOND, 10);
                file.setPropertyValue("dc:issued", (Serializable) issued.clone());
                session.saveDocument(file);
                session.save();
                nextTransaction();
            }

            for (int j = 0; j < 3; j++) {
                boolean empty = j != 1;
                Rendition rendition = renditionService.getRendition(file, renditionName, store);
                assertNotNull(rendition);
                Blob blob = rendition.getBlob();
                assertEquals(0, blob.getLength());
                String mimeType = blob.getMimeType();
                String marker = (empty ? LazyRendition.EMPTY_MARKER : LazyRendition.ERROR_MARKER);
                String falseMarker = (!empty ? LazyRendition.EMPTY_MARKER : LazyRendition.ERROR_MARKER);
                String markerMsg = String.format("mimeType: %s should contain %s (i=%s,j=%s)", mimeType, marker, i, j);
                String falseMarkerMsg = String.format(
                        "mimeType: %s should not contain %s (i=$s,j=%s)", mimeType, falseMarker, i, j);
                assertTrue(markerMsg, mimeType.contains(marker));
                assertFalse(falseMarkerMsg, mimeType.contains(falseMarker));
                nextTransaction();
            }
        }
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

        // now get a different rendition as a different user
        NuxeoPrincipal totoPrincipal = Framework.getService(UserManager.class).getPrincipal("toto");
        try (CoreSession userSession = coreFeature.openCoreSession(totoPrincipal)) {
            folder = userSession.getDocument(folder.getRef());
            Rendition totoRendition = getRendition(folder, renditionName, true, isLazy);
            assertTrue(totoRendition.isStored());
            assertNotEquals(renditionDocument.getRef(), totoRendition.getHostDocument().getRef());

            // verify Administrator's rendition is larger than user's rendition
            assertNotEquals(rendition.getHostDocument().getRef(), totoRendition.getHostDocument().getRef());
            long adminZipEntryCount = countZipEntries(new ZipInputStream(rendition.getBlob().getStream()));
            long totoZipEntryCount = countZipEntries(new ZipInputStream(totoRendition.getBlob().getStream()));
            assertTrue(
                    String.format("Admin rendition entry count %s should be greater than user rendition entry count %s",
                            adminZipEntryCount, totoZipEntryCount),
                    adminZipEntryCount > totoZipEntryCount);
        }

        coreFeature.getStorageConfiguration().maybeSleepToNextSecond();

        // now "update" the folder
        folder = session.getDocument(folder.getRef());
        folder.setPropertyValue("dc:description", "I have been updated");
        folder = session.saveDocument(folder);
        session.save();
        nextTransaction();

        folder = session.getDocument(folder.getRef());
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
        DocumentModel file = createDocumentWithBlob("/", blob, "dummy-file", "File");
        assertNotNull(file);
        return file;
    }

    protected DocumentModel createDocumentWithBlob(String parentPath, Blob blob, String name, String typeName) {
        DocumentModel doc = session.createDocumentModel(parentPath, name, typeName);
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        bh.setBlob(blob);
        doc = session.createDocument(doc);
        return doc;
    }

    protected Blob createTextBlob(String content, String filename) {
        Blob blob = Blobs.createBlob(content);
        blob.setFilename(filename);
        return blob;
    }

    protected DocumentModel createFolderWithChildren() {
        DocumentModel root = session.getRootDocument();
        ACP acp = session.getACP(root.getRef());
        ACL existingACL = acp.getOrCreateACL();
        existingACL.clear();
        existingACL.add(new ACE("Administrator", SecurityConstants.EVERYTHING, true));
        existingACL.add(new ACE("group_1", SecurityConstants.READ, true));
        acp.addACL(existingACL);
        session.setACP(root.getRef(), acp, true);

        DocumentModel folder = session.createDocumentModel(root.getPathAsString(), "dummy", "Folder");
        folder = session.createDocument(folder);
        session.save();

        for (int i = 1; i <= 2; i++) {
            String childFolderName = "childFolder" + i;
            DocumentModel childFolder = session.createDocumentModel(folder.getPathAsString(), childFolderName,
                    "Folder");
            childFolder = session.createDocument(childFolder);
            if (i == 1) {
                acp = new ACPImpl();
                ACL acl = new ACLImpl();
                acl.add(new ACE("Administrator", SecurityConstants.EVERYTHING, true));
                acl.add(ACE.BLOCK);
                acp.addACL(acl);
                childFolder.setACP(acp, true);
                session.save();
            }

            DocumentModel doc1 = createDocumentWithBlob(childFolder.getPathAsString(),
                    createTextBlob("Dummy1 text", "dummy1.txt"), "dummy1-file", "File");
            DocumentModel doc2 = createDocumentWithBlob(childFolder.getPathAsString(),
                    createTextBlob("Dummy2 text", "dummy2.txt"), "dummy2-file", "File");
        }

        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
        TransactionHelper.startTransaction();
        folder = session.getDocument(folder.getRef());
        return folder;
    }

    @Test
    public void shouldPreventAdminFromReusingOthersNonVersionedStoredRendition() throws Exception {
        DocumentModel folder = createFolderWithChildren();
        String renditionName = ZIP_TREE_EXPORT_RENDITION_DEFINITION;
        Rendition totoRendition;

        // get rendition as non-admin user 'toto'
        NuxeoPrincipal totoPrincipal = Framework.getService(UserManager.class).getPrincipal("toto");
        try (CoreSession userSession = coreFeature.openCoreSession(totoPrincipal)) {
            folder = userSession.getDocument(folder.getRef());
            totoRendition = renditionService.getRendition(folder, renditionName, true);
            assertTrue(totoRendition.isStored());
        }

        nextTransaction();
        eventService.waitForAsyncCompletion();

        coreFeature.getStorageConfiguration().maybeSleepToNextSecond();

        // now get rendition as admin user 'Administrator'
        folder = session.getDocument(folder.getRef());
        Rendition rendition = renditionService.getRendition(folder, renditionName, true);
        assertTrue(rendition.isStored());
        assertTrue(rendition.isCompleted());

        // verify Administrator's rendition is different from user's rendition, is larger than user's rendition,
        // and was created later
        assertNotEquals(rendition.getHostDocument().getRef(), totoRendition.getHostDocument().getRef());
        long adminZipEntryCount = countZipEntries(new ZipInputStream(rendition.getBlob().getStream()));
        long totoZipEntryCount = countZipEntries(new ZipInputStream(totoRendition.getBlob().getStream()));
        assertTrue(String.format("Admin rendition entry count %s should be greater than user rendition entry count %s",
                adminZipEntryCount, totoZipEntryCount), adminZipEntryCount > totoZipEntryCount);
        Calendar adminModificationDate = rendition.getModificationDate();
        Calendar totoModificationDate = totoRendition.getModificationDate();
        assertTrue(
                String.format("Admin rendition modif date %s should be after user rendition modif date %s",
                        adminModificationDate.toInstant(), totoModificationDate.toInstant()),
                adminModificationDate.after(totoModificationDate));
    }

    private long countZipEntries(ZipInputStream zis) throws IOException {
        int entryCount = 0;
        while (zis.getNextEntry() != null) {
            entryCount++;
        }
        return entryCount;
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

        List<Map<String, Serializable>> files = new ArrayList<>();
        Map<String, Serializable> file = new HashMap<>();
        file.put("file", (Serializable) firstAttachedBlob);
        files.add(file);
        file = new HashMap<>();
        file.put("file", (Serializable) secondAttachedBlob);
        files.add(file);

        fileDocument.setPropertyValue(FILES_FILES_PROPERTY, (Serializable) files);

        DocumentRef renditionDocumentRef = renditionService.storeRendition(fileDocument, PDF_RENDITION_DEFINITION);
        DocumentModel renditionDocument = session.getDocument(renditionDocumentRef);

        BlobHolder bh = renditionDocument.getAdapter(BlobHolder.class);
        Blob renditionBlob = bh.getBlob();
        assertNotNull(renditionBlob);
        assertEquals("application/pdf", renditionBlob.getMimeType());
        List<Map<String, Serializable>> renditionFiles = (List<Map<String, Serializable>>) renditionDocument.getPropertyValue(
                FILES_FILES_PROPERTY);
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
            assertTrue(e.getMessage(), e.getMessage().startsWith("Rendition pdf not available"));
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
    public void shouldStoreLatestNonVersionedRendition() throws Exception {
        runtimeHarness.deployContrib(RENDITION_CORE, RENDITION_WORKMANAGER_COMPONENT_LOCATION);

        final StorageConfiguration storageConfiguration = coreFeature.getStorageConfiguration();
        final String repositoryName = session.getRepositoryName();
        final String username = session.getPrincipal().getName();
        final String renditionName = "renditionDefinitionWithCustomOperationChain";
        final String sourceDocumentModificationDatePropertyName = "dc:issued";
        DocumentModel folder = session.createDocumentModel("/", "dummy", "Folder");
        folder.setPropertyValue(sourceDocumentModificationDatePropertyName, Calendar.getInstance());
        folder = session.createDocument(folder);
        session.save();
        nextTransaction();
        eventService.waitForAsyncCompletion();

        folder = session.getDocument(folder.getRef());
        final String folderId = folder.getId();

        RenditionThread t1 = new RenditionThread(storageConfiguration, repositoryName, username, folderId,
                renditionName, true);
        RenditionThread t2 = new RenditionThread(storageConfiguration, repositoryName, username, folderId,
                renditionName, false);
        t1.start();
        t2.start();

        // Sync #1
        RenditionThread.cyclicBarrier.await();

        // now "update" the folder description
        Calendar modificationDate = Calendar.getInstance();
        String desc = "I have been updated";
        folder = session.getDocument(folder.getRef());
        folder.setPropertyValue("dc:description", desc);
        folder.setPropertyValue(sourceDocumentModificationDatePropertyName, modificationDate);
        folder = session.saveDocument(folder);

        session.save();
        nextTransaction();
        eventService.waitForAsyncCompletion();

        // Sync #2
        RenditionThread.cyclicBarrier.await();

        // Sync #3
        RenditionThread.cyclicBarrier.await();

        t1.join();
        t2.join();

        nextTransaction();
        eventService.waitForAsyncCompletion();

        // get the "updated" folder rendition
        Rendition rendition = renditionService.getRendition(folder, renditionName, true);
        assertNotNull(rendition);
        assertTrue(rendition.isStored());
        Calendar cal = rendition.getModificationDate();
        assertTrue(!cal.before(modificationDate));
        assertNotNull(rendition.getBlob());
        assertTrue(rendition.getBlob().getString().contains(desc));

        // verify the thread renditions
        StorageConfiguration storageConfig = coreFeature.getStorageConfiguration();
        List<Rendition> renditions = Arrays.asList(t1.getDetachedRendition(), t2.getDetachedRendition());
        for (Rendition rend : renditions) {
            assertNotNull(rend);
            assertTrue(rend.isStored());
            assertFalse(cal.before(rend.getModificationDate()));
            assertNotNull(rend.getBlob());
            assertTrue(rendition.getBlob().getString().contains(desc));
        }

        runtimeHarness.undeployContrib(RENDITION_CORE, RENDITION_WORKMANAGER_COMPONENT_LOCATION);
    }

    protected static class RenditionThread extends Thread {

        public static final CyclicBarrier cyclicBarrier = new CyclicBarrier(3);

        private final StorageConfiguration storageConfiguration;

        private final String repositoryName;

        private final String username;

        private final String docId;

        private final String renditionName;

        private final boolean delayed;

        private Rendition detachedRendition;

        public RenditionThread(StorageConfiguration storageConfiguration, String repositoryName, String username,
                String docId, String renditionName, boolean delayed) {
            super();
            this.storageConfiguration = storageConfiguration;
            this.repositoryName = repositoryName;
            this.username = username;
            this.docId = docId;
            this.renditionName = renditionName;
            this.delayed = delayed;
        }

        @Override
        public void run() {
            TransactionHelper.startTransaction();
            try {
                try (CoreSession session = CoreInstance.openCoreSession(repositoryName, username)) {
                    DocumentModel doc = session.getDocument(new IdRef(docId));

                    doc.putContextData("delayed", Boolean.valueOf(delayed));

                    RenditionService renditionService = Framework.getService(RenditionService.class);
                    detachedRendition = renditionService.getRendition(doc, renditionName, true);

                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                TransactionHelper.commitOrRollbackTransaction();
                storageConfiguration.maybeSleepToNextSecond();
            }
            if (!delayed) {
                try {

                    // Not-Delayed Sync #3
                    RenditionThread.cyclicBarrier.await();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public Rendition getDetachedRendition() {
            return detachedRendition;
        }

    }

    @Inject
    TransactionalFeature txFeature;

    protected void nextTransaction() {
        txFeature.nextTransaction();
    }

    @Test
    public void shouldNotScheduleRedundantLazyRenditionBuilderWorks() throws Exception {
        final String renditionName = "lazyAutomation";
        final String sourceDocumentModificationDatePropertyName = "dc:issued";
        Calendar issued = new GregorianCalendar(2010, Calendar.OCTOBER, 10, 10, 10, 10);
        String desc = CYCLIC_BARRIER_DESCRIPTION;
        DocumentModel folder = session.createDocumentModel("/", "dummy", "Folder");
        folder.setPropertyValue("dc:title", folder.getName());
        folder.setPropertyValue("dc:description", desc);
        folder.setPropertyValue(sourceDocumentModificationDatePropertyName, (Serializable) issued.clone());
        folder = session.createDocument(folder);
        session.save();
        nextTransaction();
        eventService.waitForAsyncCompletion();

        for (int i = 0; i < 3; i++) {
            folder = session.getDocument(folder.getRef());

            Rendition rendition = renditionService.getRendition(folder, renditionName, false);
            assertNotNull(rendition);
            assertTrue(rendition.getBlob().getMimeType().contains(LazyRendition.EMPTY_MARKER));
            if (i == 0) {
                if (log.isDebugEnabled()) {
                    log.debug(DummyDocToTxt.formatLogEntry(folder.getRef(), null, desc, issued) + " before barrier 0");
                }
                CYCLIC_BARRIERS[0].await();
            }

            assertEquals(issued, folder.getPropertyValue(sourceDocumentModificationDatePropertyName));
            issued.add(Calendar.SECOND, 10);
            folder.setPropertyValue(sourceDocumentModificationDatePropertyName, (Serializable) issued.clone());
            desc = "description" + Integer.toString(i);
            folder.setPropertyValue("dc:description", desc);
            session.saveDocument(folder);
            session.save();

            if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
            }
            if (i == 0) {
                if (log.isDebugEnabled()) {
                    log.debug(DummyDocToTxt.formatLogEntry(folder.getRef(), null, desc, issued) + " before barrier 1");
                }
                CYCLIC_BARRIERS[1].await();
            }
        }

        String queueId = works.getCategoryQueueId(AbstractRenditionBuilderWork.CATEGORY);
        assertEquals(1, works.listWorkIds(queueId, Work.State.RUNNING).size());
        assertEquals(1, works.listWorkIds(queueId, Work.State.SCHEDULED).size());

        if (log.isDebugEnabled()) {
            log.debug(DummyDocToTxt.formatLogEntry(folder.getRef(), null, desc, issued) + " before barrier 2");
        }
        CYCLIC_BARRIERS[2].await();

        eventService.waitForAsyncCompletion(5000);

        folder = session.getDocument(folder.getRef());
        assertEquals(issued, folder.getPropertyValue(sourceDocumentModificationDatePropertyName));
        for (int i = 0; i < 5; i++) {
            Rendition rendition = renditionService.getRendition(folder, renditionName, false);
            assertNotNull(rendition);
            assertNotNull(rendition.getBlob());
            String mimeType = rendition.getBlob().getMimeType();
            if (mimeType != null) {
                if (mimeType.contains(LazyRendition.EMPTY_MARKER)) {
                    Thread.sleep(1000);
                    eventService.waitForAsyncCompletion(5000);
                    continue;
                } else if (mimeType.contains(LazyRendition.ERROR_MARKER)) {
                    fail("Error generating rendition for folder");
                }
            }
            String content = rendition.getBlob().getString();
            assertNotNull(content);
            assertTrue(content.contains("dummy"));
            assertNotNull(desc);
            assertTrue(content.contains(desc));
            return;
        }
        fail("Could not retrieve rendition for folder");
    }

    @Test
    public void shouldFilterRenditionDefinitions() throws Exception {
        runtimeHarness.deployContrib(RENDITION_CORE, RENDITION_FILTERS_COMPONENT_LOCATION);

        List<RenditionDefinition> availableRenditionDefinitions;
        Rendition rendition;

        // ----- Note

        DocumentModel doc = session.createDocumentModel("/", "note", "Note");
        doc = session.createDocument(doc);
        availableRenditionDefinitions = renditionService.getAvailableRenditionDefinitions(doc);
        assertRenditionDefinitions(availableRenditionDefinitions, "renditionOnlyForNote", "zipExport");

        rendition = renditionService.getRendition(doc, "renditionOnlyForNote", false);
        assertNotNull(rendition);
        // others are filtered out
        try {
            rendition = renditionService.getRendition(doc, "renditionOnlyForFile", false);
            fail();
        } catch (NuxeoException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Rendition renditionOnlyForFile cannot be used"));
        }

        // ----- File

        doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        availableRenditionDefinitions = renditionService.getAvailableRenditionDefinitions(doc);
        assertRenditionDefinitions(availableRenditionDefinitions, "renditionOnlyForFile", "zipExport");

        doc.setPropertyValue("dc:rights", "Unauthorized");
        session.saveDocument(doc);
        availableRenditionDefinitions = renditionService.getAvailableRenditionDefinitions(doc);
        // renditionOnlyForFile filtered out, unauthorized
        assertRenditionDefinitions(availableRenditionDefinitions, "zipExport");

        // ----- Folder

        doc = session.createDocumentModel("/", "folder", "Folder");
        doc = session.createDocument(doc);
        availableRenditionDefinitions = renditionService.getAvailableRenditionDefinitions(doc);
        assertRenditionDefinitions(availableRenditionDefinitions, "renditionOnlyForFolder", "zipTreeExport",
                "zipTreeExportLazily");

        runtimeHarness.undeployContrib(RENDITION_CORE, RENDITION_FILTERS_COMPONENT_LOCATION);
    }

    @Test
    public void shouldFilterRenditionDefinitionProviders() throws Exception {
        runtimeHarness.deployContrib(RENDITION_CORE, RENDITION_DEFINITION_PROVIDERS_COMPONENT_LOCATION);

        DocumentModel doc = session.createDocumentModel("/", "note", "Note");
        doc = session.createDocument(doc);
        List<RenditionDefinition> availableRenditionDefinitions = renditionService.getAvailableRenditionDefinitions(
                doc);
        assertRenditionDefinitions(availableRenditionDefinitions, "dummyRendition1", "dummyRendition2", "zipExport");

        doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        availableRenditionDefinitions = renditionService.getAvailableRenditionDefinitions(doc);
        assertRenditionDefinitions(availableRenditionDefinitions, "dummyRendition1", "dummyRendition2", "zipExport");

        doc.setPropertyValue("dc:rights", "Unauthorized");
        session.saveDocument(doc);
        availableRenditionDefinitions = renditionService.getAvailableRenditionDefinitions(doc);
        assertRenditionDefinitions(availableRenditionDefinitions, "zipExport");

        doc = session.createDocumentModel("/", "folder", "Folder");
        doc = session.createDocument(doc);
        availableRenditionDefinitions = renditionService.getAvailableRenditionDefinitions(doc);
        assertRenditionDefinitions(availableRenditionDefinitions, "dummyRendition1", "dummyRendition2", "zipTreeExport",
                "zipTreeExportLazily");

        runtimeHarness.undeployContrib(RENDITION_CORE, RENDITION_DEFINITION_PROVIDERS_COMPONENT_LOCATION);
    }

    protected static void assertRenditionDefinitions(List<RenditionDefinition> actual, String... otherExpected) {
        List<String> expected = new ArrayList<>(Arrays.asList( //
                "delayedErrorAutomationRendition", //
                "iamlazy", //
                "lazyAutomation", //
                "lazyDelayedErrorAutomationRendition", //
                "renditionDefinitionWithCustomOperationChain", //
                "xmlExport"));
        if (otherExpected != null) {
            expected.addAll(Arrays.asList(otherExpected));
            Collections.sort(expected);
        }
        assertEquals(expected, renditionNames(actual));
    }

    protected static List<String> renditionNames(List<RenditionDefinition> list) {
        return list.stream().map(RenditionDefinition::getName).sorted().collect(Collectors.toList());
    }

}
