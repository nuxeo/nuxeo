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

import java.security.Principal;
import java.util.Calendar;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.service.FileSystemChangeSummary;
import org.nuxeo.drive.service.FileSystemItemChange;
import org.nuxeo.drive.service.MockChangeFinder;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.impl.FileSystemChangeSummaryImpl;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 * Tests the {@link NuxeoDriveGetChangeSummary} operation.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features({TransactionalFeature.class, EmbeddedAutomationServerFeature.class})
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.drive.core", "org.nuxeo.drive.operations" })
@Jetty(port = 18080)
public class TestGetChangeSummary {

    @Inject
    protected CoreSession session;

    @Inject
    protected NuxeoDriveManager nuxeoDriveManager;

    @Inject
    protected Session clientSession;

    protected long lastSuccessfulSync;

    protected DocumentModel folder1;

    protected DocumentModel folder2;

    protected ObjectMapper mapper;

    protected String lastSyncActiveRoots;

    @Before
    public void init() throws Exception {

        nuxeoDriveManager.setChangeFinder(new MockChangeFinder());
        lastSuccessfulSync = Calendar.getInstance().getTimeInMillis();
        lastSyncActiveRoots = "";

        folder1 = session.createDocument(session.createDocumentModel("/",
                "folder1", "Folder"));
        folder2 = session.createDocument(session.createDocumentModel("/",
                "folder2", "Folder"));

        mapper = new ObjectMapper();
    }

    @Test
    public void testGetChangesSummary() throws Exception {

        // No sync roots => shouldn't find any changes
        FileSystemChangeSummary changeSummary = getChangeSummary();
        assertTrue(changeSummary.getFileSystemChanges().isEmpty());
        assertEquals(Boolean.FALSE, changeSummary.getHasTooManyChanges());

        // Register sync roots and create 2 documents => should find 2 changes
        DocumentModel doc1;
        DocumentModel doc2;
        try {
            Principal administrator = session.getPrincipal();
            nuxeoDriveManager.registerSynchronizationRoot(administrator,
                    folder1, session);
            nuxeoDriveManager.registerSynchronizationRoot(administrator,
                    folder2, session);

            doc1 = session.createDocumentModel("/folder1", "doc1", "File");
            doc1.setPropertyValue("file:content", new StringBlob(
                    "The content of file 1."));
            doc1 = session.createDocument(doc1);
            Thread.sleep(1000);
            doc2 = session.createDocumentModel("/folder2", "doc2", "File");
            doc2.setPropertyValue("file:content", new StringBlob(
                    "The content of file 2."));
            doc2 = session.createDocument(doc2);

            session.save();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        changeSummary = getChangeSummary();
        List<FileSystemItemChange> docChanges = changeSummary.getFileSystemChanges();
        assertEquals(2, docChanges.size());
        FileSystemItemChange docChange = docChanges.get(0);
        assertEquals("test", docChange.getRepositoryId());
        assertEquals("documentChanged", docChange.getEventId());
        assertEquals(doc2.getId(), docChange.getDocUuid());
        docChange = docChanges.get(1);
        assertEquals("test", docChange.getRepositoryId());
        assertEquals("documentChanged", docChange.getEventId());
        assertEquals(doc1.getId(), docChange.getDocUuid());
        assertEquals(Boolean.FALSE, changeSummary.getHasTooManyChanges());

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

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
    }

    /**
     * Gets the changes summary for the user bound to the {@link #session} using
     * the {@link NuxeoDriveGetChangeSummary} automation operation and updates
     * the {@link #lastSuccessfulSync} date.
     */
    protected FileSystemChangeSummary getChangeSummary() throws Exception {
        // Wait 1 second as the mock change finder relies on steps of 1 second
        Thread.sleep(1000);
        Blob changeSummaryJSON = (Blob) clientSession.newRequest(
                NuxeoDriveGetChangeSummary.ID).set("lastSyncDate",
                lastSuccessfulSync).set("lastSyncActiveRootDefinitions",
                lastSyncActiveRoots).execute();
        assertNotNull(changeSummaryJSON);

        FileSystemChangeSummary changeSummary = mapper.readValue(
                changeSummaryJSON.getStream(),
                FileSystemChangeSummaryImpl.class);
        assertNotNull(changeSummary);

        lastSuccessfulSync = changeSummary.getSyncDate();
        lastSyncActiveRoots = changeSummary.getActiveSynchronizationRootDefinitions();
        return changeSummary;
    }

}
