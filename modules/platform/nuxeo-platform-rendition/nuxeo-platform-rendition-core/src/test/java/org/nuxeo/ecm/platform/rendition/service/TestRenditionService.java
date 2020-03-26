/*
 * (C) Copyright 2010-2019 Nuxeo (http://nuxeo.com/) and others.
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
import static org.nuxeo.ecm.platform.rendition.impl.LazyRendition.EMPTY_MARKER;
import static org.nuxeo.ecm.platform.rendition.impl.LazyRendition.STALE_MARKER;

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
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.core.convert.service.ConversionServiceImpl;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.StorageConfiguration;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.extension.RenditionProvider;
import org.nuxeo.ecm.platform.rendition.impl.LazyRendition;
import org.nuxeo.ecm.platform.rendition.lazy.AbstractRenditionBuilderWork;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@RunWith(FeaturesRunner.class)
@Features(RenditionFeature.class)
@Deploy("org.nuxeo.ecm.platform.rendition.core:test-rendition-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.rendition.core:test-lazy-rendition-contrib.xml")
public class TestRenditionService {

    public static final String RENDITION_CORE = "org.nuxeo.ecm.platform.rendition.core";

    private static final String RENDITION_FILTERS_COMPONENT_LOCATION = "test-rendition-filters-contrib.xml";

    private static final String RENDITION_DEFINITION_PROVIDERS_COMPONENT_LOCATION = "test-rendition-definition-providers-contrib.xml";

    private static final String RENDITION_WORKMANAGER_COMPONENT_LOCATION = "test-rendition-multithreads-workmanager-contrib.xml";

    public static final String PDF_RENDITION_DEFINITION = "pdf";

    public static final String ZIP_TREE_EXPORT_RENDITION_DEFINITION = "zipTreeExport";

    public static CyclicBarrier[] CYCLIC_BARRIERS = new CyclicBarrier[] { new CyclicBarrier(2), new CyclicBarrier(2),
            new CyclicBarrier(2) };

    public static final String CYCLIC_BARRIER_DESCRIPTION = "cyclicBarrierDesc";

    public static final Logger log = LogManager.getLogger(TestRenditionService.class);

    @Inject
    protected HotDeployer deployer;

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

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
                "renditionDefinitionWithUnknownOperationChain", "zipExport", "zipTreeExport", "zipTreeExportLazily",
                "containerDefaultRendition");

        RenditionDefinition rd = renditionDefinitions.stream()
                                                     .filter(renditionDefinition -> PDF_RENDITION_DEFINITION.equals(
                                                             renditionDefinition.getName()))
                                                     .findFirst()
                                                     .orElseThrow();
        assertNotNull(rd);
        assertEquals(PDF_RENDITION_DEFINITION, rd.getName());
        assertEquals("blobToPDF", rd.getOperationChain());
        assertEquals("label.rendition.pdf", rd.getLabel());
        assertTrue(rd.isEnabled());

        rd = renditionDefinitions.stream()
                                 .filter(renditionDefinition -> "renditionDefinitionWithCustomOperationChain".equals(
                                         renditionDefinition.getName()))
                                 .findFirst()
                                 .orElseThrow();
        assertNotNull(rd);
        assertEquals("renditionDefinitionWithCustomOperationChain", rd.getName());
        assertEquals("Dummy", rd.getOperationChain());
    }

    @Test
    public void testAvailableRenditionDefinitions() {
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
    @ConditionalIgnoreRule.Ignore(condition = ConditionalIgnoreRule.IgnoreWindows.class, cause = "NXP-26757")
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
        assertEquals("dummy.pdf", renditionBlob.getFilename());

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

    // NXP-27078
    @Test
    @ConditionalIgnoreRule.Ignore(condition = ConditionalIgnoreRule.IgnoreWindows.class, cause = "NXP-26757")
    public void doPDFRenditionOnChainedConverter() {
        // assert the markdown converter is a chained one
        ConverterDescriptor md2pdf = ConversionServiceImpl.getConverterDescriptor("md2pdf");
        assertNotNull(md2pdf);
        md2pdf.initConverter();
        assertEquals(ConverterDescriptor.CHAINED_CONVERTER_TYPE, md2pdf.getConverterType());

        DocumentModel note = session.createDocumentModel("/", "dummy-note", "Note");
        String markdown = "# H1 Heading\n" + //
                "\n" + //
                "## H2 Heading\n" + //
                "\n" + //
                "Some text here.\n" + //
                "\n" + //
                "**Some bold   text.**\n" + //
                "\n" + //
                "*Some italics.*\n" + //
                "\n" + //
                "Some bullet points:\n" + //
                "* one\n" + //
                "* two\n" + //
                "* three";
        note.setPropertyValue("note:note", markdown);
        note.setPropertyValue("note:mime_type", "text/x-web-markdown");
        note = session.createDocument(note);

        Rendition rendition = renditionService.getRendition(note, PDF_RENDITION_DEFINITION);
        assertNotNull(rendition);
        assertNotNull(rendition.getBlob());
    }

    @Test
    @ConditionalIgnoreRule.Ignore(condition = ConditionalIgnoreRule.IgnoreWindows.class, cause = "NXP-26757")
    public void doRenditionVersioning() {
        DocumentModel file = createBlobFile();

        assertEquals("project", file.getCurrentLifeCycleState());
        file.followTransition("approve");
        assertEquals("approved", file.getCurrentLifeCycleState());

        // create a version of the document
        file.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);
        file = session.saveDocument(file);
        session.save();
        txFeature.nextTransaction();
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
        assertEquals(file.getRef(), rendition.getHostDocument().getRef());

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

        // update the source Document
        file.setPropertyValue("dc:description", "I have been updated yet again");
        file = session.saveDocument(file);
        assertEquals("0.3+", file.getVersionLabel());

        // create a version of the document
        file.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);
        file = session.saveDocument(file);
        session.save();
        txFeature.nextTransaction();
        assertEquals("0.4", file.getVersionLabel());

        // update the source Document
        file.setPropertyValue("dc:description", "I have been updated a very last time");
        file = session.saveDocument(file);
        assertEquals("0.4+", file.getVersionLabel());

        // check that source doc is referenced in rendered version.
        VersionModel versionModel = new VersionModelImpl();
        versionModel.setLabel("0.4");
        rendition = renditionService.getRendition(session.getVersion(file.getId(), versionModel),
                PDF_RENDITION_DEFINITION, true);
        assertEquals(file.getId(),
                rendition.getHostDocument().getPropertyValue(RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY));

    }

    @Test
    public void doErrorRendition() {
        DocumentModel file = createBlobFile();
        session.save();
        txFeature.nextTransaction();

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
    public void doErrorLazyRendition() {
        DocumentModel file = createBlobFile();
        Calendar issued = new GregorianCalendar(2010, Calendar.OCTOBER, 10, 10, 10, 10);
        file.setPropertyValue("dc:issued", (Serializable) issued.clone());
        session.saveDocument(file);
        session.save();
        txFeature.nextTransaction();

        String renditionName = "lazyDelayedErrorAutomationRendition";

        // Check rendition in error
        checkLazyRendition(file, renditionName, false, "text/plain;empty=true");
        checkLazyRendition(file, renditionName, false, "text/plain;error=true");
        checkLazyRendition(file, renditionName, false, "text/plain;empty=true");

        issued.add(Calendar.SECOND, 10);
        file.setPropertyValue("dc:issued", (Serializable) issued.clone());
        session.saveDocument(file);
        session.save();
        txFeature.nextTransaction();

        // Check rendition in error and stale
        checkLazyRendition(file, renditionName, true, "text/plain;error=true;stale=true");
        checkLazyRendition(file, renditionName, true, "text/plain;error=true");
        checkLazyRendition(file, renditionName, true, "text/plain;empty=true");
    }

    protected void checkLazyRendition(DocumentModel doc, String renditionName, boolean store, String expectedMimeType) {
        Rendition rendition = renditionService.getRendition(doc, renditionName, store);
        assertNotNull(rendition);
        Blob blob = rendition.getBlob();
        assertEquals(0, blob.getLength());
        String mimeType = blob.getMimeType();
        assertEquals(expectedMimeType, mimeType);
        txFeature.nextTransaction();
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
        Rendition rendition = getRendition(folder, renditionName, true, isLazy, false);
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
        CoreSession userSession = coreFeature.getCoreSession(totoPrincipal);
        folder = userSession.getDocument(folder.getRef());
        Rendition totoRendition = getRendition(folder, renditionName, true, isLazy, false);
        assertTrue(totoRendition.isStored());
        assertNotEquals(renditionDocument.getRef(), totoRendition.getHostDocument().getRef());

        // verify Administrator's rendition is larger than user's rendition
        assertNotEquals(rendition.getHostDocument().getRef(), totoRendition.getHostDocument().getRef());
        long adminZipEntryCount = countZipEntries(new ZipInputStream(rendition.getBlob().getStream()));
        long totoZipEntryCount = countZipEntries(new ZipInputStream(totoRendition.getBlob().getStream()));
        assertTrue(String.format("Admin rendition entry count %s should be greater than user rendition entry count %s",
                adminZipEntryCount, totoZipEntryCount), adminZipEntryCount > totoZipEntryCount);

        coreFeature.getStorageConfiguration().maybeSleepToNextSecond();

        // wait before updating the folder to have different modification date
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        // now "update" the folder
        folder = session.getDocument(folder.getRef());
        folder.setPropertyValue("dc:description", "I have been updated");
        folder = session.saveDocument(folder);
        session.save();
        txFeature.nextTransaction();

        // expect a stale rendition
        folder = session.getDocument(folder.getRef());
        rendition = getRendition(folder, renditionName, false, isLazy, true);
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

    protected Rendition getRendition(DocumentModel doc, String renditionName, boolean store, boolean isLazy,
            boolean stale) {
        Rendition rendition = renditionService.getRendition(doc, renditionName, store);
        assertNotNull(rendition);
        if (isLazy) {
            assertFalse(rendition.isStored());
            Blob blob = rendition.getBlob();
            if (stale) {
                assertTrue(rendition.isCompleted());
                assertNotNull(rendition.getModificationDate());
                assertFalse(blob.getMimeType().contains(EMPTY_MARKER));
                assertTrue(blob.getMimeType().contains(STALE_MARKER));
                assertNotEquals(LazyRendition.IN_PROGRESS_MARKER, blob.getFilename());
                assertTrue(blob.getLength() > 0);
            } else {
                assertFalse(rendition.isCompleted());
                assertNull(rendition.getModificationDate());
                assertTrue(blob.getMimeType().contains(EMPTY_MARKER));
                assertFalse(blob.getMimeType().contains(STALE_MARKER));
                assertEquals(LazyRendition.IN_PROGRESS_MARKER, blob.getFilename());
                assertEquals(0, blob.getLength());
            }
            txFeature.nextTransaction();
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

            createDocumentWithBlob(childFolder.getPathAsString(), createTextBlob("Dummy1 text", "dummy1.txt"),
                    "dummy1-file", "File");
            createDocumentWithBlob(childFolder.getPathAsString(), createTextBlob("Dummy2 text", "dummy2.txt"),
                    "dummy2-file", "File");
        }

        session.save();
        txFeature.nextTransaction();
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
        CoreSession userSession = coreFeature.getCoreSession(totoPrincipal);
        folder = userSession.getDocument(folder.getRef());
        totoRendition = renditionService.getRendition(folder, renditionName, true);
        assertTrue(totoRendition.isStored());

        txFeature.nextTransaction();

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
    @ConditionalIgnoreRule.Ignore(condition = ConditionalIgnoreRule.IgnoreWindows.class, cause = "NXP-26757")
    public void testRenderAProxyDocument() throws IOException {
        DocumentModel file = createBlobFile();
        DocumentRef fileRef = file.getRef();

        // render a live document (reference)
        Rendition rendition = renditionService.getRendition(file, PDF_RENDITION_DEFINITION);
        String expectedRenditionContent = rendition.getBlob().getString();

        // render a proxy to a live document, lazy rendition
        DocumentRef rootRef = session.getRootDocument().getRef();
        DocumentModel proxy = session.createProxy(fileRef, rootRef);
        rendition = renditionService.getRendition(proxy, PDF_RENDITION_DEFINITION);
        DocumentModel hostDocument = rendition.getHostDocument();
        assertTrue(hostDocument.isProxy());
        assertFalse(hostDocument.isVersion());
        assertEquals(rootRef, hostDocument.getParentRef());
        assertEquals(expectedRenditionContent, rendition.getBlob().getString());

        // render a proxy to a live document, stored rendition
        rendition = renditionService.getRendition(proxy, PDF_RENDITION_DEFINITION, true);
        hostDocument = rendition.getHostDocument();
        assertFalse(hostDocument.isProxy());
        assertTrue(hostDocument.isVersion());
        assertNull(hostDocument.getParentRef()); // placeless
        assertEquals(expectedRenditionContent, rendition.getBlob().getString());

        // render a proxy to a version, lazy rendition
        session.checkOut(fileRef);
        session.checkIn(fileRef, null, null);
        DocumentModel version = session.getLastDocumentVersion(fileRef);
        proxy = session.createProxy(version.getRef(), rootRef);
        rendition = renditionService.getRendition(proxy, PDF_RENDITION_DEFINITION);
        hostDocument = rendition.getHostDocument();
        assertTrue(hostDocument.isProxy());
        assertFalse(hostDocument.isVersion());
        assertEquals(rootRef, hostDocument.getParentRef());
        assertEquals(expectedRenditionContent, rendition.getBlob().getString());

        // render a proxy to a version, stored rendition
        rendition = renditionService.getRendition(proxy, PDF_RENDITION_DEFINITION, true);
        hostDocument = rendition.getHostDocument();
        assertFalse(hostDocument.isProxy());
        assertTrue(hostDocument.isVersion());
        assertNull(hostDocument.getParentRef()); // placeless
        assertEquals(expectedRenditionContent, rendition.getBlob().getString());
    }

    @Test
    @ConditionalIgnoreRule.Ignore(condition = ConditionalIgnoreRule.IgnoreWindows.class, cause = "NXP-26757")
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
            assertEquals("The rendition definition 'undefinedRenditionDefinition' is not registered", e.getMessage());
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
        assertEquals("dummy", rendition.getBlob().getString());
    }

    @Test
    @SuppressWarnings("unchecked")
    @ConditionalIgnoreRule.Ignore(condition = ConditionalIgnoreRule.IgnoreWindows.class, cause = "NXP-26757")
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
    public void shouldNotRenderANonFolderishDocumentWithoutBlobHolder() {
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
    public void shouldRenderFolderishDocumentAsAFile() {
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        Set<String> docTypeNames = schemaManager.getDocumentTypeNamesForFacet(FacetNames.FOLDERISH);
        for (String docTypeName : docTypeNames) {
            DocumentModel folder = session.createDocumentModel("/", "dummy-folder-" + docTypeName, docTypeName);
            folder = session.createDocument(folder);
            DocumentRef renditionDocRef;
            try {
                renditionDocRef = renditionService.storeRendition(folder, ZIP_TREE_EXPORT_RENDITION_DEFINITION);
            } catch (LifeCycleException ignored) {
                log.debug("Could not create stored rendition for doc type: {}", docTypeName);
                continue;
            }
            DocumentModel renditionDocModel = session.getDocument(renditionDocRef);
            String renditionDocTypeName = renditionDocModel.getType();
            assertEquals(String.format("Folderish with docType '%s' rendered as '%s' instead of 'File'", docTypeName,
                    renditionDocTypeName), "File", renditionDocTypeName);
        }
    }

    @Test
    public void shouldNotStoreRenditionByDefault() {
        DocumentModel folder = createFolderWithChildren();
        Rendition rendition = renditionService.getRendition(folder, ZIP_TREE_EXPORT_RENDITION_DEFINITION);
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
        txFeature.nextTransaction();
        rendition = renditionService.getRendition(folder, renditionName);
        assertNotNull(rendition);
        assertTrue(rendition.isStored());
    }

    @Test
    public void shouldStoreLatestNonVersionedRendition() throws Exception {
        deployer.deploy(RENDITION_CORE + ":" + RENDITION_WORKMANAGER_COMPONENT_LOCATION);

        final StorageConfiguration storageConfiguration = coreFeature.getStorageConfiguration();
        final String repositoryName = session.getRepositoryName();
        final String username = session.getPrincipal().getName();
        final String renditionName = "renditionDefinitionWithCustomOperationChain";
        final String sourceDocumentModificationDatePropertyName = "dc:issued";
        DocumentModel folder = session.createDocumentModel("/", "dummy", "Folder");
        folder.setPropertyValue(sourceDocumentModificationDatePropertyName, Calendar.getInstance());
        folder = session.createDocument(folder);
        session.save();
        txFeature.nextTransaction();

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
        txFeature.nextTransaction();

        // Sync #2
        RenditionThread.cyclicBarrier.await();

        // Sync #3
        RenditionThread.cyclicBarrier.await();

        t1.join();
        t2.join();

        txFeature.nextTransaction();

        // get the "updated" folder rendition
        Rendition rendition = renditionService.getRendition(folder, renditionName, true);
        assertNotNull(rendition);
        assertTrue(rendition.isStored());
        Calendar cal = rendition.getModificationDate();
        assertFalse(cal.before(modificationDate));
        assertNotNull(rendition.getBlob());
        assertTrue(rendition.getBlob().getString().contains(desc));

        // verify the thread renditions
        List<Rendition> renditions = Arrays.asList(t1.getDetachedRendition(), t2.getDetachedRendition());
        for (Rendition rend : renditions) {
            assertNotNull(rend);
            assertTrue(rend.isStored());
            assertFalse(cal.before(rend.getModificationDate()));
            assertNotNull(rend.getBlob());
            assertTrue(rendition.getBlob().getString().contains(desc));
        }
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
                CoreSession session = CoreInstance.getCoreSession(repositoryName, username);
                DocumentModel doc = session.getDocument(new IdRef(docId));

                doc.putContextData("delayed", Boolean.valueOf(delayed));

                RenditionService renditionService = Framework.getService(RenditionService.class);
                detachedRendition = renditionService.getRendition(doc, renditionName, true);
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
        txFeature.nextTransaction();

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
            desc = "description" + i;
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

        if (log.isDebugEnabled()) {
            log.debug(DummyDocToTxt.formatLogEntry(folder.getRef(), null, desc, issued) + " before barrier 2");
        }
        CYCLIC_BARRIERS[2].await();

        txFeature.nextTransaction();

        folder = session.getDocument(folder.getRef());
        assertEquals(issued, folder.getPropertyValue(sourceDocumentModificationDatePropertyName));
        for (int i = 0; i < 5; i++) {
            Rendition rendition = renditionService.getRendition(folder, renditionName, false);
            assertNotNull(rendition);
            assertNotNull(rendition.getBlob());
            String mimeType = rendition.getBlob().getMimeType();
            if (mimeType != null) {
                if (mimeType.contains(LazyRendition.EMPTY_MARKER)) {
                    txFeature.nextTransaction();
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
        deployer.deploy(RENDITION_CORE + ":" + RENDITION_FILTERS_COMPONENT_LOCATION);

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
        assertRenditionDefinitions(availableRenditionDefinitions, "containerDefaultRendition", "renditionOnlyForFolder",
                "zipTreeExport", "zipTreeExportLazily");
    }

    @Test
    public void shouldFilterRenditionDefinitionProviders() throws Exception {
        deployer.deploy(RENDITION_CORE + ":" + RENDITION_DEFINITION_PROVIDERS_COMPONENT_LOCATION);

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
        assertRenditionDefinitions(availableRenditionDefinitions, "containerDefaultRendition", "dummyRendition1",
                "dummyRendition2", "zipTreeExport", "zipTreeExportLazily");
    }

    /**
     * @since 10.10
     */
    @Test
    public void testGetAvailableRenditionDefinitionByName() throws Exception {
        deployer.deploy(RENDITION_CORE + ":" + RENDITION_DEFINITION_PROVIDERS_COMPONENT_LOCATION);

        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);

        // rendition definition not registered
        try {
            renditionService.getAvailableRenditionDefinition(doc, "unknown");
            fail("Getting an unknown rendition definition should fail");
        } catch (NuxeoException e) {
            assertEquals("The rendition definition 'unknown' is not registered", e.getMessage());
        }

        // rendition definition registered directly
        RenditionDefinition renditionDefinition = renditionService.getAvailableRenditionDefinition(doc, "mainBlob");
        assertNotNull(renditionDefinition);
        assertEquals("mainBlob", renditionDefinition.getName());
        RenditionProvider renditionProvider = renditionDefinition.getProvider();
        assertNotNull(renditionProvider);
        assertEquals("DefaultAutomationRenditionProvider", renditionProvider.getClass().getSimpleName());

        // rendition definition registered through a rendition definition provider but not bound to any rendition
        // provider
        try {
            renditionService.getAvailableRenditionDefinition(doc, "dummyRendition1");
            fail("Getting a rendition definition not bound to any rendition provider should fail");
        } catch (NuxeoException e) {
            assertEquals("Rendition definition dummyRendition1 isn't bound to any rendition provider", e.getMessage());
        }

        // rendition definition registered through a rendition definition provider and bound to a rendition provider
        renditionDefinition = renditionService.getAvailableRenditionDefinition(doc, "dummyRendition2");
        assertNotNull(renditionDefinition);
        assertEquals("dummyRendition2", renditionDefinition.getName());
        renditionProvider = renditionDefinition.getProvider();
        assertNotNull(renditionProvider);
        assertEquals("DummyRenditionProvider", renditionProvider.getClass().getSimpleName());
    }

    /**
     * @since 10.3
     */
    @Test
    @ConditionalIgnoreRule.Ignore(condition = ConditionalIgnoreRule.IgnoreWindows.class, cause = "NXP-26757")
    public void shouldNonAdminPublishRendition() {
        DocumentModel file = createBlobFile();
        DocumentModel section = session.createDocumentModel("/", "section", "Section");
        section = session.createDocument(section);

        ACP acp = new ACPImpl();
        ACL acl = new ACLImpl();
        acl.add(ACE.builder("toto", "Write").creator("Administrator").build());
        acl.add(ACE.builder("toto", "Read").creator("Administrator").build());
        acl.add(ACE.builder("pouet", "Read").creator("Administrator").build());
        acp.addACL(acl);
        file.setACP(acp, true);
        section.setACP(acp, true);
        session.save();

        CoreSession totoSession = coreFeature.getCoreSession("toto");
        file = totoSession.getDocument(file.getRef());
        section = totoSession.getDocument(section.getRef());
        DocumentModel publishedRendition = renditionService.publishRendition(file, section, PDF_RENDITION_DEFINITION,
                false);
        assertNotNull(publishedRendition);
        assertTrue(publishedRendition.isProxy());
        assertEquals(section.getRef(), publishedRendition.getParentRef());

        CoreSession pouetSession = coreFeature.getCoreSession("pouet");
        file = pouetSession.getDocument(file.getRef());
        section = pouetSession.getDocument(section.getRef());
        try {
            renditionService.publishRendition(file, section, PDF_RENDITION_DEFINITION, false);
            fail("User should not have permission to publish");
        } catch (DocumentSecurityException e) {
            // Expected
        }
    }

    protected static void assertRenditionDefinitions(List<RenditionDefinition> actual, String... otherExpected) {
        List<String> expected = new ArrayList<>(Arrays.asList( //
                "delayedErrorAutomationRendition", //
                "iamlazy", //
                "mainBlob", //
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
