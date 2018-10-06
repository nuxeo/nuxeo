/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.dublincore;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener.DISABLE_DUBLINCORE_LISTENER;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.CoreSession.CopyOption;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.StorageConfiguration;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * DublinCoreStorage Test Case.
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, ClientLoginFeature.class })
@Deploy("org.nuxeo.ecm.platform.dublincore")
@Deploy("org.nuxeo.ecm.platform.dublincore.tests:OSGI-INF/types-contrib.xml")
public class TestDublinCoreStorage {

    @Inject
    protected CoreFeature feature;

    @Inject
    protected CoreSession session;

    @Inject
    protected HotDeployer deployer;

    protected StorageConfiguration storageConfiguration;

    @Before
    public void before() {
        storageConfiguration = feature.getStorageConfiguration();
    }

    @Test
    public void testCreationDateAndCreator() {
        DocumentModel childFile = session.createDocumentModel("/", "file-007", "File");
        DocumentModel childFile2 = session.createDocument(childFile);
        assertNotNull(childFile2.getPropertyValue("dc:created"));
        assertEquals("Administrator", childFile2.getPropertyValue("dc:creator"));
    }

    @Test
    public void testModificationDate() {
        DocumentModel childFile = session.createDocumentModel("/", "file-008", "File");
        DocumentModel childFile2 = session.createDocument(childFile);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        childFile2.setProperty("dublincore", "title", "toto");

        session.saveDocument(childFile2);

        DataModel dm = childFile2.getDataModel("dublincore");
        Calendar created = (Calendar) dm.getData("created");
        assertNotNull(created);

        Calendar modified = (Calendar) childFile2.getPropertyValue("dc:modified");
        assertNotNull(modified);

        assertTrue(modified.getTime() + " !> " + created.getTime(), modified.after(created));
    }

    // Wait until we can have a real list management
    @Test
    public void testContributors() {
        DocumentModel childFile = session.createDocumentModel("/", "file-008", "File");
        DocumentModel childFile2 = session.createDocument(childFile);
        DataModel dm = childFile2.getDataModel("dublincore");

        String author = (String) dm.getData("creator");
        assertEquals("Administrator", author);

        String[] contributorsArray = (String[]) dm.getData("contributors");
        List<String> contributorsList = Arrays.asList(contributorsArray);
        assertTrue(contributorsList.contains("Administrator"));

        // modify security to test with a new user

        DocumentModel root = session.getRootDocument();
        ACP acp = root.getACP();
        ACL[] acls = acp.getACLs();
        ACL theAcl = acls[0];
        ACE ace = new ACE("Jacky", SecurityConstants.EVERYTHING, true);
        theAcl.add(ace);
        root.setACP(acp, true);

        // create a new session
        session.save();

        try (CloseableCoreSession session2 = CoreInstance.openCoreSession(session.getRepositoryName(), "Jacky")) {
            DocumentModel childFile3 = session2.getDocument(childFile2.getRef());
            childFile3.setProperty("dublincore", "source", "testing");
            childFile3 = session2.saveDocument(childFile3);

            contributorsArray = (String[]) childFile3.getDataModel("dublincore").getData("contributors");
            contributorsList = Arrays.asList(contributorsArray);
            assertTrue(contributorsList.contains("Jacky"));
            assertEquals("Administrator", childFile3.getProperty("dublincore", "creator"));
        }
    }

    @Test
    public void testLastContributor() {
        DocumentModel childFile = session.createDocumentModel("/", "file-008", "File");
        DocumentModel childFile2 = session.createDocument(childFile);
        DataModel dm = childFile2.getDataModel("dublincore");

        String lastContributor = (String) dm.getData("lastContributor");
        assertEquals("Administrator", lastContributor);

        String[] contributorsArray = (String[]) dm.getData("contributors");
        List<String> contributorsList = Arrays.asList(contributorsArray);
        assertTrue(contributorsList.contains("Administrator"));

        // modify security to test with tow new user
        DocumentModel root = session.getRootDocument();
        ACP acp = root.getACP();
        ACL[] acls = acp.getACLs();
        ACL theAcl = acls[0];
        ACE ace = new ACE("Jacky", SecurityConstants.EVERYTHING, true);
        theAcl.add(ace);
        ace = new ACE("Fredo", SecurityConstants.EVERYTHING, true);
        theAcl.add(ace);
        root.setACP(acp, true);
        session.save();

        // create a new session
        try (CloseableCoreSession session2 = CoreInstance.openCoreSession(session.getRepositoryName(), "Jacky")) {
            DocumentModel childFile3 = session2.getDocument(childFile2.getRef());
            childFile3.setProperty("dublincore", "source", "testing");
            childFile3 = session2.saveDocument(childFile3);

            contributorsArray = (String[]) childFile3.getDataModel("dublincore").getData("contributors");
            contributorsList = Arrays.asList(contributorsArray);
            assertTrue(contributorsList.contains("Jacky"));
            assertEquals(1, contributorsList.indexOf("Jacky"));
            assertEquals("Jacky", childFile3.getProperty("dublincore", "lastContributor"));
            session2.save();
        }

        // Test if a new contributor will be at the end of the list
        try (CloseableCoreSession session3 = CoreInstance.openCoreSession(session.getRepositoryName(), "Fredo")) {
            DocumentModel childFile3 = session3.getDocument(childFile2.getRef());
            childFile3.setProperty("dublincore", "source", "testing2"); // make a change
            childFile3 = session3.saveDocument(childFile3);

            contributorsArray = (String[]) childFile3.getDataModel("dublincore").getData("contributors");
            contributorsList = Arrays.asList(contributorsArray);
            assertTrue(contributorsList.contains("Fredo"));
            assertEquals("Fredo", childFile3.getProperty("dublincore", "lastContributor"));
            session3.save();
        }

        // Test if a previously contributor will be move to the end of the list
        try (CloseableCoreSession session4 = CoreInstance.openCoreSession(session.getRepositoryName(),
                "Administrator")) {
            DocumentModel childFile3 = session4.getDocument(childFile2.getRef());
            childFile3.setProperty("dublincore", "source", "testing");
            childFile3 = session4.saveDocument(childFile3);

            contributorsArray = (String[]) childFile3.getDataModel("dublincore").getData("contributors");
            contributorsList = Arrays.asList(contributorsArray);
            assertTrue(contributorsList.contains("Administrator"));
            assertEquals("Administrator", childFile3.getProperty("dublincore", "lastContributor"));
        }
    }

    @Test
    public void testLastContributorForSystemSession() {
        // use a system session with no originating user name
        try (CloseableCoreSession session2 = CoreInstance.openCoreSessionSystem(session.getRepositoryName(), (String) null)) {
            DocumentModel file = session2.createDocumentModel("/", "file", "File");
            file = session2.createDocument(file);
            // check we haven't inserted a null in last contributors
            assertEquals(Collections.singletonList(SecurityConstants.SYSTEM_USERNAME),
                    Arrays.asList((String[]) file.getPropertyValue("dc:contributors")));
        }
    }

    @Test
    public void testContributorsAndModifiedDoesNotChangeIfThereAreNoChanges() {
        DocumentModel childFile = session.createDocumentModel("/", "file-008", "File");
        childFile = session.createDocument(childFile);
        DataModel dm = childFile.getDataModel("dublincore");
        // backup the data to check
        Calendar modified = (Calendar) dm.getData("modified");
        String lastContributor = (String) dm.getData("lastContributor");
        String[] contributors = (String[]) dm.getData("contributors");
        // save the document with no changes
        childFile = session.saveDocument(childFile);
        // get the data to check
        Calendar modified2 = (Calendar) dm.getData("modified");
        String lastContributor2 = (String) dm.getData("lastContributor");
        String[] contributors2 = (String[]) dm.getData("contributors");
        assertEquals(modified, modified2);
        assertEquals(lastContributor, lastContributor2);
        assertArrayEquals(contributors, contributors2);
    }

    @Test
    public void testIssuedDate() {
        DocumentModel folder1 = session.createDocumentModel("/", "testfolder1", "Folder");
        folder1 = session.createDocument(folder1);
        DocumentModel file1 = session.createDocumentModel("/testfolder1", "testfile1", "File");
        file1 = session.createDocument(file1);
        DocumentModel proxyDoc = session.publishDocument(file1, folder1);

        getEventProducer().fireEvent(
                new DocumentEventContext(session, session.getPrincipal(), proxyDoc).newEvent("documentPublished"));

        DocumentModel version = session.getSourceDocument(proxyDoc.getRef());
        Calendar issued = (Calendar) version.getPropertyValue("dc:issued");
        assertNotNull(issued);
    }

    @Test
    public void testProxySchemas() {
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        DocumentModel proxy = session.publishDocument(doc, folder);
        session.save();

        proxy.setPropertyValue("info:info", "proxyinfo");
        proxy = session.saveDocument(proxy);
        session.save();
    }

    @Test
    public void testProxySchemasWithComplex() {
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        DocumentModel proxy = session.publishDocument(doc, folder);
        session.save();

        // read part of a non-initialized complex prop
        // should not mark it dirty which would cause problems on save
        proxy.getPropertyValue("file:content");
        // write a modifiable proxy schema
        proxy.setPropertyValue("info:info", "proxyinfo");
        proxy = session.saveDocument(proxy);
        session.save();
    }

    /* check that creating a live proxy to a doc doesn't update the doc. */
    @Test
    public void testProxyLive() throws Exception {
        // create file
        DocumentModel file = session.createDocumentModel("/", "file", "File");
        file = session.createDocument(file);
        Calendar created1 = (Calendar) file.getPropertyValue("dc:created");
        Thread.sleep(1000);

        // create proxy
        DocumentModel proxy = session.createProxy(file.getRef(), new PathRef("/"));
        proxy = session.saveDocument(proxy);

        // check file
        file = session.getDocument(file.getRef());
        Calendar created2 = (Calendar) file.getPropertyValue("dc:created");

        assertEquals(created1.getTimeInMillis(), created2.getTimeInMillis());
    }

    @Test
    public void testDisableListener() {
        DocumentModel childFile = session.createDocumentModel("/", "file-007", "File");
        childFile.setPropertyValue("dc:creator", "Bender");
        Date now = new Date();
        childFile.setPropertyValue("dc:created", now);
        childFile.putContextData(DISABLE_DUBLINCORE_LISTENER, Boolean.TRUE);
        DocumentModel childFile2 = session.createDocument(childFile);
        assertEquals(now, ((Calendar) childFile2.getPropertyValue("dc:created")).getTime());
        assertEquals("Bender", childFile2.getPropertyValue("dc:creator"));
    }

    private static EventProducer getEventProducer() {
        return Framework.getService(EventProducer.class);
    }

    @Test
    public void testCreatorForUnrestrictedSessionCreatedDoc() throws Exception {
        try (CloseableCoreSession session2 = CoreInstance.openCoreSession(session.getRepositoryName(), "Jacky")) {
            CreateDocumentUnrestricted runner = new CreateDocumentUnrestricted(session2);
            runner.runUnrestricted();
            DocumentModel doc = runner.getFolder();
            String creator = (String) doc.getPropertyValue("dc:creator");
            assertEquals("Jacky", creator);
        }
    }

    public class CreateDocumentUnrestricted extends UnrestrictedSessionRunner {

        private DocumentModel folder;

        public CreateDocumentUnrestricted(CoreSession session) {
            super(session);
        }

        @Override
        public void run() {
            folder = session.createDocumentModel("/", "testfolder1", "Folder");
            folder = session.createDocument(folder);
            folder = session.saveDocument(folder);
            folder.detach(true);
        }

        public DocumentModel getFolder() {
            return folder;
        }

    }

    @Test
    public void testCopyDocument() throws Exception {
        DocumentModel file = session.createDocument(session.createDocumentModel("/", "file-007", "File"));
        storageConfiguration.maybeSleepToNextSecond();
        DocumentModel copy = session.copy(file.getRef(), file.getParentRef(), "file-008");

        waitForAsyncCompletion();

        assertNotNull(copy);
        assertEquals(file.getPropertyValue("dc:created"), copy.getPropertyValue("dc:created"));
        assertEquals(file.getPropertyValue("dc:modified"), copy.getPropertyValue("dc:modified"));
    }

    @Test
    public void testCopyDocumentWithResetCoreMetadata() throws Exception {
        DocumentModel file = session.createDocument(session.createDocumentModel("/", "file-007", "File"));
        storageConfiguration.maybeSleepToNextSecond();
        DocumentModel copy = session.copy(file.getRef(), file.getParentRef(), "file-008", CopyOption.RESET_CREATOR);

        waitForAsyncCompletion();

        assertNotNull(copy);
        assertNotEquals(file.getPropertyValue("dc:created"), copy.getPropertyValue("dc:created"));
        assertNotEquals(file.getPropertyValue("dc:modified"), copy.getPropertyValue("dc:modified"));
    }

    @Test
    public void testCopyDocumentWithResetCoreMetadataByConfiguration() throws Exception {
        deployer.deploy("org.nuxeo.ecm.platform.dublincore.tests:OSGI-INF/reset-creator-contrib.xml");

        DocumentModel file = session.createDocument(session.createDocumentModel("/", "file-007", "File"));
        storageConfiguration.maybeSleepToNextSecond();
        DocumentModel copy = session.copy(file.getRef(), file.getParentRef(), "file-008");

        waitForAsyncCompletion();

        assertNotNull(copy);
        assertNotEquals(file.getPropertyValue("dc:created"), copy.getPropertyValue("dc:created"));
        assertNotEquals(file.getPropertyValue("dc:modified"), copy.getPropertyValue("dc:modified"));
    }

    @Test
    public void testListenerRunsAtCreationBeforeVersioning() throws Exception {
        // (Note is configured by default with auto-versioning)
        DocumentModel note = session.createDocumentModel("/", "note", "Note");
        note = session.createDocument(note);
        session.save();
        // note is checked in after auto-versioning
        assertFalse(note.isCheckedOut());
        // note's dublincore info was correctly filled in
        Calendar created = (Calendar) note.getPropertyValue("dc:created");
        assertNotNull(created);
        assertEquals("Administrator", note.getPropertyValue("lastContributor"));
        // one version exists
        List<DocumentModel> versions = session.getVersions(note.getRef());
        assertEquals(1, versions.size());
        DocumentModel version = versions.get(0);
        assertEquals("0.1", version.getVersionLabel());
        // version's dublincore info was correctly filled in
        Calendar versionCreated = (Calendar) version.getPropertyValue("dc:created");
        assertNotNull(versionCreated);
        assertEquals(created.toInstant(), versionCreated.toInstant());
        assertEquals("Administrator", version.getPropertyValue("dc:lastContributor"));
    }

    protected void waitForAsyncCompletion() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        Framework.getService(EventService.class).waitForAsyncCompletion();
    }

}
