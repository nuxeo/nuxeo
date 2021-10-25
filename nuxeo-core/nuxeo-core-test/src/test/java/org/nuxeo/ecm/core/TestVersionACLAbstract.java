/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.BROWSE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ_WRITE;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.model.BaseSession.VersionAclMode;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.ecm.core.storage.dbs.DBSTransactionState;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public abstract class TestVersionACLAbstract {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected SecurityService securityService;

    protected abstract VersionAclMode getVersionAclMode();

    protected abstract boolean isReadVersionPermissionEnabled();

    @Test
    public void testVersionACL() {
        VersionAclMode mode = getVersionAclMode();
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        ACP acp = new ACPImpl();
        acp.addACE("acl1", ACE.BLOCK);
        acp.addACE("acl1", new ACE("user1", "Read"));
        session.setACP(folder.getRef(), acp, true);

        DocumentModel file = session.createDocumentModel("/folder", "file", "File");
        file = session.createDocument(file);
        acp = new ACPImpl();
        acp.addACE("acl2", new ACE("user2", "Read"));
        session.setACP(file.getRef(), acp, true);

        // create a version
        DocumentRef verRef = session.checkIn(file.getRef(), VersioningOption.MINOR, null);
        String verId = session.getDocument(verRef).getId();
        // add ACL on version itself
        acp = new ACPImpl();
        acp.addACE("acl3", new ACE("user3", "Read"));
        session.setACP(verRef, acp, true);

        // create a proxy pointing to the version
        session.createProxy(verRef, folder.getRef());
        session.save();
        coreFeature.waitForAsyncCompletion(); // DBS read ACL computation is async

        // check ACLs on the version
        acp = session.getACP(verRef);
        List<ACE> aces = acpToAces(acp);
        assertEquals(mode == VersionAclMode.ENABLED ? 4 : 3, aces.size());
        Iterator<ACE> aceit = aces.iterator();
        if (mode == VersionAclMode.ENABLED) {
            assertEquals("user3", aceit.next().getUsername());
        }
        assertEquals("user2", aceit.next().getUsername());
        assertEquals("user1", aceit.next().getUsername());
        assertEquals(ACE.BLOCK, aceit.next());

        // check Browse permission on the ACL
        assertCanBrowse(false, acp, "nosuchuser");
        assertCanBrowse(true, acp, "user1");
        assertCanBrowse(true, acp, "user2");
        assertCanBrowse(mode == VersionAclMode.ENABLED, acp, "user3");

        // check Browse permission using CoreSession document API
        assertCanBrowse(false, verRef, "nosuchuser");
        assertCanBrowse(true, verRef, "user1");
        assertCanBrowse(true, verRef, "user2");
        assertCanBrowse(mode == VersionAclMode.ENABLED, verRef, "user3");

        // check Browse permission using CoreSession query API
        assertCanQuery(false, verId, "nosuchuser");
        assertCanQuery(true, verId, "user1");
        assertCanQuery(true, verId, "user2");
        assertCanQuery(mode != VersionAclMode.DISABLED, verId, "user3");

        // delete live document
        // the version stays because of the proxy (and orphan version removal is async anyway)
        session.removeDocument(file.getRef());
        session.save();
        coreFeature.waitForAsyncCompletion(); // DBS read ACL computation is async

        // check ACLs on the version
        acp = session.getACP(verRef);
        if (mode == VersionAclMode.ENABLED) {
            aces = acpToAces(acp);
            assertEquals(1, aces.size());
            assertEquals("user3", aces.get(0).getUsername());

            // check Browse permission on the ACL
            assertCanBrowse(false, acp, "nosuchuser");
            assertCanBrowse(false, acp, "user1");
            assertCanBrowse(false, acp, "user2");
            assertCanBrowse(true, acp, "user3");
        } else {
            assertNull(acp);
        }

        // check Browse permission using CoreSession document API
        assertCanBrowse(false, verRef, "nosuchuser");
        assertCanBrowse(false, verRef, "user1");
        assertCanBrowse(false, verRef, "user2");
        assertCanBrowse(mode == VersionAclMode.ENABLED, verRef, "user3");

        // check Browse permission using CoreSession query API
        assertCanQuery(false, verId, "nosuchuser");
        // the current implementation cannot recompute read acls on versions when a live doc is deleted
        // assertCanQuery(false, verId, "user1")
        // assertCanQuery(false, verId, "user2")
        assertCanQuery(mode != VersionAclMode.DISABLED, verId, "user3");
    }

    @Test
    public void testReadVersionPermissionOnDocument() {
        doTestReadVersionPermission(true);
    }

    @Test
    public void testReadVersionPermissionOnFolder() {
        doTestReadVersionPermission(false);
    }

    @Test
    public void isReadACLUpdatedOnVersion() {
        testReadACLOnVersions();
    }

    @Test
    @WithFrameworkProperty(name = DBSTransactionState.READ_ACL_ASYNC_THRESHOLD_PROPERTY, value = "10")
    public void isFatReadACLUpdatedOnVersion() {
        testReadACLOnVersions();
    }

    protected void testReadACLOnVersions() {
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        // Give access to the data structure to user1
        setPermission(folder, "user1", READ_WRITE);

        Set<String> versionIds = new HashSet<>();
        try (CloseableCoreSession user1Session = CoreInstance.openCoreSession(session.getRepositoryName(), "user1")) {
            // Check in level 1
            for (int i = 0; i < 5; i++) {
                versionIds.add(versionDocument(user1Session, "/folder", "file" + i, "File"));
            }
            // Check in level 2
            versionIds.add(versionDocument(user1Session, "/folder", "subfolder", "Folder"));
            versionIds.add(versionDocument(user1Session, "/folder/subfolder", "file", "File"));
        }

        // user1 can find the versions
        versionIds.forEach(id -> assertCanQuery(true, id, "user1"));
        // user2 cannot find the versions yet
        versionIds.forEach(id -> assertCanQuery(false, id, "user2"));

        // Give access to the data structure to user2
        setPermission(folder, "user2", READ);
        coreFeature.waitForAsyncCompletion();

        // user2 can also find the versions even if they were checked in before he gets access to the live documents
        versionIds.forEach(id -> assertCanQuery(true, id, "user2"));
    }

    protected String versionDocument(CoreSession coreSession, String path, String name, String type) {
        DocumentModel file = coreSession.createDocumentModel(path, name, type);
        file = coreSession.createDocument(file);
        DocumentRef versionRef = coreSession.checkIn(file.getRef(), VersioningOption.MINOR, null);
        return coreSession.getDocument(versionRef).getId();
    }

    protected void setPermission(DocumentModel doc, String user, String permission) {
        ACP acp = doc.getACP();
        ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
        ACE ace = new ACE(user, permission);
        localACL.add(ace);
        doc.setACP(acp, true);
    }

    protected void doTestReadVersionPermission(boolean aclOnDocument) {
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);

        DocumentModel file = session.createDocumentModel("/folder", "file", "File");
        file = session.createDocument(file);

        DocumentModel aclCarrier = aclOnDocument ? file : folder;
        ACP acp = new ACPImpl();
        acp.addACE("acl1", ACE.BLOCK);
        acp.addACE("acl1", new ACE("user1", "ReadVersion"));
        session.setACP(aclCarrier.getRef(), acp, true);

        // create a version
        DocumentRef verRef = session.checkIn(file.getRef(), VersioningOption.MINOR, null);
        String verId = session.getDocument(verRef).getId();

        // create a proxy pointing to the version
        session.createProxy(verRef, folder.getRef());
        session.save();
        coreFeature.waitForAsyncCompletion(); // DBS read ACL computation is async

        // check ACLs on the version
        acp = session.getACP(verRef);
        List<ACE> aces = acpToAces(acp);
        assertEquals(2, aces.size());
        assertEquals("user1", aces.get(0).getUsername());
        assertEquals(ACE.BLOCK, aces.get(1));

        // check Browse permission on the ACL
        assertCanBrowse(false, acp, "nosuchuser");
        assertCanBrowse(isReadVersionPermissionEnabled(), acp, "user1");

        // check Browse permission using CoreSession document API
        assertCanBrowse(false, verRef, "nosuchuser");
        assertCanBrowse(isReadVersionPermissionEnabled(), verRef, "user1");

        // check Browse permission using CoreSession query API
        assertCanQuery(false, verId, "nosuchuser");
        boolean expected = isReadVersionPermissionEnabled();
        if (getVersionAclMode() == VersionAclMode.LEGACY && aclOnDocument
                && coreFeature.getStorageConfiguration().isVCS()) {
            // legacy mode on VCS XXX
            expected = false;
        }
        assertCanQuery(expected, verId, "user1");

        // delete live document
        // the version stays because of the proxy (and orphan version removal is async anyway)
        session.removeDocument(file.getRef());
        session.save();
        coreFeature.waitForAsyncCompletion(); // DBS read ACL computation is async

        // check ACLs on the version
        acp = session.getACP(verRef);
        aces = acpToAces(acp);
        assertTrue(aces.isEmpty());

        // check Browse permission on the ACL
        assertCanBrowse(false, acp, "nosuchuser");
        assertCanBrowse(false, acp, "user1");

        // check Browse permission using CoreSession document API
        assertCanBrowse(false, verRef, "nosuchuser");
        assertCanBrowse(false, verRef, "user1");

        // check Browse permission using CoreSession query API
        assertCanQuery(false, verId, "nosuchuser");
        // the current implementation cannot recompute read acls on versions when a live doc is deleted
        // assertCanQuery(false, verId, "user1")
    }

    protected static List<ACE> acpToAces(ACP acp) {
        if (acp == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(acp.getACLs()).flatMap(ACL::stream).collect(toList());
    }

    protected void assertCanBrowse(boolean expected, ACP acp, String user) {
        String[] browsePermissions = securityService.getPermissionsToCheck(BROWSE);
        boolean access = acp != null && acp.getAccess(new String[] { user }, browsePermissions) == Access.GRANT;
        assertEquals(expected, access);
    }

    protected void assertCanBrowse(boolean expected, DocumentRef docRef, String user) {
        try (CloseableCoreSession userSession = CoreInstance.openCoreSession(session.getRepositoryName(), user)) {
            assertEquals(expected, userSession.exists(docRef));
        }
    }

    protected void assertCanQuery(boolean expected, String docId, String user) {
        try (CloseableCoreSession userSession = CoreInstance.openCoreSession(session.getRepositoryName(), user)) {
            DocumentModelList res = userSession.query(
                    String.format("SELECT * FROM Document WHERE ecm:uuid = '%s'", docId));
            // first check the pure query, without accessing the document
            int size = res.size();
            assertTrue(String.valueOf(size), size <= 1);
            assertEquals(expected, size == 1);
        }
    }

}
