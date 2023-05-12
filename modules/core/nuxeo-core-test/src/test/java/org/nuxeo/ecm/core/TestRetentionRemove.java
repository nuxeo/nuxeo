/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume RENARD
 */

package org.nuxeo.ecm.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.RECORDS_CLEANER_GROUP;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.REMOVE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.REMOVE_CHILDREN;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE;
import static org.nuxeo.ecm.core.model.Session.PROP_RETENTION_COMPLIANCE_MODE_ENABLED;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.security.auth.login.LoginException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentExistsException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.NuxeoLoginContext;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * @since 11.5
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.core.test.tests:test-retain-files-property.xml")
public class TestRetentionRemove {

    public static final String JOHN = "John";

    @Inject
    protected CoreSession session;

    protected DocumentModel folder;

    protected DocumentModel documentModelUnderLegalHold;

    protected DocumentModel documentModelUnderRetention;

    protected void addPermission(DocumentModel documentModel, String username, String... permissions) {
        ACP acp = documentModel.getACP();
        ACL acl = acp.getOrCreateACL();
        for (String permission : permissions) {
            acl.add(new ACE(username, permission, true));
        }
        session.setACP(documentModel.getRef(), acp, false);
        session.save();
    }

    protected void checkRemoveBlob(DocumentRef docRef, String xpath, CoreSession session, boolean expectedToFail) {
        DocumentModel doc = session.getDocument(docRef);
        assertNotNull(String.format("Property %s must not be null.", xpath), doc.getPropertyValue(xpath));
        doc.setPropertyValue(xpath, null);
        try {
            session.saveDocument(doc);
            if (expectedToFail) {
                fail();
            }
        } catch (DocumentSecurityException e) {
            if (expectedToFail) {
                assertEquals(String.format("Cannot change blob from document %s, it is under retention / hold",
                        doc.getRef()), e.getMessage());
            } else {
                throw e;
            }
        }
    }

    protected void checkRemoveDocument(DocumentRef docRef, CoreSession session, boolean expectedToFail) {
        try {
            session.removeDocument(docRef);
            if (expectedToFail) {
                fail();
            }
        } catch (DocumentExistsException e) {
            if (expectedToFail) {
                String patternStr = String.format("Cannot remove %s, subdocument \\S+ is under (retention|legal hold)",
                        folder.getRef());
                Pattern pattern = Pattern.compile(patternStr);
                Matcher matcher = pattern.matcher(e.getMessage());
                boolean matchFound = matcher.matches();
                assertTrue(matchFound);
            } else {
                throw e;
            }
        } catch (DocumentSecurityException e) {
            if (expectedToFail) {
                assertEquals(String.format(
                        "Permission denied: cannot remove document %s, Missing permission 'Remove' on document %s",
                        docRef, docRef), e.getMessage());
            } else {
                throw e;
            }
        }
    }

    protected DocumentModel createFileDocument(CoreSession session, DocumentModel parent) {
        DocumentModel documentModel = session.createDocumentModel(parent.getPathAsString(), "myDocument", "File");
        Blob blob = Blobs.createBlob("Any Content");
        documentModel.setPropertyValue("file:content", (Serializable) blob);
        blob = Blobs.createBlob("bar", "text/plain");
        documentModel.setPropertyValue("files:files",
                (Serializable) Collections.singletonList(Collections.singletonMap("file", blob)));
        return session.createDocument(documentModel);
    }

    @Test
    @WithFrameworkProperty(name = PROP_RETENTION_COMPLIANCE_MODE_ENABLED, value = "true")
    public void iCannotRemoveFolderWithRetainedChildrenInCompliance() throws LoginException {
        try (NuxeoLoginContext ignored = Framework.loginUser(JOHN)) {
            CoreSession notAdminSession = CoreInstance.getCoreSession(session.getRepositoryName());
            folder = notAdminSession.getDocument(folder.getRef());
            checkRemoveDocument(folder.getRef(), notAdminSession, true);

            // Add current user to records cleaner group.
            NuxeoPrincipal.getCurrent().setGroups(Collections.singletonList(RECORDS_CLEANER_GROUP));

            folder = notAdminSession.getDocument(folder.getRef());
            checkRemoveDocument(folder.getRef(), notAdminSession, true);
        }
    }

    @Test
    @WithFrameworkProperty(name = PROP_RETENTION_COMPLIANCE_MODE_ENABLED, value = "true")
    public void iCannotRemoveRetainedDocInCompliance() throws LoginException {
        try (NuxeoLoginContext ignored = Framework.loginUser(JOHN)) {
            CoreSession notAdminSession = CoreInstance.getCoreSession(session.getRepositoryName());
            checkRemoveDocument(documentModelUnderLegalHold.getRef(), notAdminSession, true);
            checkRemoveDocument(documentModelUnderRetention.getRef(), notAdminSession, true);
        }
    }

    @Test
    @WithFrameworkProperty(name = PROP_RETENTION_COMPLIANCE_MODE_ENABLED, value = "true")
    public void iCannotRemoveRetainedDocInComplianceAsRecordCleaner() throws LoginException {
        try (NuxeoLoginContext ignored = Framework.loginUser(JOHN)) {
            CoreSession notAdminSession = CoreInstance.getCoreSession(session.getRepositoryName());
            // Add current user to records cleaner group.
            NuxeoPrincipal.getCurrent().setGroups(Collections.singletonList(RECORDS_CLEANER_GROUP));
            checkRemoveDocument(documentModelUnderLegalHold.getRef(), notAdminSession, true);
            checkRemoveDocument(documentModelUnderRetention.getRef(), notAdminSession, true);
        }
    }

    @Test
    public void iCannotRemoveRetainedDocInGovernance() throws LoginException {
        try (NuxeoLoginContext ignored = Framework.loginUser(JOHN)) {
            CoreSession notAdminSession = CoreInstance.getCoreSession(session.getRepositoryName());
            checkRemoveDocument(documentModelUnderLegalHold.getRef(), notAdminSession, true);
            checkRemoveDocument(documentModelUnderRetention.getRef(), notAdminSession, true);
        }
    }

    @Test
    @WithFrameworkProperty(name = PROP_RETENTION_COMPLIANCE_MODE_ENABLED, value = "true")
    public void iCannotResetAttachementsInCompliance() throws LoginException {
        try (NuxeoLoginContext ignored = Framework.loginUser(JOHN)) {
            CoreSession notAdminSession = CoreInstance.getCoreSession(session.getRepositoryName());
            checkRemoveBlob(documentModelUnderLegalHold.getRef(), "files:files", notAdminSession, true);
            checkRemoveBlob(documentModelUnderRetention.getRef(), "files:files", notAdminSession, true);
        }
    }

    @Test
    @WithFrameworkProperty(name = PROP_RETENTION_COMPLIANCE_MODE_ENABLED, value = "true")
    public void iCannotResetAttachementsInComplianceAsRecordCleaner() throws LoginException {
        try (NuxeoLoginContext ignored = Framework.loginUser(JOHN)) {
            CoreSession notAdminSession = CoreInstance.getCoreSession(session.getRepositoryName());
            // Add current user to records cleaner group.
            NuxeoPrincipal.getCurrent().setGroups(Collections.singletonList(RECORDS_CLEANER_GROUP));
            checkRemoveBlob(documentModelUnderLegalHold.getRef(), "files:files", notAdminSession, true);
            checkRemoveBlob(documentModelUnderRetention.getRef(), "files:files", notAdminSession, true);
        }
    }

    @Test
    public void iCannotResetAttachementsInGovernance() throws LoginException {
        try (NuxeoLoginContext ignored = Framework.loginUser(JOHN)) {
            CoreSession notAdminSession = CoreInstance.getCoreSession(session.getRepositoryName());
            checkRemoveBlob(documentModelUnderLegalHold.getRef(), "files:files", notAdminSession, true);
            checkRemoveBlob(documentModelUnderRetention.getRef(), "files:files", notAdminSession, true);
        }
    }

    @Test
    @WithFrameworkProperty(name = PROP_RETENTION_COMPLIANCE_MODE_ENABLED, value = "true")
    public void iCannotResetMainContentInCompliance() throws LoginException {
        try (NuxeoLoginContext ignored = Framework.loginUser(JOHN)) {
            CoreSession notAdminSession = CoreInstance.getCoreSession(session.getRepositoryName());
            checkRemoveBlob(documentModelUnderLegalHold.getRef(), "file:content", notAdminSession, true);
            checkRemoveBlob(documentModelUnderRetention.getRef(), "file:content", notAdminSession, true);
        }
    }

    @Test
    @WithFrameworkProperty(name = PROP_RETENTION_COMPLIANCE_MODE_ENABLED, value = "true")
    public void iCannotResetMainContentInComplianceAsRecordCleaner() throws LoginException {
        try (NuxeoLoginContext ignored = Framework.loginUser(JOHN)) {
            CoreSession notAdminSession = CoreInstance.getCoreSession(session.getRepositoryName());
            // Add current user to records cleaner group.
            NuxeoPrincipal.getCurrent().setGroups(Collections.singletonList(RECORDS_CLEANER_GROUP));
            checkRemoveBlob(documentModelUnderLegalHold.getRef(), "file:content", notAdminSession, true);
            checkRemoveBlob(documentModelUnderRetention.getRef(), "file:content", notAdminSession, true);
        }
    }

    @Test
    public void iCannotResetMainContentInGovernance() throws LoginException {
        try (NuxeoLoginContext ignored = Framework.loginUser(JOHN)) {
            CoreSession notAdminSession = CoreInstance.getCoreSession(session.getRepositoryName());
            checkRemoveBlob(documentModelUnderLegalHold.getRef(), "file:content", notAdminSession, true);
            checkRemoveBlob(documentModelUnderRetention.getRef(), "file:content", notAdminSession, true);
        }
    }

    @Test
    @WithFrameworkProperty(name = PROP_RETENTION_COMPLIANCE_MODE_ENABLED, value = "true")
    public void iCannotResetOneAttachementInCompliance() throws LoginException {
        try (NuxeoLoginContext ignored = Framework.loginUser(JOHN)) {
            CoreSession notAdminSession = CoreInstance.getCoreSession(session.getRepositoryName());
            checkRemoveBlob(documentModelUnderLegalHold.getRef(), "files:files/0/file", notAdminSession, true);
            checkRemoveBlob(documentModelUnderRetention.getRef(), "files:files/0/file", notAdminSession, true);
        }
    }

    @Test
    @WithFrameworkProperty(name = PROP_RETENTION_COMPLIANCE_MODE_ENABLED, value = "true")
    public void iCannotResetOneAttachementInComplianceAsRecordCleaner() throws LoginException {
        try (NuxeoLoginContext ignored = Framework.loginUser(JOHN)) {
            CoreSession notAdminSession = CoreInstance.getCoreSession(session.getRepositoryName());
            // Add current user to records cleaner group.
            NuxeoPrincipal.getCurrent().setGroups(Collections.singletonList(RECORDS_CLEANER_GROUP));
            checkRemoveBlob(documentModelUnderLegalHold.getRef(), "files:files/0/file", notAdminSession, true);
            checkRemoveBlob(documentModelUnderRetention.getRef(), "files:files/0/file", notAdminSession, true);
        }
    }

    @Test
    public void iCannotResetOneAttachementInGovernance() throws LoginException {
        try (NuxeoLoginContext ignored = Framework.loginUser(JOHN)) {
            CoreSession notAdminSession = CoreInstance.getCoreSession(session.getRepositoryName());
            checkRemoveBlob(documentModelUnderLegalHold.getRef(), "files:files/0/file", notAdminSession, true);
            checkRemoveBlob(documentModelUnderRetention.getRef(), "files:files/0/file", notAdminSession, true);
        }
    }

    @Test
    public void iCanRemoveFolderWithRetainedChildrenInGovernanceAsRecordCleaner() throws LoginException {
        try (NuxeoLoginContext ignored = Framework.loginUser(JOHN)) {
            CoreSession notAdminSession = CoreInstance.getCoreSession(session.getRepositoryName());
            folder = notAdminSession.getDocument(folder.getRef());
            checkRemoveDocument(folder.getRef(), notAdminSession, true);

            // Add current user to records cleaner group.
            NuxeoPrincipal.getCurrent().setGroups(Collections.singletonList(RECORDS_CLEANER_GROUP));

            folder = notAdminSession.getDocument(folder.getRef());
            checkRemoveDocument(folder.getRef(), notAdminSession, false);
        }
    }

    @Test
    public void iCanRemoveRetainedDocInGovernanceAsRecordCleaner() throws LoginException {
        try (NuxeoLoginContext ignored = Framework.loginUser(JOHN)) {
            CoreSession notAdminSession = CoreInstance.getCoreSession(session.getRepositoryName());
            // Add current user to records cleaner group.
            NuxeoPrincipal.getCurrent().setGroups(Collections.singletonList(RECORDS_CLEANER_GROUP));
            checkRemoveDocument(documentModelUnderLegalHold.getRef(), notAdminSession, false);
            checkRemoveDocument(documentModelUnderRetention.getRef(), notAdminSession, false);
        }
    }

    @Test
    public void iCanResetAttachementsInGovernanceAsRecordCleaner() throws LoginException {
        try (NuxeoLoginContext ignored = Framework.loginUser(JOHN)) {
            CoreSession notAdminSession = CoreInstance.getCoreSession(session.getRepositoryName());
            // Add current user to records cleaner group.
            NuxeoPrincipal.getCurrent().setGroups(Collections.singletonList(RECORDS_CLEANER_GROUP));
            checkRemoveBlob(documentModelUnderLegalHold.getRef(), "files:files", notAdminSession, false);
            checkRemoveBlob(documentModelUnderRetention.getRef(), "files:files", notAdminSession, false);
        }
    }

    @Test
    public void iCanResetMainContentInGovernanceAsRecordCleaner() throws LoginException {
        try (NuxeoLoginContext ignored = Framework.loginUser(JOHN)) {
            CoreSession notAdminSession = CoreInstance.getCoreSession(session.getRepositoryName());
            // Add current user to records cleaner group.
            NuxeoPrincipal.getCurrent().setGroups(Collections.singletonList(RECORDS_CLEANER_GROUP));
            checkRemoveBlob(documentModelUnderLegalHold.getRef(), "file:content", notAdminSession, false);
            checkRemoveBlob(documentModelUnderRetention.getRef(), "file:content", notAdminSession, false);
        }
    }

    @Test
    public void iCanResetOneAttachementInGovernanceAsRecordCleaner() throws LoginException {
        try (NuxeoLoginContext ignored = Framework.loginUser(JOHN)) {
            CoreSession notAdminSession = CoreInstance.getCoreSession(session.getRepositoryName());
            // Add current user to records cleaner group.
            NuxeoPrincipal.getCurrent().setGroups(Collections.singletonList(RECORDS_CLEANER_GROUP));
            checkRemoveBlob(documentModelUnderLegalHold.getRef(), "files:files/0/file", notAdminSession, false);
            checkRemoveBlob(documentModelUnderRetention.getRef(), "files:files/0/file", notAdminSession, false);
        }
    }

    @Test
    @WithFrameworkProperty(name = PROP_RETENTION_COMPLIANCE_MODE_ENABLED, value = "true")
    public void iDoNotHaveRemovePermissionInCompliance() throws LoginException {
        assertFalse(session.hasPermission(documentModelUnderLegalHold.getRef(), REMOVE));
        assertFalse(session.hasPermission(documentModelUnderRetention.getRef(), REMOVE));
        try (NuxeoLoginContext ignored = Framework.loginUser(JOHN)) {
            CoreSession notAdminSession = CoreInstance.getCoreSession(session.getRepositoryName());
            assertFalse(notAdminSession.hasPermission(documentModelUnderLegalHold.getRef(), REMOVE));
            assertFalse(notAdminSession.hasPermission(documentModelUnderRetention.getRef(), REMOVE));
            NuxeoPrincipal.getCurrent().setGroups(Collections.singletonList(RECORDS_CLEANER_GROUP));
            assertFalse(notAdminSession.hasPermission(documentModelUnderLegalHold.getRef(), REMOVE));
            assertFalse(notAdminSession.hasPermission(documentModelUnderRetention.getRef(), REMOVE));
        }
    }

    @Test
    public void iHaveRemovePermissionInGovernance() throws LoginException {
        assertFalse(session.hasPermission(documentModelUnderLegalHold.getRef(), REMOVE));
        assertFalse(session.hasPermission(documentModelUnderRetention.getRef(), REMOVE));
        try (NuxeoLoginContext ignored = Framework.loginUser(JOHN)) {
            CoreSession notAdminSession = CoreInstance.getCoreSession(session.getRepositoryName());
            assertFalse(notAdminSession.hasPermission(documentModelUnderLegalHold.getRef(), REMOVE));
            assertFalse(notAdminSession.hasPermission(documentModelUnderRetention.getRef(), REMOVE));
            NuxeoPrincipal.getCurrent().setGroups(Collections.singletonList(RECORDS_CLEANER_GROUP));
            assertTrue(notAdminSession.hasPermission(documentModelUnderLegalHold.getRef(), REMOVE));
            assertTrue(notAdminSession.hasPermission(documentModelUnderRetention.getRef(), REMOVE));
        }
    }

    @Before
    public void setUp() {
        addPermission(session.getRootDocument(), JOHN, REMOVE_CHILDREN);
        folder = session.createDocumentModel("/", "myFolder", "Folder");
        folder = session.createDocument(folder);
        // Make a document under legal hold and another one under retention
        documentModelUnderLegalHold = createFileDocument(session, folder);
        documentModelUnderRetention = createFileDocument(session, folder);
        session.makeRecord(documentModelUnderLegalHold.getRef());
        session.makeRecord(documentModelUnderRetention.getRef());
        session.setLegalHold(documentModelUnderLegalHold.getRef(), true, null);
        Calendar retainUntil = Calendar.getInstance();
        retainUntil.add(Calendar.DAY_OF_MONTH, 5);
        session.setRetainUntil(documentModelUnderRetention.getRef(), retainUntil, "any comment");

        // Give the permission to another user
        addPermission(folder, JOHN, READ, WRITE, REMOVE);
        addPermission(documentModelUnderLegalHold, JOHN, READ, WRITE, REMOVE);
        addPermission(documentModelUnderRetention, JOHN, READ, WRITE, REMOVE);
    }

}
