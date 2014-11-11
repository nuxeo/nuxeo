/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.dublincore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener.DISABLE_DUBLINCORE_LISTENER;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.dublincore.service.DublinCoreStorageService;
import org.nuxeo.runtime.api.Framework;

/**
 * DublinCoreStorage Test Case.
 */
public class TestDublinCoreStorage extends SQLRepositoryTestCase {

    private DocumentModel root;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.platform.dublincore");
        deployContrib("org.nuxeo.ecm.platform.dublincore.tests",
                "OSGI-INF/types-contrib.xml");
        deployBundle("org.nuxeo.ecm.core.event");

        EventServiceAdmin eventAdmin = Framework.getService(EventServiceAdmin.class);
        eventAdmin.setBulkModeEnabled(true);
        eventAdmin.setListenerEnabledFlag("sql-storage-binary-text", false);

        openSession();
        root = session.getRootDocument();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    @Test
    public void testStorageService() {
        DublinCoreStorageService service = NXDublinCore.getDublinCoreStorageService();
        assertNotNull(service);
    }

    @Test
    public void testCreationDateAndCreator() throws ClientException {
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                "file-007", "File");
        DocumentModel childFile2 = session.createDocument(childFile);
        assertNotNull(childFile2.getPropertyValue("dc:created"));
        assertEquals("Administrator", childFile2.getPropertyValue("dc:creator"));
    }

    @Test
    public void testModificationDate() throws ClientException {
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                "file-008", "File");
        DocumentModel childFile2 = session.createDocument(childFile);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }

        childFile2.setProperty("dublincore", "title", "toto");

        session.saveDocument(childFile2);

        DataModel dm = childFile2.getDataModel("dublincore");
        Calendar created = (Calendar) dm.getData("created");
        assertNotNull(created);

        Calendar modified = (Calendar) childFile2.getPropertyValue("dc:modified");
        assertNotNull(modified);

        assertTrue(modified.getTime() + " !> " + created.getTime(),
                modified.after(created));
    }

    // Wait until we can have a real list management
    @Test
    public void testContributors() throws ClientException {
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                "file-008", "File");
        DocumentModel childFile2 = session.createDocument(childFile);
        DataModel dm = childFile2.getDataModel("dublincore");

        String author = (String) dm.getData("creator");
        assertEquals("Administrator", author);

        String[] contributorsArray = (String[]) dm.getData("contributors");
        List<String> contributorsList = Arrays.asList(contributorsArray);
        assertTrue(contributorsList.contains("Administrator"));

        // modify security to test with a new user

        ACP acp = root.getACP();
        ACL[] acls = acp.getACLs();
        ACL theAcl = acls[0];
        ACE ace = new ACE("Jacky", SecurityConstants.EVERYTHING, true);
        theAcl.add(ace);
        root.setACP(acp, true);

        // create a new session
        session.save();
        closeSession();
        session = openSessionAs("Jacky");

        DocumentModel childFile3 = session.getDocument(childFile2.getRef());
        childFile3.setProperty("dublincore", "source", "testing");
        childFile3 = session.saveDocument(childFile3);

        contributorsArray = (String[]) childFile3.getDataModel("dublincore").getData(
                "contributors");
        contributorsList = Arrays.asList(contributorsArray);
        assertTrue(contributorsList.contains("Jacky"));
        assertEquals("Administrator",
                childFile3.getProperty("dublincore", "creator"));
    }

    @Test
    public void testLastContributor() throws ClientException {
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                "file-008", "File");
        DocumentModel childFile2 = session.createDocument(childFile);
        DataModel dm = childFile2.getDataModel("dublincore");

        String lastContributor = (String) dm.getData("lastContributor");
        assertEquals("Administrator", lastContributor);

        String[] contributorsArray = (String[]) dm.getData("contributors");
        List<String> contributorsList = Arrays.asList(contributorsArray);
        assertTrue(contributorsList.contains("Administrator"));

        // modify security to test with tow new user
        ACP acp = root.getACP();
        ACL[] acls = acp.getACLs();
        ACL theAcl = acls[0];
        ACE ace = new ACE("Jacky", SecurityConstants.EVERYTHING, true);
        theAcl.add(ace);
        ace = new ACE("Fredo", SecurityConstants.EVERYTHING, true);
        theAcl.add(ace);
        root.setACP(acp, true);

        // create a new session
        session.save();
        closeSession();
        session = openSessionAs("Jacky");

        DocumentModel childFile3 = session.getDocument(childFile2.getRef());
        childFile3.setProperty("dublincore", "source", "testing");
        childFile3 = session.saveDocument(childFile3);

        contributorsArray = (String[]) childFile3.getDataModel("dublincore").getData(
                "contributors");
        contributorsList = Arrays.asList(contributorsArray);
        assertTrue(contributorsList.contains("Jacky"));
        assertEquals(1, contributorsList.indexOf("Jacky"));
        assertEquals("Jacky",
                childFile3.getProperty("dublincore", "lastContributor"));
        session.save();
        closeSession();

        // Test if a new contributor will be at the end of the list
        session = openSessionAs("Fredo");

        childFile3 = session.getDocument(childFile2.getRef());
        childFile3.setProperty("dublincore", "source", "testing");
        childFile3 = session.saveDocument(childFile3);

        contributorsArray = (String[]) childFile3.getDataModel("dublincore").getData(
                "contributors");
        contributorsList = Arrays.asList(contributorsArray);
        assertTrue(contributorsList.contains("Fredo"));
        assertEquals("Fredo",
                childFile3.getProperty("dublincore", "lastContributor"));
        session.save();
        closeSession();

        // Test if a previously contributor will be move to the end of the list
        session = openSessionAs("Administrator");

        childFile3 = session.getDocument(childFile2.getRef());
        childFile3.setProperty("dublincore", "source", "testing");
        childFile3 = session.saveDocument(childFile3);

        contributorsArray = (String[]) childFile3.getDataModel("dublincore").getData(
                "contributors");
        contributorsList = Arrays.asList(contributorsArray);
        assertTrue(contributorsList.contains("Administrator"));
        assertEquals("Administrator",
                childFile3.getProperty("dublincore", "lastContributor"));
    }

    @Test
    public void testIssuedDate() throws ClientException {
        DocumentModel folder1 = new DocumentModelImpl("/", "testfolder1",
                "Folder");
        folder1 = session.createDocument(folder1);
        DocumentModel file1 = new DocumentModelImpl("/testfolder1",
                "testfile1", "File");
        file1 = session.createDocument(file1);
        DocumentModel proxyDoc = session.publishDocument(file1, folder1);

        getEventProducer().fireEvent(
                new DocumentEventContext(session, session.getPrincipal(),
                        proxyDoc).newEvent("documentPublished"));

        DocumentModel version = session.getSourceDocument(proxyDoc.getRef());
        Calendar issued = (Calendar) version.getPropertyValue("dc:issued");
        assertNotNull(issued);
    }

    @Test
    public void testProxySchemas() throws ClientException {
        DocumentModel folder = new DocumentModelImpl("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = new DocumentModelImpl("/", "file", "File");
        doc = session.createDocument(doc);
        DocumentModel proxy = session.publishDocument(doc, folder);
        session.save();

        proxy.setPropertyValue("info:info", "proxyinfo");
        proxy = session.saveDocument(proxy);
        session.save();
    }

    @Test
    public void testProxySchemasWithComplex() throws ClientException {
        DocumentModel folder = new DocumentModelImpl("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = new DocumentModelImpl("/", "file", "File");
        doc = session.createDocument(doc);
        DocumentModel proxy = session.publishDocument(doc, folder);
        session.save();

        // read part of a non-initialized complex prop
        // should not mark it dirty which would cause problems on save
        proxy.getPropertyValue("file:filename");
        // write a modifiable proxy schema
        proxy.setPropertyValue("info:info", "proxyinfo");
        proxy = session.saveDocument(proxy);
        session.save();
    }

    @Test
    public void testDisableListener() throws ClientException {
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                "file-007", "File");
        childFile.setPropertyValue("dc:creator", "Bender");
        Date now = new Date();
        childFile.setPropertyValue("dc:created", now);
        childFile.putContextData(DISABLE_DUBLINCORE_LISTENER, Boolean.TRUE);
        DocumentModel childFile2 = session.createDocument(childFile);
        assertEquals(
                now,
                ((Calendar) childFile2.getPropertyValue("dc:created")).getTime());
        assertEquals("Bender", childFile2.getPropertyValue("dc:creator"));
    }

    private static EventProducer getEventProducer() throws ClientException {
        try {
            return Framework.getService(EventProducer.class);
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    @Test
    public void testCreatorForUnrestrictedSessionCreatedDoc() throws Exception {
        closeSession();
        session = openSessionAs("Jacky");
        CreateDocumentUnrestricted runner = new CreateDocumentUnrestricted(
                session);
        runner.runUnrestricted();
        DocumentModel doc = runner.getFolder();
        String creator = (String) doc.getPropertyValue("dc:creator");
        assertEquals("Jacky", creator);
    }

    public class CreateDocumentUnrestricted extends UnrestrictedSessionRunner {

        private DocumentModel folder;

        public CreateDocumentUnrestricted(CoreSession session) {
            super(session);
        }

        @Override
        public void run() throws ClientException {
            folder = new DocumentModelImpl("/", "testfolder1", "Folder");
            folder = session.createDocument(folder);
            session.saveDocument(folder);
        }

        public DocumentModel getFolder() {
            return folder;
        }

    }

}
