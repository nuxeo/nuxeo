
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

package org.nuxeo.ecm.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.EVERYONE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.EVERYTHING;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.RECORDS_CLEANER_GROUP;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.REMOVE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.REMOVE_CHILDREN;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE;
import static org.nuxeo.ecm.core.model.Session.PROP_RETENTION_COMPLIANCE_MODE_ENABLED;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentExistsException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.PermissionProvider;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * @since 11.5
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
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

    protected void blockPermissionInheritance(DocumentModel documentModel) {
        ACP acp = documentModel.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE(EVERYONE, EVERYTHING, false));
        session.setACP(documentModel.getRef(), acp, false);
        session.save();
    }

    protected void checkRemoveBlob(DocumentModel doc, CoreSession session, boolean expectedToFail) {
        try {
            doc.setPropertyValue("file:content", null);
            session.saveDocument(doc);
            if (expectedToFail) {
                fail();
            }
        } catch (DocumentSecurityException e) {
            if (expectedToFail) {
                assertEquals(String.format("Cannot delete blob from document %s, it is under retention / hold",
                        doc.getRef()), e.getMessage());
            } else {
                throw e;
            }
        }
    }

    protected void checkRemoveDocument(DocumentModel doc, CoreSession session, boolean expectedToFail) {
        try {
            session.removeDocument(doc.getRef());
            if (expectedToFail) {
                fail();
            }
        } catch (DocumentExistsException e) {
            if (expectedToFail) {
                String patternStr = String.format("Cannot remove %s, subdocument \\S+ is under retention / hold",
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
                        doc.getRef(), doc.getRef()), e.getMessage());
            } else {
                throw e;
            }
        }
    }

    protected DocumentModel createFileDocument(CoreSession session, DocumentModel parent) {
        DocumentModel documentModel = session.createDocumentModel(parent.getPathAsString(), "myDocument", "File");
        Blob blob = Blobs.createBlob("Any Content");
        documentModel.setPropertyValue("file:content", (Serializable) blob);
        return session.createDocument(documentModel);
    }

    @Test
    @WithFrameworkProperty(name = PROP_RETENTION_COMPLIANCE_MODE_ENABLED, value = "true")
    public void iCannotRemoveFolderWithRetainedChildrenInCompliance() throws LoginException {
        LoginContext ctx = Framework.loginAsUser(JOHN);
        try (CloseableCoreSession notAdminSession = CoreInstance.openCoreSession(session.getRepositoryName())) {
            folder = notAdminSession.getDocument(folder.getRef());
            checkRemoveDocument(folder, notAdminSession, true);

            // Add current user to records cleaner group.
            ClientLoginModule.getCurrentPrincipal().setGroups(Collections.singletonList(RECORDS_CLEANER_GROUP));

            folder = notAdminSession.getDocument(folder.getRef());
            checkRemoveDocument(folder, notAdminSession, true);
        } finally {
            ctx.logout();
        }
    }

    @Test
    @WithFrameworkProperty(name = PROP_RETENTION_COMPLIANCE_MODE_ENABLED, value = "true")
    public void iCannotRemoveRetainedDocInCompliance() throws LoginException {
        LoginContext ctx = Framework.loginAsUser(JOHN);
        try (CloseableCoreSession notAdminSession = CoreInstance.openCoreSession(session.getRepositoryName())) {
            documentModelUnderLegalHold = notAdminSession.getDocument(documentModelUnderLegalHold.getRef());
            documentModelUnderRetention = notAdminSession.getDocument(documentModelUnderRetention.getRef());
            checkRemoveBlob(documentModelUnderLegalHold, notAdminSession, true);
            checkRemoveBlob(documentModelUnderRetention, notAdminSession, true);
            checkRemoveDocument(documentModelUnderLegalHold, notAdminSession, true);
            checkRemoveDocument(documentModelUnderRetention, notAdminSession, true);

            // Add current use to record cleaner group.
            ClientLoginModule.getCurrentPrincipal().setGroups(Collections.singletonList(RECORDS_CLEANER_GROUP));

            documentModelUnderLegalHold = notAdminSession.getDocument(documentModelUnderLegalHold.getRef());
            documentModelUnderRetention = notAdminSession.getDocument(documentModelUnderRetention.getRef());
            checkRemoveBlob(documentModelUnderLegalHold, notAdminSession, true);
            checkRemoveBlob(documentModelUnderRetention, notAdminSession, true);
            checkRemoveDocument(documentModelUnderLegalHold, notAdminSession, true);
            checkRemoveDocument(documentModelUnderRetention, notAdminSession, true);
        } finally {
            ctx.logout();
        }
    }

    @Test
    public void iCanRemoveFolderWithRetainedChildrenInGovernance() throws LoginException {
        LoginContext ctx = Framework.loginAsUser(JOHN);
        try (CloseableCoreSession notAdminSession = CoreInstance.openCoreSession(session.getRepositoryName())) {
            folder = notAdminSession.getDocument(folder.getRef());
            checkRemoveDocument(folder, notAdminSession, true);

            // Add current user to records cleaner group.
            ClientLoginModule.getCurrentPrincipal().setGroups(Collections.singletonList(RECORDS_CLEANER_GROUP));

            folder = notAdminSession.getDocument(folder.getRef());
            checkRemoveDocument(folder, notAdminSession, false);
        } finally {
            ctx.logout();
        }
    }

    @Test
    public void iCanRemoveRetainedDocInGovernance() throws LoginException {
        LoginContext ctx = Framework.loginAsUser(JOHN);
        try (CloseableCoreSession notAdminSession = CoreInstance.openCoreSession(session.getRepositoryName())) {
            documentModelUnderLegalHold = notAdminSession.getDocument(documentModelUnderLegalHold.getRef());
            documentModelUnderRetention = notAdminSession.getDocument(documentModelUnderRetention.getRef());
            checkRemoveBlob(documentModelUnderLegalHold, notAdminSession, true);
            checkRemoveBlob(documentModelUnderRetention, notAdminSession, true);
            checkRemoveDocument(documentModelUnderLegalHold, notAdminSession, true);
            checkRemoveDocument(documentModelUnderRetention, notAdminSession, true);

            // Add current user to records cleaner group.
            ClientLoginModule.getCurrentPrincipal().setGroups(Collections.singletonList(RECORDS_CLEANER_GROUP));

            documentModelUnderLegalHold = notAdminSession.getDocument(documentModelUnderLegalHold.getRef());
            documentModelUnderRetention = notAdminSession.getDocument(documentModelUnderRetention.getRef());
            checkRemoveBlob(documentModelUnderLegalHold, notAdminSession, false);
            checkRemoveBlob(documentModelUnderRetention, notAdminSession, false);
            checkRemoveDocument(documentModelUnderLegalHold, notAdminSession, false);
            checkRemoveDocument(documentModelUnderRetention, notAdminSession, false);
        } finally {
            ctx.logout();
        }
    }

    @Test
    @WithFrameworkProperty(name = PROP_RETENTION_COMPLIANCE_MODE_ENABLED, value = "true")
    public void iDoNotHaveRemovePermissionInCompliance() throws LoginException {
        assertFalse(session.hasPermission(documentModelUnderLegalHold.getRef(), REMOVE));
        assertFalse(session.hasPermission(documentModelUnderRetention.getRef(), REMOVE));
        // check that other permissions are still granted to admin
        List<String> allPermissions = new ArrayList<String>(
                Arrays.asList(Framework.getService(PermissionProvider.class).getPermissions()));
        Collection<String> adminPermissions = session.filterGrantedPermissions(session.getPrincipal(),
                documentModelUnderLegalHold.getRef(), allPermissions);
        assertTrue(adminPermissions.contains(READ));
        assertTrue(adminPermissions.contains(WRITE));
        assertFalse(adminPermissions.contains(REMOVE));

        LoginContext ctx = Framework.loginAsUser(JOHN);
        try (CloseableCoreSession notAdminSession = CoreInstance.openCoreSession(session.getRepositoryName())) {
            assertFalse(notAdminSession.hasPermission(documentModelUnderLegalHold.getRef(), REMOVE));
            assertFalse(notAdminSession.hasPermission(documentModelUnderRetention.getRef(), REMOVE));
            ClientLoginModule.getCurrentPrincipal().setGroups(Collections.singletonList(RECORDS_CLEANER_GROUP));
            assertFalse(notAdminSession.hasPermission(documentModelUnderLegalHold.getRef(), REMOVE));
            assertFalse(notAdminSession.hasPermission(documentModelUnderRetention.getRef(), REMOVE));
        } finally {
            ctx.logout();
        }
    }

    @Test
    public void iHaveRemovePermissionInGovernance() throws LoginException {
        assertFalse(session.hasPermission(documentModelUnderLegalHold.getRef(), REMOVE));
        assertFalse(session.hasPermission(documentModelUnderRetention.getRef(), REMOVE));
        LoginContext ctx = Framework.loginAsUser(JOHN);
        try (CloseableCoreSession notAdminSession = CoreInstance.openCoreSession(session.getRepositoryName())) {
            assertFalse(notAdminSession.hasPermission(documentModelUnderLegalHold.getRef(), REMOVE));
            assertFalse(notAdminSession.hasPermission(documentModelUnderRetention.getRef(), REMOVE));
            ClientLoginModule.getCurrentPrincipal().setGroups(Collections.singletonList(RECORDS_CLEANER_GROUP));
            assertTrue(notAdminSession.hasPermission(documentModelUnderLegalHold.getRef(), REMOVE));
            assertTrue(notAdminSession.hasPermission(documentModelUnderRetention.getRef(), REMOVE));
        } finally {
            ctx.logout();
        }
    }

    @Before
    public void setUp() {
        addPermission(session.getRootDocument(), JOHN, REMOVE_CHILDREN);
        folder = session.createDocumentModel("/", "myFolder", "Folder");
        folder = session.createDocument(folder);
        blockPermissionInheritance(folder);
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