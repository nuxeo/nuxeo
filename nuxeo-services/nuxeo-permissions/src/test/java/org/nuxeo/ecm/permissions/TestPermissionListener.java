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
 *     Thomas Roger
 */

package org.nuxeo.ecm.permissions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.EVERYTHING;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE;
import static org.nuxeo.ecm.permissions.Constants.ACE_INFO_DIRECTORY;
import static org.nuxeo.ecm.permissions.Constants.COMMENT_KEY;
import static org.nuxeo.ecm.permissions.Constants.NOTIFY_KEY;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import com.google.inject.Inject;

/**
 * @since 7.4
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, PlatformFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.permissions")
public class TestPermissionListener {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected DirectoryService directoryService;

    protected LoginContext loginContext;

    protected void login(String username) throws LoginException {
        loginContext = Framework.login(username, username);
    }

    protected void logout() throws LoginException {
        loginContext.logout();
    }

    @Test
    public void shouldFillDirectory() {
        DocumentModel doc = createTestDocument();

        ACE fryACE = new ACE("fry", WRITE, true);
        ACE leelaACE = new ACE("leela", READ, true);
        ACP acp = doc.getACP();
        acp.addACE(ACL.LOCAL_ACL, fryACE);
        acp.addACE(ACL.LOCAL_ACL, leelaACE);
        doc.setACP(acp, true);

        try (Session session = directoryService.open(ACE_INFO_DIRECTORY)) {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("docId", doc.getId());
            DocumentModelList entries = session.query(filter);
            assertEquals(2, entries.size());

            String id = PermissionHelper.computeDirectoryId(doc, ACL.LOCAL_ACL, fryACE.getId());
            DocumentModel entry = session.getEntry(id);
            assertEquals(doc.getRepositoryName(), entry.getPropertyValue("aceinfo:repositoryName"));
            assertEquals("local", entry.getPropertyValue("aceinfo:aclName"));
            assertEquals(fryACE.getId(), entry.getPropertyValue("aceinfo:aceId"));

            id = PermissionHelper.computeDirectoryId(doc, ACL.LOCAL_ACL, leelaACE.getId());
            entry = session.getEntry(id);
            assertEquals(doc.getRepositoryName(), entry.getPropertyValue("aceinfo:repositoryName"));
            assertEquals("local", entry.getPropertyValue("aceinfo:aclName"));
            assertEquals(leelaACE.getId(), entry.getPropertyValue("aceinfo:aceId"));
        }
    }

    protected DocumentModel createTestDocument() {
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        return session.createDocument(doc);
    }

    @Test
    public void shouldUpdateDirectory() throws Exception {
        String repositoryName = session.getRepositoryName();
        String docId;
        String fryACEId;

        DocumentModel root = session.getRootDocument();
        ACP rootACP = root.getACP();
        rootACP.addACE(ACL.LOCAL_ACL, new ACE("joe", EVERYTHING, true));
        root.setACP(rootACP, true);

        login("joe");
        try (CloseableCoreSession joeSession = CoreInstance.openCoreSession(repositoryName)) {
            DocumentModel doc = joeSession.createDocumentModel("/", "file", "File");
            doc = joeSession.createDocument(doc);
            docId = doc.getId();

            ACE fryACE = new ACE("fry", WRITE, true);
            ACE leelaACE = new ACE("leela", READ, true);
            ACP acp = doc.getACP();
            acp.addACE(ACL.LOCAL_ACL, fryACE);
            acp.addACE(ACL.LOCAL_ACL, leelaACE);
            doc.setACP(acp, true);

            acp = doc.getACP();
            acp.removeACE(ACL.LOCAL_ACL, leelaACE);
            acp.removeACE(ACL.LOCAL_ACL, fryACE);
            fryACE = new ACE("fry", READ, true);
            acp.addACE(ACL.LOCAL_ACL, fryACE);
            doc.setACP(acp, true);
            fryACEId = fryACE.getId();
        } finally {
            logout();
        }

        try (Session session = directoryService.open(ACE_INFO_DIRECTORY)) {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("docId", docId);
            DocumentModelList entries = session.query(filter);
            assertEquals(1, entries.size());

            DocumentModel entry = entries.get(0);
            assertEquals(repositoryName, entry.getPropertyValue("aceinfo:repositoryName"));
            assertEquals("local", entry.getPropertyValue("aceinfo:aclName"));
            assertEquals(fryACEId, entry.getPropertyValue("aceinfo:aceId"));
        }

        login("joe");
        try (CloseableCoreSession joeSession = CoreInstance.openCoreSession(repositoryName)) {
            DocumentModel doc = joeSession.getDocument(new IdRef(docId));
            ACP acp = doc.getACP();
            ACL acl = acp.getOrCreateACL();
            acl.clear();
            acp.addACL(acl);
            doc.setACP(acp, true);
        } finally {
            logout();
        }

        try (Session session = directoryService.open(ACE_INFO_DIRECTORY)) {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("docId", docId);
            DocumentModelList entries = session.query(filter);
            assertTrue(entries.isEmpty());
        }
    }

    @Test
    public void shouldStoreNotifyAndComment() {
        DocumentModel doc = createTestDocument();

        ACP acp = doc.getACP();
        ACL acl = acp.getOrCreateACL();
        ACE fryACE = new ACE("fry", WRITE, true);
        fryACE.putContextData(NOTIFY_KEY, true);
        fryACE.putContextData(COMMENT_KEY, "fry comment");
        ACE leelaACE = new ACE("leela", READ, true);
        acl.add(fryACE);
        acl.add(leelaACE);
        acp.addACL(acl);
        doc.setACP(acp, true);

        try (Session session = directoryService.open(ACE_INFO_DIRECTORY)) {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("docId", doc.getId());
            DocumentModelList entries = session.query(filter);
            assertEquals(2, entries.size());

            String id = PermissionHelper.computeDirectoryId(doc, ACL.LOCAL_ACL, fryACE.getId());
            DocumentModel entry = session.getEntry(id);
            assertEquals(fryACE.getId(), entry.getPropertyValue("aceinfo:aceId"));
            assertTrue((Boolean) entry.getPropertyValue("aceinfo:notify"));
            assertEquals("fry comment", entry.getPropertyValue("aceinfo:comment"));

            id = PermissionHelper.computeDirectoryId(doc, ACL.LOCAL_ACL, leelaACE.getId());
            entry = session.getEntry(id);
            assertEquals(leelaACE.getId(), entry.getPropertyValue("aceinfo:aceId"));
            assertFalse((Boolean) entry.getPropertyValue("aceinfo:notify"));
            assertNull(entry.getPropertyValue("aceinfo:comment"));
        }
    }

    @Test
    public void replacingAnACEShouldReplaceTheOldEntry() {
        DocumentModel doc = createTestDocument();

        ACP acp = doc.getACP();
        ACL acl = acp.getOrCreateACL();
        ACE fryACE = new ACE("fry", WRITE, true);
        fryACE.putContextData(NOTIFY_KEY, true);
        fryACE.putContextData(COMMENT_KEY, "fry comment");
        acl.add(fryACE);
        acp.addACL(acl);
        doc.setACP(acp, true);

        try (Session dirSession = directoryService.open(ACE_INFO_DIRECTORY)) {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("docId", doc.getId());
            DocumentModelList entries = dirSession.query(filter);
            assertEquals(1, entries.size());

            String id = PermissionHelper.computeDirectoryId(doc, ACL.LOCAL_ACL, fryACE.getId());
            DocumentModel entry = dirSession.getEntry(id);
            assertEquals(fryACE.getId(), entry.getPropertyValue("aceinfo:aceId"));
            assertTrue((Boolean) entry.getPropertyValue("aceinfo:notify"));
            assertEquals("fry comment", entry.getPropertyValue("aceinfo:comment"));
        }

        // replacing the ACE for fry
        ACE newFryACE = ACE.builder("fry", READ).build();
        session.replaceACE(doc.getRef(), ACL.LOCAL_ACL, fryACE, newFryACE);
        try (Session dirSession = directoryService.open(ACE_INFO_DIRECTORY)) {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("docId", doc.getId());
            DocumentModelList entries = dirSession.query(filter);
            assertEquals(1, entries.size());

            String id = PermissionHelper.computeDirectoryId(doc, ACL.LOCAL_ACL, newFryACE.getId());
            DocumentModel entry = dirSession.getEntry(id);
            assertEquals(newFryACE.getId(), entry.getPropertyValue("aceinfo:aceId"));
            assertFalse((Boolean) entry.getPropertyValue("aceinfo:notify"));
            assertNull(entry.getPropertyValue("aceinfo:comment"));
        }

        ACE newFryACE2 = ACE.builder("fry", WRITE).build();
        newFryACE2.putContextData(NOTIFY_KEY, true);
        session.replaceACE(doc.getRef(), ACL.LOCAL_ACL, newFryACE, newFryACE2);
        try (Session dirSession = directoryService.open(ACE_INFO_DIRECTORY)) {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("docId", doc.getId());
            DocumentModelList entries = dirSession.query(filter);
            assertEquals(1, entries.size());

            String id = PermissionHelper.computeDirectoryId(doc, ACL.LOCAL_ACL, newFryACE2.getId());
            DocumentModel entry = dirSession.getEntry(id);
            assertEquals(newFryACE2.getId(), entry.getPropertyValue("aceinfo:aceId"));
            assertTrue((Boolean) entry.getPropertyValue("aceinfo:notify"));
            assertNull(entry.getPropertyValue("aceinfo:comment"));
        }
    }
}
