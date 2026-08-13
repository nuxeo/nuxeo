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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.nuxeo.audit.api.AuditQueryBuilder;
import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.audit.api.LogEntryConstants;
import org.nuxeo.audit.impl.StreamAuditWriter;
import org.nuxeo.audit.listener.StreamAuditEventListener;
import org.nuxeo.audit.service.AuditBackend;
import org.nuxeo.drive.service.FileSystemChangeFinder;
import org.nuxeo.drive.service.FileSystemChangeSummary;
import org.nuxeo.drive.service.FileSystemItemChange;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.impl.AuditChangeFinder;
import org.nuxeo.drive.service.impl.RootDefinitionsHelper;
import org.nuxeo.drive.test.NuxeoDriveFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.query.sql.model.OrderByExprs;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamService;
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

    private static final Logger log = LogManager.getLogger(AbstractChangeFinderTestCase.class);

    protected static final String COLLECTION_FOLDER = "collectionFolder";

    protected static final String COLLECTION_SYNC_ROOT = "collectionSyncRoot";

    protected static final String DEFAULT_FILE_SYSTEM_ITEM_FACTORY_PREFIX = "defaultFileSystemItemFactory#test#";

    protected static final String DEFAULT_SYNC_ROOT_FOLDER_ITEM_FACTORY_PREFIX = "defaultSyncRootFolderItemFactory#test#";

    protected static final String FILE_CONTENT = "file:content";

    protected static final String FILE_SYSTEM_ITEM_ID_PREFIX = "test#";

    protected static final String FILE_TYPE = "File";

    protected static final String FOLDER_1 = "folder1";

    protected static final String FOLDER_1_PATH = "/folder1";

    protected static final String FOLDER_2 = "folder2";

    protected static final String FOLDER_2_PATH = "/folder2";

    protected static final String FOLDER_3 = "folder3";

    protected static final String FOLDER_3_PATH = "/folder3";

    protected static final String FOLDER_TYPE = "Folder";

    protected static final String FOLDERISH_COLLECTION = "FolderishCollection";

    protected static final String SECTION_SYNC_ROOT = "sectionSyncRoot";

    protected static final String SUB_FOLDER = "subFolder";

    protected static final String TEST_DOC = "testDoc";

    protected static final String TEST_DOC_CONTENT = "The content of testDoc.";

    protected static final String TEST_REPOSITORY = "test";

    protected static final String USER_1 = "user1";

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

    // Dump the raw audit trail on failure to help diagnose flaky change-finder assertions, since the
    // audit entries are written asynchronously (stream computation) and may not be visible yet when a test fails.
    @Rule
    public TestWatcher auditDumpOnFailure = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            dumpAuditTrailForDiagnostic(description);
        }
    };

    protected void dumpAuditTrailForDiagnostic(Description description) {
        try {
            var builder = new AuditQueryBuilder().predicate(
                    Predicates.eq(LogEntryConstants.LOG_REPOSITORY_ID, TEST_REPOSITORY))
                                                 .order(OrderByExprs.desc(LogEntryConstants.LOG_ID))
                                                 .limit(50);
            List<LogEntry> entries = Framework.getService(AuditBackend.class).queryLogs(builder);
            // dump the audit stream's Kafka lag too: distinguishes "writer hasn't caught up yet" (lag > 0) from
            // "writer is caught up but entries are still missing from the backend" (lag == 0, entries missing)
            var logManager = Framework.getService(StreamService.class).getLogManager();
            var streamName = Name.ofUrn(StreamAuditEventListener.STREAM_NAME);
            var computationName = Name.ofUrn(StreamAuditWriter.COMPUTATION_NAME);
            var lagPerPartition = logManager.getLagPerPartition(streamName, computationName);
            log.error(
                    "Diagnostic dump for failed test {}: lastEventLogId={}, lastSyncActiveRootDefinitions={}, "
                            + "audit stream lag per partition: {}, last {} audit entries (most recent first): {}",
                    description::getDisplayName, () -> lastEventLogId, () -> lastSyncActiveRootDefinitions,
                    () -> lagPerPartition, entries::size,
                    () -> entries.stream()
                                 .map(entry -> "{id=%s,eventId=%s,category=%s,docUUID=%s,docPath=%s,eventDate=%s,logDate=%s}".formatted(
                                         entry.getId(), entry.getEventId(), entry.getCategory(), entry.getDocUUID(),
                                         entry.getDocPath(), entry.getEventDate(), entry.getLogDate()))
                                 .collect(Collectors.joining(", ", "[", "]")));
        } catch (RuntimeException dumpFailure) {
            log.error("Diagnostic dump for failed test {} could not be performed", description.getDisplayName(),
                    dumpFailure);
        }
    }

    @Before
    public void init() {
        // Enable deletion listener because the tear down disables it
        eventServiceAdmin.setListenerEnabledFlag("nuxeoDriveFileSystemDeletionListener", true);

        lastEventLogId = 0;
        lastSyncActiveRootDefinitions = "";
        Framework.getProperties().put("org.nuxeo.drive.document.change.limit", "20");

        // Create test users
        try (Session userDir = directoryService.open("userDirectory")) {
            Map<String, Object> user1 = new HashMap<>();
            user1.put("username", USER_1);
            user1.put("groups", Arrays.asList("members"));
            userDir.createEntry(user1);
        }
        user1Session = coreFeature.getCoreSession(USER_1);

        commitAndWaitForAsyncCompletion();

        folder1 = session.createDocument(session.createDocumentModel("/", FOLDER_1, FOLDER_TYPE));
        folder2 = session.createDocument(session.createDocumentModel("/", FOLDER_2, FOLDER_TYPE));
        folder3 = session.createDocument(session.createDocumentModel("/", FOLDER_3, FOLDER_TYPE));
        setPermissions(folder1, new ACE(USER_1, SecurityConstants.READ_WRITE));
        setPermissions(folder2, new ACE(USER_1, SecurityConstants.READ_WRITE));

        commitAndWaitForAsyncCompletion();
    }

    @After
    public void tearDown() {
        try (Session usersDir = directoryService.open("userDirectory")) {
            usersDir.deleteEntry(USER_1);
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
    protected List<FileSystemItemChange> getChanges(NuxeoPrincipal principal) {
        return getChangeSummary(principal).getFileSystemChanges();
    }

    /**
     * Gets the document changes for the Administrator user.
     */
    protected List<FileSystemItemChange> getChanges() {
        return getChanges(session.getPrincipal());
    }

    /**
     * Gets the document changes summary for the given user's synchronization roots using the {@link NuxeoDriveManager}
     * and updates {@link #lastEventLogId}.
     */
    protected FileSystemChangeSummary getChangeSummary(NuxeoPrincipal principal) {
        Map<String, Set<IdRef>> lastSyncActiveRootRefs = RootDefinitionsHelper.parseRootDefinitions(
                lastSyncActiveRootDefinitions);
        FileSystemChangeSummary changeSummary = nuxeoDriveManager.getChangeSummary(principal, lastSyncActiveRootRefs,
                lastEventLogId);
        assertNotNull(changeSummary);
        lastEventLogId = changeSummary.getUpperBound();
        lastSyncActiveRootDefinitions = changeSummary.getActiveSynchronizationRootDefinitions();
        return changeSummary;
    }

    @Inject
    TransactionalFeature txFeature;

    protected void commitAndWaitForAsyncCompletion() {
        txFeature.nextTransaction();
    }

    protected void setPermissions(DocumentModel doc, ACE... aces) {
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
