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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.DefaultSyncRootFolderItem;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.test.RestFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 * Tests the {@link FileSystemItem} related operations.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features(RestFeature.class)
@Deploy({ "org.nuxeo.drive.core", "org.nuxeo.drive.operations" })
@Jetty(port = 18080)
public class TestFileSystemItemOperations {

    @Inject
    protected CoreSession session;

    @Inject
    protected NuxeoDriveManager nuxeoDriveManager;

    @Inject
    protected HttpAutomationClient automationClient;

    protected DocumentModel syncRoot1;

    protected DocumentModel syncRoot2;

    protected Session clientSession;

    protected ObjectMapper mapper;

    @Before
    public void init() throws Exception {

        // Create 2 sync roots
        syncRoot1 = session.createDocument(session.createDocumentModel("/",
                "folder1", "Folder"));
        syncRoot2 = session.createDocument(session.createDocumentModel("/",
                "folder2", "Folder"));

        // Register sync roots
        nuxeoDriveManager.registerSynchronizationRoot("Administrator",
                syncRoot1, session);
        nuxeoDriveManager.registerSynchronizationRoot("Administrator",
                syncRoot2, session);

        // Get an Automation client session
        clientSession = automationClient.getSession("Administrator",
                "Administrator");
        mapper = new ObjectMapper();
    }

    @Test
    public void testGetTopLevelChildren() throws Exception {

        Blob topLevelChildrenJSON = (Blob) clientSession.newRequest(
                NuxeoDriveGetTopLevelChildren.ID).execute();
        assertNotNull(topLevelChildrenJSON);

        List<DefaultSyncRootFolderItem> topLevelChildren = mapper.readValue(
                topLevelChildrenJSON.getStream(),
                new TypeReference<List<DefaultSyncRootFolderItem>>() {
                });

        TransactionHelper.startTransaction();
        try {
            assertNotNull(topLevelChildren);
            assertEquals(2, topLevelChildren.size());

            FileSystemItem child = topLevelChildren.get(0);
            assertTrue(child instanceof DefaultSyncRootFolderItem);
            assertEquals(
                    "defaultSyncRootFolderItemFactory/test/"
                            + syncRoot1.getId(), child.getId());
            assertTrue(child.getParentId().endsWith(
                    "DefaultTopLevelFolderItemFactory/"));
            assertEquals("folder1", child.getName());
            assertTrue(child.isFolder());
            assertEquals("Administrator", child.getCreator());
            assertFalse(child.getCanRename());
            assertTrue(child.getCanDelete());
            assertTrue(((FolderItem) child).getCanCreateChild());

            child = topLevelChildren.get(1);
            assertTrue(child instanceof DefaultSyncRootFolderItem);
            assertEquals(
                    "defaultSyncRootFolderItemFactory/test/"
                            + syncRoot2.getId(), child.getId());
            assertEquals(
                    "org.nuxeo.drive.service.impl.DefaultTopLevelFolderItemFactory/",
                    child.getParentId());
            assertEquals("folder2", child.getName());
            assertTrue(child.isFolder());
            assertEquals("Administrator", child.getCreator());
            assertFalse(child.getCanRename());
            assertTrue(child.getCanDelete());
            assertTrue(((FolderItem) child).getCanCreateChild());
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
    }
}
