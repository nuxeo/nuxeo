/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Julien Thimonier
 *     Florent Guillaume
 */

package org.nuxeo.ecm.lifeCycle;

import java.util.Collection;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;

/**
 * Simple test Case for MassLifeCycleChangeListener
 */
public class BulkLifeCycleChangeListenerTest extends SQLRepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.webapp.core");
    }

    protected void waitForAsyncExec() {
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
    }

    public void testLifeCycleAPI() throws ClientException {
        openSession();

        DocumentModel folderDoc = session.createDocumentModel("/",
                "testFolder", "Folder");
        folderDoc = session.createDocument(folderDoc);
        DocumentModel testFile1 = session.createDocumentModel(
                folderDoc.getPathAsString(), "testFile1", "File");
        testFile1 = session.createDocument(testFile1);
        DocumentModel testFile2 = session.createDocumentModel(
                folderDoc.getPathAsString(), "testFile2", "File");
        testFile2 = session.createDocument(testFile2);

        session.saveDocument(folderDoc);
        session.saveDocument(testFile1);
        session.saveDocument(testFile2);

        Collection<String> allowedStateTransitions = session.getAllowedStateTransitions(folderDoc.getRef());
        assertTrue(allowedStateTransitions.contains("approve"));

        assertTrue(session.followTransition(folderDoc.getRef(), "approve"));

        session.save();
        waitForAsyncExec();
        session.save(); // process async invalidations

        // Check that the MassCycleListener has changed child files to approved
        assertEquals("approved",
                session.getCurrentLifeCycleState(testFile1.getRef()));
        assertEquals("approved",
                session.getCurrentLifeCycleState(testFile2.getRef()));
    }

}
