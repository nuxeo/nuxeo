/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.nuxeo.drive.service.impl.AuditChangeFinder;
import org.nuxeo.drive.service.impl.RootDefinitionsHelper;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.core.storage.sql.DatabaseSQLServer;
import org.nuxeo.ecm.core.test.RepositorySettings;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 * Base class to test the {@link FileSystemChangeFinder} implementations.
 *
 * @since 7.3
 */
@RunWith(FeaturesRunner.class)
@Deploy({ "org.nuxeo.ecm.platform.userworkspace.types", "org.nuxeo.ecm.platform.userworkspace.core",
        "org.nuxeo.drive.core", "org.nuxeo.ecm.platform.collections.core",
        "org.nuxeo.drive.core.test:OSGI-INF/test-nuxeodrive-types-contrib.xml", })
public class AbstractChangeFinderTestCase {

    @Inject
    protected CoreSession session;

    @Inject
    protected RepositorySettings repository;

    @Inject
    protected NuxeoDriveManager nuxeoDriveManager;

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

    @BeforeClass
    public static void ignoreIfSQLServer() {
        assumeTrue(!(DatabaseHelper.DATABASE instanceof DatabaseSQLServer));
    }

    @Before
    public void init() throws Exception {
        // Enable deletion listener because the tear down disables it
        eventServiceAdmin.setListenerEnabledFlag("nuxeoDriveFileSystemDeletionListener", true);

        lastEventLogId = 0;
        lastSyncActiveRootDefinitions = "";
        Framework.getProperties().put("org.nuxeo.drive.document.change.limit", "10");

        // Create test users
        Session userDir = directoryService.getDirectory("userDirectory").getSession();
        try {
            if (userDir.getEntry("user1") != null) {
                userDir.deleteEntry("user1");
            }
            Map<String, Object> user1 = new HashMap<String, Object>();
            user1.put("username", "user1");
            user1.put("groups", Arrays.asList(new String[] { "members" }));
            userDir.createEntry(user1);
        } finally {
            userDir.close();
        }
        user1Session = repository.openSessionAs("user1");

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
            user1Session.close();
        }
        Session usersDir = directoryService.getDirectory("userDirectory").getSession();
        try {
            if (usersDir.getEntry("user1") != null) {
                usersDir.deleteEntry("user1");
            }
        } finally {
            usersDir.close();
        }

        // Disable deletion listener for the repository cleanup phase done in
        // CoreFeature#afterTeardown to avoid exception due to no active
        // transaction in FileSystemItemManagerImpl#getSession
        eventServiceAdmin.setListenerEnabledFlag("nuxeoDriveFileSystemDeletionListener", false);
    }

    /**
     * Gets the document changes for the given user's synchronization roots using the {@link AuditChangeFinder} and
     * updates {@link #lastEventLogId}.
     *
     * @throws ClientException
     */
    protected List<FileSystemItemChange> getChanges(Principal principal) throws InterruptedException, ClientException {
        return getChangeSummary(principal).getFileSystemChanges();
    }

    /**
     * Gets the document changes for the Administrator user.
     */
    protected List<FileSystemItemChange> getChanges() throws InterruptedException, ClientException {
        return getChanges(session.getPrincipal());
    }

    /**
     * Gets the document changes summary for the given user's synchronization roots using the {@link NuxeoDriveManager}
     * and updates {@link #lastEventLogId}.
     */
    protected FileSystemChangeSummary getChangeSummary(Principal principal)
            throws ClientException, InterruptedException {
        Map<String, Set<IdRef>> lastSyncActiveRootRefs = RootDefinitionsHelper.parseRootDefinitions(
                lastSyncActiveRootDefinitions);
        FileSystemChangeSummary changeSummary = nuxeoDriveManager.getChangeSummaryIntegerBounds(principal,
                lastSyncActiveRootRefs, lastEventLogId);
        assertNotNull(changeSummary);
        lastEventLogId = changeSummary.getUpperBound();
        lastSyncActiveRootDefinitions = changeSummary.getActiveSynchronizationRootDefinitions();
        return changeSummary;
    }

    protected void commitAndWaitForAsyncCompletion() throws Exception {
        TransactionHelper.commitOrRollbackTransaction();
        waitForAsyncCompletion();
        TransactionHelper.startTransaction();

    }

    protected void waitForAsyncCompletion() throws Exception {
        workManager.awaitCompletion(20, TimeUnit.SECONDS);
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
        Set<SimpleFileSystemItemChange> simpleChanges = new HashSet<SimpleFileSystemItemChange>();
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

    protected final class SimpleFileSystemItemChange {

        protected String docId;

        protected String eventName;

        protected String repositoryId;

        protected String lifeCycleState;

        protected String fileSystemItemId;

        protected String fileSystemItemName;

        public SimpleFileSystemItemChange(String docId, String eventName) {
            this(docId, eventName, null);
        }

        public SimpleFileSystemItemChange(String docId, String eventName, String repositoryId) {
            this(docId, eventName, repositoryId, null);
        }

        public SimpleFileSystemItemChange(String docId, String eventName, String repositoryId,
                String fileSystemItemId) {
            this(docId, eventName, repositoryId, fileSystemItemId, null);
        }

        public SimpleFileSystemItemChange(String docId, String eventName, String repositoryId, String fileSystemItemId,
                String fileSystemItemName) {
            this.docId = docId;
            this.eventName = eventName;
            this.repositoryId = repositoryId;
            this.fileSystemItemId = fileSystemItemId;
            this.fileSystemItemName = fileSystemItemName;
        }

        public String getDocId() {
            return docId;
        }

        public String getEventName() {
            return eventName;
        }

        public String getRepositoryId() {
            return repositoryId;
        }

        public String getLifeCycleState() {
            return lifeCycleState;
        }

        public String getFileSystemItemId() {
            return fileSystemItemId;
        }

        public String getFileSystemItemName() {
            return fileSystemItemName;
        }

        public void setLifeCycleState(String lifeCycleState) {
            this.lifeCycleState = lifeCycleState;
        }

        @Override
        public int hashCode() {
            int hash = 17;
            hash = hash * 37 + docId.hashCode();
            return hash * 37 + eventName.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof SimpleFileSystemItemChange)) {
                return false;
            }
            SimpleFileSystemItemChange other = (SimpleFileSystemItemChange) obj;
            boolean isEqual = docId.equals(other.getDocId()) && eventName.equals(other.getEventName());
            return isEqual
                    && (repositoryId == null || other.getRepositoryId() == null
                            || repositoryId.equals(other.getRepositoryId()))
                    && (lifeCycleState == null || other.getLifeCycleState() == null
                            || lifeCycleState.equals(other.getLifeCycleState()))
                    && (fileSystemItemId == null || other.getFileSystemItemId() == null
                            || fileSystemItemId.equals(other.getFileSystemItemId()))
                    && (fileSystemItemName == null || other.getFileSystemItemName() == null
                            || fileSystemItemName.equals(other.getFileSystemItemName()));
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("(");
            sb.append(docId);
            sb.append(", ");
            sb.append(eventName);
            if (repositoryId != null) {
                sb.append(", ");
                sb.append(repositoryId);
            }
            if (lifeCycleState != null) {
                sb.append(", ");
                sb.append(lifeCycleState);
            }
            if (fileSystemItemId != null) {
                sb.append(", ");
                sb.append(fileSystemItemId);
            }
            if (fileSystemItemName != null) {
                sb.append(", ");
                sb.append(fileSystemItemName);
            }
            sb.append(")");
            return sb.toString();
        }
    }

}
