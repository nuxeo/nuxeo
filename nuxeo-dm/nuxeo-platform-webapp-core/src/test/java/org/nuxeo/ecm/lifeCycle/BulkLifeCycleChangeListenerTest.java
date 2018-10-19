/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Julien Thimonier
 *     Florent Guillaume
 */
package org.nuxeo.ecm.lifeCycle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.LifeCycleConstants.DELETED_STATE;
import static org.nuxeo.ecm.core.api.LifeCycleConstants.DELETE_TRANSITION;

import java.util.Collection;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.CoreSession.CopyOption;
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
 * Simple test Case for BulkLifeCycleChangeListener
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
        testBulkLifeCycleChange();
    }

    /*
     * NXP-22197
     */
    @Test
    @Deploy("org.nuxeo.ecm.webapp.core.tests:OSGI-INF/test-bulk-life-cycle-change-paginate-contrib.xml")
    public void testLifeCycleAPIPaginate() {
        testBulkLifeCycleChange();
    }

    protected void testBulkLifeCycleChange() {

        DocumentModel folderDoc = session.createDocumentModel("/", "testFolder", "Folder");
        folderDoc = session.createDocument(folderDoc);
        DocumentModel testFile1 = session.createDocumentModel("/testFolder", "testFile1", "File");
        testFile1 = session.createDocument(testFile1);
        DocumentModel testFile2 = session.createDocumentModel("/testFolder", "testFile2", "File");
        testFile2 = session.createDocument(testFile2);

        session.saveDocument(folderDoc);
        session.saveDocument(testFile1);
        session.saveDocument(testFile2);

        Collection<String> allowedStateTransitions = session.getAllowedStateTransitions(folderDoc.getRef());
        assertTrue(allowedStateTransitions.contains("approve"));

        assertTrue(session.followTransition(folderDoc.getRef(), "approve"));
        session.save();

        nextTransaction();

        // Check that the BulkLifeCycleChangeListener has changed child files to approved
        assertEquals("approved", session.getCurrentLifeCycleState(testFile1.getRef()));
        assertEquals("approved", session.getCurrentLifeCycleState(testFile2.getRef()));
    }

    @Test
    public void testCopyLifeCycleHandler() {
        testBulkLifeCycleCopy();
    }

    /*
     * NXP-22197
     */
    @Test
    @Deploy("org.nuxeo.ecm.webapp.core.tests:OSGI-INF/test-bulk-life-cycle-change-paginate-contrib.xml")
    public void testCopyLifeCycleHandlerPaginate() {
        testBulkLifeCycleCopy();
    }

    private void testBulkLifeCycleCopy() {
        DocumentModel folderDoc = session.createDocumentModel("/", "testFolder", "Folder");
        folderDoc = session.createDocument(folderDoc);
        DocumentModel testFile1 = session.createDocumentModel("/testFolder", "testFile1", "File");
        testFile1 = session.createDocument(testFile1);
        DocumentModel testFile2 = session.createDocumentModel("/testFolder", "testFile2", "File");
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
        folderCopy = session.copy(folderDoc.getRef(), folderCopy.getRef(), "folderCopy", CopyOption.RESET_LIFE_CYCLE);
        session.save();

        nextTransaction();

        DocumentModelList childrenCopy = session.getChildren(folderCopy.getRef());
        assertEquals("project", session.getCurrentLifeCycleState(folderCopy.getRef()));
        for (DocumentModel child : childrenCopy) {
            assertEquals("project", session.getCurrentLifeCycleState(child.getRef()));
        }
    }

    /*
     * NXP-22608
     */
    @Test
    public void testLifeCycleAPITwoLevels() {
        testBulkLifeCycleChangeTwoLevels();
    }

    /*
     * NXP-22197
     */
    @Test
    @Deploy("org.nuxeo.ecm.webapp.core.tests:OSGI-INF/test-bulk-life-cycle-change-paginate-contrib.xml")
    public void testLifeCycleAPITwoLevelsPaginate() {
        testBulkLifeCycleChangeTwoLevels();
    }

    protected void testBulkLifeCycleChangeTwoLevels() {

        DocumentModel folderDoc1 = session.createDocumentModel("/", "testFolder1", "Folder");
        folderDoc1 = session.createDocument(folderDoc1);
        DocumentModel folderDoc2 = session.createDocumentModel("/testFolder1", "testFolder2", "Folder");
        folderDoc2 = session.createDocument(folderDoc2);
        DocumentModel testFile1 = session.createDocumentModel("/testFolder1/testFolder2", "testFile1", "File");
        testFile1 = session.createDocument(testFile1);
        DocumentModel testFile2 = session.createDocumentModel("/testFolder1/testFolder2", "testFile2", "File");
        testFile2 = session.createDocument(testFile2);
        DocumentModel testFile3 = session.createDocumentModel("/testFolder1/testFolder2", "testFile3", "File");
        testFile3 = session.createDocument(testFile3);

        session.saveDocument(folderDoc1);
        session.saveDocument(folderDoc2);
        session.saveDocument(testFile1);
        session.saveDocument(testFile2);
        session.saveDocument(testFile3);

        Collection<String> allowedStateTransitions = session.getAllowedStateTransitions(folderDoc1.getRef());
        assertTrue(allowedStateTransitions.contains("approve"));

        assertTrue(session.followTransition(folderDoc1.getRef(), "approve"));
        session.save();

        nextTransaction();

        // Check that the BulkLifeCycleChangeListener has changed child folders and files to approved
        assertEquals("approved", session.getCurrentLifeCycleState(folderDoc2.getRef()));
        assertEquals("approved", session.getCurrentLifeCycleState(testFile1.getRef()));
        assertEquals("approved", session.getCurrentLifeCycleState(testFile2.getRef()));
        assertEquals("approved", session.getCurrentLifeCycleState(testFile3.getRef()));
    }

    @Test
    @Deprecated
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-trash-service-lifecycle-override.xml")
    public void testLifeCycleAPIDelete() {
        testBulkLifeCycleChangeDelete();
    }

    /*
     * NXP-22197
     */
    @Test
    @Deprecated
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-trash-service-lifecycle-override.xml")
    @Deploy("org.nuxeo.ecm.webapp.core.tests:OSGI-INF/test-bulk-life-cycle-change-paginate-contrib.xml")
    public void testLifeCycleAPIDeletePaginate() {
        testBulkLifeCycleChangeDelete();
    }

    @Deprecated
    protected void testBulkLifeCycleChangeDelete() {

        DocumentModel folderDoc = session.createDocumentModel("/", "testFolder", "Folder");
        folderDoc = session.createDocument(folderDoc);
        DocumentModel testFile1 = session.createDocumentModel("/testFolder", "testFile1", "File");
        testFile1 = session.createDocument(testFile1);
        DocumentModel testFile2 = session.createDocumentModel("/testFolder", "testFile2", "File");
        testFile2 = session.createDocument(testFile2);
        DocumentModel testFile3 = session.createDocumentModel("/testFolder", "testFile3", "File");
        testFile3 = session.createDocument(testFile3);

        session.saveDocument(folderDoc);
        session.saveDocument(testFile1);
        session.saveDocument(testFile2);
        session.saveDocument(testFile3);

        Collection<String> allowedStateTransitions = session.getAllowedStateTransitions(folderDoc.getRef());
        assertTrue(allowedStateTransitions.contains(DELETE_TRANSITION));

        assertTrue(session.followTransition(folderDoc.getRef(), DELETE_TRANSITION));
        session.save();

        nextTransaction();

        // Check that the BulkLifeCycleChangeListener has changed child folders and files to approved
        assertEquals(DELETED_STATE, session.getCurrentLifeCycleState(testFile1.getRef()));
        assertEquals(DELETED_STATE, session.getCurrentLifeCycleState(testFile2.getRef()));
        assertEquals(DELETED_STATE, session.getCurrentLifeCycleState(testFile3.getRef()));
    }

}
