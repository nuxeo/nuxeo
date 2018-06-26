/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.drive.fixtures;

import static org.junit.Assert.assertNotNull;

import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.nuxeo.drive.service.FileSystemChangeFinder;
import org.nuxeo.drive.service.FileSystemChangeSummary;
import org.nuxeo.drive.service.FileSystemItemChange;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.impl.AuditChangeFinder;
import org.nuxeo.drive.service.impl.RootDefinitionsHelper;
import org.nuxeo.drive.test.NuxeoDriveFeature;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Base class to test the {@link FileSystemChangeFinder} implementations.
 *
 * @since 7.3
 */
@RunWith(FeaturesRunner.class)
@Features(NuxeoDriveFeature.class)
public abstract class AbstractChangeFinderTestCase {

    @Inject
    protected CoreSession session;

    @Inject
    protected NuxeoDriveManager nuxeoDriveManager;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected DirectoryService directoryService;

    @Inject
    protected EventServiceAdmin eventServiceAdmin;

    @Inject
    protected WorkManager workManager;

    protected long lastEventLogId;

    protected String lastSyncActiveRootDefinitions;

    protected DocumentModel folder1;

    protected DocumentModel folder2;

    protected DocumentModel folder3;

    protected CoreSession user1Session;

    @Before
    public void init() throws Exception {
        // Enable deletion listener because the tear down disables it
        eventServiceAdmin.setListenerEnabledFlag("nuxeoDriveFileSystemDeletionListener", true);

        lastEventLogId = 0;
        lastSyncActiveRootDefinitions = "";
        Framework.getProperties().put("org.nuxeo.drive.document.change.limit", "20");

        // Create test users
        try (Session userDir = directoryService.open("userDirectory")) {
            Map<String, Object> user1 = new HashMap<>();
            user1.put("username", "user1");
            user1.put("groups", Arrays.asList(new String[] { "members" }));
            userDir.createEntry(user1);
        }
        user1Session = coreFeature.openCoreSession("user1");

        commitAndWaitForAsyncCompletion();

        folder1 = session.createDocument(session.createDocumentModel("/", "folder1", "Folder"));
        folder2 = session.createDocument(session.createDocumentModel("/", "folder2", "Folder"));
        folder3 = session.createDocument(session.createDocumentModel("/", "folder3", "Folder"));
        setPermissions(folder1, new ACE("user1", SecurityConstants.READ_WRITE));
        setPermissions(folder2, new ACE("user1", SecurityConstants.READ_WRITE));

        commitAndWaitForAsyncCompletion();
    }

    @After
    public void tearDown() throws Exception {

        if (user1Session != null) {
            ((CloseableCoreSession) user1Session).close();
        }
        try (Session usersDir = directoryService.open("userDirectory")) {
            usersDir.deleteEntry("user1");
        }

        // Disable deletion listener for the repository cleanup phase done in
        // CoreFeature#afterTeardown to avoid exception due to no active
        // transaction in FileSystemItemManagerImpl#getSession
        eventServiceAdmin.setListenerEnabledFlag("nuxeoDriveFileSystemDeletionListener", false);
    }

    /**
     * Gets the document changes for the given user's synchronization roots using the {@link AuditChangeFinder} and
     * updates {@link #lastEventLogId}.
     */
    protected List<FileSystemItemChange> getChanges(Principal principal) throws InterruptedException {
        return getChangeSummary(principal).getFileSystemChanges();
    }

    /**
     * Gets the document changes for the Administrator user.
     */
    protected List<FileSystemItemChange> getChanges() throws InterruptedException {
        return getChanges(session.getPrincipal());
    }

    /**
     * Gets the document changes summary for the given user's synchronization roots using the {@link NuxeoDriveManager}
     * and updates {@link #lastEventLogId}.
     */
    protected FileSystemChangeSummary getChangeSummary(Principal principal) throws InterruptedException {
        Map<String, Set<IdRef>> lastSyncActiveRootRefs = RootDefinitionsHelper.parseRootDefinitions(
                lastSyncActiveRootDefinitions);
        FileSystemChangeSummary changeSummary = nuxeoDriveManager.getChangeSummaryIntegerBounds(principal,
                lastSyncActiveRootRefs, lastEventLogId);
        assertNotNull(changeSummary);
        lastEventLogId = changeSummary.getUpperBound();
        lastSyncActiveRootDefinitions = changeSummary.getActiveSynchronizationRootDefinitions();
        return changeSummary;
    }

    @Inject
    TransactionalFeature txFeature;

    protected void commitAndWaitForAsyncCompletion() throws Exception {
        txFeature.nextTransaction();
    }

    protected void waitForAsyncCompletion() throws Exception {
        txFeature.nextTransaction(20, TimeUnit.SECONDS);
    }

    protected void setPermissions(DocumentModel doc, ACE... aces) throws Exception {
        ACP acp = session.getACP(doc.getRef());
        ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
        for (int i = 0; i < aces.length; i++) {
            localACL.add(i, aces[i]);
        }
        session.setACP(doc.getRef(), acp, true);
        commitAndWaitForAsyncCompletion();
    }

    protected void resetPermissions(DocumentModel doc, String userName) {
        ACP acp = session.getACP(doc.getRef());
        ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
        Iterator<ACE> localACLIt = localACL.iterator();
        while (localACLIt.hasNext()) {
            ACE ace = localACLIt.next();
            if (userName.equals(ace.getUsername())) {
                localACLIt.remove();
            }
        }
        session.setACP(doc.getRef(), acp, true);
        session.save();
    }

    protected Set<SimpleFileSystemItemChange> toSimpleFileSystemItemChanges(List<FileSystemItemChange> changes) {
        Set<SimpleFileSystemItemChange> simpleChanges = new HashSet<>();
        for (FileSystemItemChange change : changes) {
            simpleChanges.add(toSimpleFileSystemItemChange(change));
        }
        return simpleChanges;
    }

    protected SimpleFileSystemItemChange toSimpleFileSystemItemChange(FileSystemItemChange change) {
        SimpleFileSystemItemChange simpleChange = new SimpleFileSystemItemChange(change.getDocUuid(),
                change.getEventId(), change.getRepositoryId(), change.getFileSystemItemId(),
                change.getFileSystemItemName());
        DocumentRef changeDocRef = new IdRef(change.getDocUuid());
        if (session.exists(changeDocRef)) {
            simpleChange.setLifeCycleState(session.getDocument(changeDocRef).getCurrentLifeCycleState());
        }
        return simpleChange;
    }

}
