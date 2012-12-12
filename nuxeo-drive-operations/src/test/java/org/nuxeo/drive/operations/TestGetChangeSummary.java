/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *
 * Contributors:
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.service.MockChangeFinder;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.impl.FileSystemChangeSummary;
import org.nuxeo.drive.service.impl.FileSystemItemChange;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.test.RestFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;

import com.google.inject.Inject;

/**
 * Tests the {@link NuxeoDriveGetChangeSummary} operation.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features(RestFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.drive.core", "org.nuxeo.drive.operations" })
@Jetty(port = 18080)
public class TestGetChangeSummary {

    @Inject
    protected CoreSession session;

    @Inject
    protected NuxeoDriveManager nuxeoDriveManager;

    @Inject
    protected HttpAutomationClient automationClient;

    protected long lastSuccessfulSync;

    protected DocumentModel folder1;

    protected DocumentModel folder2;

    protected Session clientSession;

    protected ObjectMapper mapper;

    @Before
    public void init() throws Exception {

        nuxeoDriveManager.setChangeFinder(new MockChangeFinder());
        lastSuccessfulSync = Calendar.getInstance().getTimeInMillis();

        folder1 = session.createDocument(session.createDocumentModel("/",
                "folder1", "Folder"));
        folder2 = session.createDocument(session.createDocumentModel("/",
                "folder2", "Folder"));

        clientSession = automationClient.getSession("Administrator",
                "Administrator");
        mapper = new ObjectMapper();
    }

    @Test
    public void testGetChangesSummary() throws Exception {

        // No sync roots => shouldn't find any changes
        FileSystemChangeSummary docChangeSummary = getChangeSummary();
        assertTrue(docChangeSummary.getDocumentChanges().isEmpty());
        assertEquals("no_changes", docChangeSummary.getStatusCode());

        // Register sync roots and create 2 documents => should find 2 changes
        nuxeoDriveManager.registerSynchronizationRoot("Administrator", folder1,
                session);
        nuxeoDriveManager.registerSynchronizationRoot("Administrator", folder2,
                session);

        DocumentModel doc1 = session.createDocumentModel("/folder1", "doc1",
                "File");
        doc1.setPropertyValue("file:content", new StringBlob(
                "The content of file 1."));
        doc1 = session.createDocument(doc1);
        Thread.sleep(1000);
        DocumentModel doc2 = session.createDocumentModel("/folder2", "doc2",
                "File");
        doc2.setPropertyValue("file:content", new StringBlob(
                "The content of file 2."));
        doc2 = session.createDocument(doc2);

        session.save();

        docChangeSummary = getChangeSummary();
        Set<String> expectedSyncRootPaths = new HashSet<String>();
        expectedSyncRootPaths.add("/folder1");
        expectedSyncRootPaths.add("/folder2");
        assertEquals(expectedSyncRootPaths, docChangeSummary.getSyncRootPaths());

        List<FileSystemItemChange> docChanges = docChangeSummary.getDocumentChanges();
        assertEquals(2, docChanges.size());
        FileSystemItemChange docChange = docChanges.get(0);
        assertEquals("test", docChange.getRepositoryId());
        assertEquals("documentChanged", docChange.getEventId());
        assertEquals("project", docChange.getDocLifeCycleState());
        assertEquals("/folder2/doc2", docChange.getDocPath());
        assertEquals(doc2.getId(), docChange.getDocUuid());
        docChange = docChanges.get(1);
        assertEquals("test", docChange.getRepositoryId());
        assertEquals("documentChanged", docChange.getEventId());
        assertEquals("project", docChange.getDocLifeCycleState());
        assertEquals("/folder1/doc1", docChange.getDocPath());
        assertEquals(doc1.getId(), docChange.getDocUuid());
        assertEquals("found_changes", docChangeSummary.getStatusCode());

        // Create 2 documents in the same sync root: "/folder1" and 1 document
        // in another sync root => should find 2 changes for "/folder1"
        DocumentModel doc3 = session.createDocumentModel("/folder1", "doc3",
                "File");
        doc3.setPropertyValue("file:content", new StringBlob(
                "The content of file 3."));
        doc3 = session.createDocument(doc3);
        DocumentModel doc4 = session.createDocumentModel("/folder1", "doc4",
                "File");
        doc4.setPropertyValue("file:content", new StringBlob(
                "The content of file 4."));
        doc4 = session.createDocument(doc4);
        DocumentModel doc5 = session.createDocumentModel("/folder2", "doc5",
                "File");
        doc5.setPropertyValue("file:content", new StringBlob(
                "The content of file 5."));
        doc5 = session.createDocument(doc5);

        session.save();

        docChangeSummary = getFolderDocumentChangeSummary("/folder1");
        expectedSyncRootPaths.remove("/folder2");
        assertEquals(expectedSyncRootPaths, docChangeSummary.getSyncRootPaths());
        assertEquals(2, docChangeSummary.getDocumentChanges().size());
        assertEquals("found_changes", docChangeSummary.getStatusCode());
    }

    /**
     * Gets the changes summary for the user bound to the
     * {@link #session} using the {@link NuxeoDriveGetChangeSummary}
     * automation operation and updates the {@link #lastSuccessfulSync} date.
     */
    protected FileSystemChangeSummary getChangeSummary() throws Exception {

        // Wait 1 second as the mock change finder relies on steps of 1 second
        Thread.sleep(1000);
        Blob docChangeSummaryJSON = (Blob) clientSession.newRequest(
                NuxeoDriveGetChangeSummary.ID).set(
                "lastSuccessfulSync", lastSuccessfulSync).execute();
        assertNotNull(docChangeSummaryJSON);

        FileSystemChangeSummary docChangeSummary = mapper.readValue(
                docChangeSummaryJSON.getStream(), FileSystemChangeSummary.class);
        assertNotNull(docChangeSummary);

        lastSuccessfulSync = docChangeSummary.getSyncDate();
        return docChangeSummary;
    }

    /**
     * Gets the document changes summary for the given folder using the
     * {@link NuxeoDriveGetFolderChangeSummary} automation operation and
     * updates the {@link #lastSuccessfulSync} date.
     */
    protected FileSystemChangeSummary getFolderDocumentChangeSummary(
            String folderPath) throws Exception {

        // Wait 1 second as the mock change finder relies on steps of 1 second
        Thread.sleep(1000);
        Blob docChangeSummaryJSON = (Blob) clientSession.newRequest(
                NuxeoDriveGetFolderChangeSummary.ID).set("folderPath",
                folderPath).set("lastSuccessfulSync", lastSuccessfulSync).execute();
        assertNotNull(docChangeSummaryJSON);

        FileSystemChangeSummary docChangeSummary = mapper.readValue(
                docChangeSummaryJSON.getStream(), FileSystemChangeSummary.class);
        assertNotNull(docChangeSummary);

        lastSuccessfulSync = docChangeSummary.getSyncDate();
        return docChangeSummary;
    }

}
