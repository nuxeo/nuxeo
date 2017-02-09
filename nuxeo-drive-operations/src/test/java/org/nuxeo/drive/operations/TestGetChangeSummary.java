/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.security.Principal;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.service.FileSystemChangeSummary;
import org.nuxeo.drive.service.FileSystemItemChange;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.impl.FileSystemChangeSummaryImpl;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Tests the {@link NuxeoDriveGetChangeSummary} operation.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features(EmbeddedAutomationServerFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.drive.core", "org.nuxeo.drive.operations", "org.nuxeo.ecm.core.cache",
        "org.nuxeo.drive.core.test:OSGI-INF/test-nuxeodrive-sync-root-cache-contrib.xml" })
@LocalDeploy("org.nuxeo.drive.operations:OSGI-INF/test-nuxeodrive-change-finder-contrib.xml")
@Jetty(port = 18080)
public class TestGetChangeSummary {

    @Inject
    protected CoreSession session;

    @Inject
    protected NuxeoDriveManager nuxeoDriveManager;

    @Inject
    protected Session clientSession;

    protected long lastSyncDate;

    protected DocumentModel folder1;

    protected DocumentModel folder2;

    protected ObjectMapper mapper;

    protected String lastSyncActiveRoots;

    @Before
    public void init() throws Exception {

        lastSyncDate = Calendar.getInstance().getTimeInMillis();
        lastSyncActiveRoots = "";

        folder1 = session.createDocument(session.createDocumentModel("/", "folder1", "Folder"));
        folder2 = session.createDocument(session.createDocumentModel("/", "folder2", "Folder"));

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
            nuxeoDriveManager.registerSynchronizationRoot(administrator, folder1, session);
            nuxeoDriveManager.registerSynchronizationRoot(administrator, folder2, session);

            doc1 = session.createDocumentModel("/folder1", "doc1", "File");
            doc1.setPropertyValue("file:content", new StringBlob("The content of file 1."));
            doc1 = session.createDocument(doc1);
            Thread.sleep(1000);
            doc2 = session.createDocumentModel("/folder2", "doc2", "File");
            doc2.setPropertyValue("file:content", new StringBlob("The content of file 2."));
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
        DocumentModel doc3 = session.createDocumentModel("/folder1", "doc3", "File");
        doc3.setPropertyValue("file:content", new StringBlob("The content of file 3."));
        doc3 = session.createDocument(doc3);
        DocumentModel doc4 = session.createDocumentModel("/folder1", "doc4", "File");
        doc4.setPropertyValue("file:content", new StringBlob("The content of file 4."));
        doc4 = session.createDocument(doc4);
        DocumentModel doc5 = session.createDocumentModel("/folder2", "doc5", "File");
        doc5.setPropertyValue("file:content", new StringBlob("The content of file 5."));
        doc5 = session.createDocument(doc5);
        // Ensure the rounded modification date will not be equal to the truncated sync date
        Thread.sleep(1000);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
    }

    /**
     * Gets the changes summary for the user bound to the {@link #session} using the {@link NuxeoDriveGetChangeSummary}
     * automation operation and updates the {@link #lastSyncDate} date.
     */
    protected FileSystemChangeSummary getChangeSummary() throws Exception {
        // Wait 1 second as the mock change finder relies on steps of 1 second
        Thread.sleep(1000);
        Blob changeSummaryJSON = (Blob) clientSession.newRequest(NuxeoDriveGetChangeSummary.ID).set("lastSyncDate",
                lastSyncDate).set("lastSyncActiveRootDefinitions", lastSyncActiveRoots).execute();
        assertNotNull(changeSummaryJSON);

        FileSystemChangeSummary changeSummary = mapper.readValue(changeSummaryJSON.getStream(),
                FileSystemChangeSummaryImpl.class);
        assertNotNull(changeSummary);

        lastSyncDate = changeSummary.getSyncDate();
        lastSyncActiveRoots = changeSummary.getActiveSynchronizationRootDefinitions();
        return changeSummary;
    }

}
