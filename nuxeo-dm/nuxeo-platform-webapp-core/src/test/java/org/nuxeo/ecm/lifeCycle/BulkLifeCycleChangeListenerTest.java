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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Simple test Case for MassLifeCycleChangeListener
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.webapp.core")
public class BulkLifeCycleChangeListenerTest {

    @Inject
    protected CoreSession session;

    protected void nextTransaction() {
        TransactionHelper.commitOrRollbackTransaction();
        Framework.getService(EventService.class).waitForAsyncCompletion();
        TransactionHelper.startTransaction();
    }

    @Test
    public void testLifeCycleAPI() {

        DocumentModel folderDoc = session.createDocumentModel("/", "testFolder", "Folder");
        folderDoc = session.createDocument(folderDoc);
        DocumentModel testFile1 = session.createDocumentModel(folderDoc.getPathAsString(), "testFile1", "File");
        testFile1 = session.createDocument(testFile1);
        DocumentModel testFile2 = session.createDocumentModel(folderDoc.getPathAsString(), "testFile2", "File");
        testFile2 = session.createDocument(testFile2);

        session.saveDocument(folderDoc);
        session.saveDocument(testFile1);
        session.saveDocument(testFile2);

        Collection<String> allowedStateTransitions = session.getAllowedStateTransitions(folderDoc.getRef());
        assertTrue(allowedStateTransitions.contains("approve"));

        assertTrue(session.followTransition(folderDoc.getRef(), "approve"));
        session.save();

        nextTransaction();

        // Check that the MassCycleListener has changed child files to approved
        assertEquals("approved", session.getCurrentLifeCycleState(testFile1.getRef()));
        assertEquals("approved", session.getCurrentLifeCycleState(testFile2.getRef()));
    }

    @Test
    public void testCopyLifeCycleHandler() {
        DocumentModel folderDoc = session.createDocumentModel("/", "testFolder", "Folder");
        folderDoc = session.createDocument(folderDoc);
        DocumentModel testFile1 = session.createDocumentModel(folderDoc.getPathAsString(), "testFile1", "File");
        testFile1 = session.createDocument(testFile1);
        DocumentModel testFile2 = session.createDocumentModel(folderDoc.getPathAsString(), "testFile2", "File");
        testFile2 = session.createDocument(testFile2);
        session.saveDocument(folderDoc);
        session.saveDocument(testFile1);
        session.saveDocument(testFile2);
        session.followTransition(folderDoc.getRef(), "approve");
        session.save();

        nextTransaction();

        // All documents in approve life cycle state
        // Checking document copy lifecycle handler
        DocumentModel folderCopy = session.createDocumentModel("/", "folderCopy", "Folder");
        folderCopy = session.createDocument(folderCopy);
        folderCopy = session.copy(folderDoc.getRef(), folderCopy.getRef(), "folderCopy", true);
        session.save();

        nextTransaction();

        DocumentModelList childrenCopy = session.getChildren(folderCopy.getRef());
        assertEquals("project", session.getCurrentLifeCycleState(folderCopy.getRef()));
        for (DocumentModel child : childrenCopy) {
            assertEquals("project", session.getCurrentLifeCycleState(child.getRef()));
        }
    }

}
