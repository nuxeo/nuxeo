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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.impl.DefaultTopLevelFolderItem;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.test.RestFeature;
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
    protected HttpAutomationClient automationClient;

    protected Session clientSession;

    protected ObjectMapper mapper;

    @Before
    public void init() throws Exception {

        // Get an Automation client session
        clientSession = automationClient.getSession("Administrator",
                "Administrator");
        mapper = new ObjectMapper();
    }

    @Test
    public void testGetTopLevelFolderItem() throws Exception {

        Blob topLevelFolderItemJSON = (Blob) clientSession.newRequest(
                NuxeoDriveGetTopLevelFolderItem.ID).execute();
        assertNotNull(topLevelFolderItemJSON);

        DefaultTopLevelFolderItem topLevelFolderItem = mapper.readValue(
                topLevelFolderItemJSON.getStream(),
                DefaultTopLevelFolderItem.class);

        TransactionHelper.startTransaction();
        try {
            assertNotNull(topLevelFolderItem);
            assertTrue(topLevelFolderItem instanceof DefaultTopLevelFolderItem);
            assertTrue(topLevelFolderItem.getId().endsWith(
                    "DefaultTopLevelFolderItemFactory/"));
            assertNull(topLevelFolderItem.getParentId());
            assertEquals("Nuxeo Drive", topLevelFolderItem.getName());
            assertTrue(topLevelFolderItem.isFolder());
            assertEquals("system", topLevelFolderItem.getCreator());
            assertFalse(topLevelFolderItem.getCanRename());
            assertFalse(topLevelFolderItem.getCanDelete());
            assertFalse(topLevelFolderItem.getCanCreateChild());
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

}
