/*
 * (C) Copyright 2014-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.nuxeo.ecm.core.search.BaseCoreSearchFeature.assertIndexedContains;
import static org.nuxeo.ecm.core.search.BaseCoreSearchFeature.newSearchQuery;

import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import jakarta.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.api.trash.TrashService;
import org.nuxeo.ecm.core.search.index.commands.ThreadLocalIndexingCommandsStacker;
import org.nuxeo.ecm.core.test.CoreSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features(CoreSearchFeature.class)
public class TestSearchTreeIndexing {

    @Inject
    protected CoreSession session;

    @Inject
    protected SearchService searchService;

    @Inject
    protected TransactionalFeature txFeature;

    @Before
    public void buildTree() {
        String root = "/";
        for (int i = 0; i < 10; i++) {
            String name = "folder" + i;
            DocumentModel doc = session.createDocumentModel(root, name, "Folder");
            doc.setPropertyValue("dc:title", "Folder" + i);
            session.createDocument(doc);
            root = root + name + "/";
        }
        txFeature.nextTransaction();

        // check indexing
        var searchResponse = searchAll();
        assertEquals(10, searchResponse.getTotal());
    }

    protected SearchResponse searchAll() {
        return searchAll(session);
    }

    protected SearchResponse searchAll(CoreSession coreSession) {
        var searchQuery = newSearchQuery(coreSession, "SELECT * FROM Document");
        return searchService.search(searchQuery);
    }

    @Test
    public void shouldIndexTree() {
        // check sub tree search
        SearchResponse searchResponse = searchService.search(
                newSearchQuery(session, "ecm:path STARTSWITH '/folder0/folder1/folder2'"));
        assertEquals(7, searchResponse.getTotal());
    }

    @Test
    public void shouldUnIndexSubTree() {
        DocumentRef ref = new PathRef("/folder0/folder1/folder2");
        assertTrue(session.exists(ref));

        session.removeDocument(ref);
        txFeature.nextTransaction();

        SearchResponse searchResponse = searchAll();
        assertEquals(2, searchResponse.getTotal());
    }

    @Test
    public void shouldIndexMovedSubTree() {
        DocumentRef ref = new PathRef("/folder0/folder1/folder2");
        assertTrue(session.exists(ref));
        DocumentModel doc = session.getDocument(ref);

        // move in the same folder : rename
        session.move(ref, doc.getParentRef(), "folderA");

        txFeature.nextTransaction();
        SearchResponse searchResponse = searchAll();
        assertEquals(10, searchResponse.getTotal());

        // check sub tree search
        searchResponse = searchService.search(
                newSearchQuery(session, "ecm:path STARTSWITH '/folder0/folder1/folder2'"));
        assertEquals(0, searchResponse.getTotal());

        searchResponse = searchService.search(
                newSearchQuery(session, "ecm:path STARTSWITH '/folder0/folder1/folderA'"));
        assertEquals(7, searchResponse.getTotal());

        searchResponse = searchService.search(newSearchQuery(session, "ecm:path STARTSWITH '/folder0/folder1'"));
        assertEquals(8, searchResponse.getTotal());

    }

    @Test
    public void shouldFilterTreeOnSecurity() {
        DocumentModelList docs = searchAll().loadDocuments(session);
        assertEquals(10, docs.totalSize());

        // check for user with no rights
        CoreSession restrictedSession = CoreInstance.getCoreSession(null, "toto");
        docs = searchAll(restrictedSession).loadDocuments(restrictedSession);
        assertEquals(0, docs.totalSize());

        // add READ rights and check that user now has access
        DocumentRef ref = new PathRef("/folder0/folder1/folder2");
        ACP acp = new ACPImpl();
        ACL acl = ACPImpl.newACL(ACL.LOCAL_ACL);
        acl.add(new ACE("toto", SecurityConstants.READ, true));
        acp.addACL(acl);
        session.setACP(ref, acp, true);
        txFeature.nextTransaction();

        docs = searchAll(restrictedSession).loadDocuments(restrictedSession);
        assertEquals(8, docs.totalSize());

        // block rights and check that blocking is taken into account
        ref = new PathRef("/folder0/folder1/folder2/folder3/folder4/folder5");
        acp = new ACPImpl();
        acl = ACPImpl.newACL(ACL.LOCAL_ACL);
        acl.add(new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false));
        acl.add(new ACE("Administrator", SecurityConstants.EVERYTHING, true));
        acp.addACL(acl);
        session.setACP(ref, acp, true);
        txFeature.nextTransaction();

        docs = searchAll(restrictedSession).loadDocuments(restrictedSession);
        assertEquals(3, docs.totalSize());
    }

    /**
     * Checks that an ACL change indexed asynchronously updates the index for the folder itself and not only for its
     * descendants.
     */
    @Test
    public void shouldFilterTreeOnSecurityAsync() {
        DocumentModelList docs = searchAll().loadDocuments(session);
        assertEquals(10, docs.totalSize());

        // check for user with no rights
        CoreSession restrictedSession = CoreInstance.getCoreSession(null, "toto");
        docs = searchAll(restrictedSession).loadDocuments(restrictedSession);
        assertEquals(0, docs.totalSize());

        // add READ rights on a folder using asynchronous indexing
        DocumentRef ref = new PathRef("/folder0/folder1/folder2");
        ACP acp = new ACPImpl();
        ACL acl = ACPImpl.newACL(ACL.LOCAL_ACL);
        acl.add(new ACE("toto", SecurityConstants.READ, true));
        acp.addACL(acl);
        boolean syncIndexing = ThreadLocalIndexingCommandsStacker.useSyncIndexing.get();
        ThreadLocalIndexingCommandsStacker.useSyncIndexing.set(false);
        try {
            session.setACP(ref, acp, true);
        } finally {
            ThreadLocalIndexingCommandsStacker.useSyncIndexing.set(syncIndexing);
        }
        txFeature.nextTransaction();

        // the folder itself must be reindexed, not only its descendants
        DocumentModel folder = session.getDocument(ref);
        assertIndexedContains(folder.getId(), "toto");

        // the user can see the folder and its descendants
        docs = searchAll(restrictedSession).loadDocuments(restrictedSession);
        assertEquals(8, docs.totalSize());
    }

    @Test
    public void shouldDenyAccessOnUnsupportedACL() {
        assumeTrue(session.isNegativeAclAllowed());

        DocumentModelList docs = searchAll().loadDocuments(session);
        assertEquals(10, docs.totalSize());

        // check for user with no rights
        CoreSession restrictedSession = CoreInstance.getCoreSession(null, "toto");
        docs = searchAll(restrictedSession).loadDocuments(restrictedSession);
        assertEquals(0, docs.totalSize());

        // add READ rights and check that user now has access
        DocumentRef ref = new PathRef("/folder0/folder1/folder2");
        ACP acp = new ACPImpl();
        ACL acl = ACPImpl.newACL(ACL.LOCAL_ACL);
        acl.add(new ACE("toto", SecurityConstants.READ, true));
        acp.addACL(acl);
        session.setACP(ref, acp, true);
        txFeature.nextTransaction();

        docs = searchAll(restrictedSession).loadDocuments(restrictedSession);
        assertEquals(8, docs.totalSize());

        // Add an unsupported negative ACL
        ref = new PathRef("/folder0/folder1/folder2/folder3/folder4/folder5");
        acp = new ACPImpl();
        acl = ACPImpl.newACL(ACL.LOCAL_ACL);
        acl.add(new ACE("bob", SecurityConstants.EVERYTHING, false));

        acp.addACL(acl);
        session.setACP(ref, acp, true);
        txFeature.nextTransaction();

        docs = searchAll(restrictedSession).loadDocuments(restrictedSession);
        // can view folder2, folder3 and folder4
        assertEquals(3, docs.totalSize());
    }

    @Test
    public void shouldStoreOnlyEffectiveACEs() {
        DocumentModelList docs = searchAll().loadDocuments(session);
        assertEquals(10, docs.totalSize());

        CoreSession restrictedSession = CoreInstance.getCoreSession(null, "toto");
        docs = searchAll(restrictedSession).loadDocuments(restrictedSession);
        assertEquals(0, docs.totalSize());

        DocumentRef ref = new PathRef("/folder0");
        ACP acp = new ACPImpl();
        ACL acl = ACPImpl.newACL(ACL.LOCAL_ACL);
        acl.add(ACE.builder("toto", SecurityConstants.READ).build());
        acp.addACL(acl);
        session.setACP(ref, acp, true);
        txFeature.nextTransaction();

        docs = searchAll(restrictedSession).loadDocuments(restrictedSession);
        assertEquals(10, docs.totalSize());

        acp = new ACPImpl();
        acl = ACPImpl.newACL(ACL.LOCAL_ACL);
        // make the ACE archived
        Date now = new Date();
        Calendar begin = new GregorianCalendar();
        begin.setTimeInMillis(now.toInstant().minus(10, ChronoUnit.DAYS).toEpochMilli());
        Calendar end = new GregorianCalendar();
        end.setTimeInMillis(now.toInstant().minus(2, ChronoUnit.DAYS).toEpochMilli());
        acl.add(ACE.builder("toto", SecurityConstants.READ).begin(begin).end(end).build());
        acp.addACL(acl);
        session.setACP(ref, acp, true);
        txFeature.nextTransaction();

        docs = searchAll(restrictedSession).loadDocuments(restrictedSession);
        assertEquals(0, docs.totalSize());
    }

    @Test
    public void shouldReindexSubTreeInTrash() {
        DocumentRef ref = new PathRef("/folder0/folder1/folder2");
        assertTrue(session.exists(ref));
        Framework.getService(TrashService.class).trashDocument(session.getDocument(ref));

        // let BAF do its work
        txFeature.nextTransaction();

        DocumentModelList docs = searchService.search(newSearchQuery(session, "ecm:isTrashed = 0"))
                                              .loadDocuments(session);
        assertEquals(2, docs.totalSize());
    }

    @Test
    public void shouldIndexOnCopy() {
        DocumentRef src = new PathRef("/folder0/folder1/folder2");
        DocumentRef dst = new PathRef("/folder0");
        session.copy(src, dst, "folder2-copy");

        txFeature.nextTransaction();

        DocumentModelList docs = searchAll().loadDocuments(session);
        assertEquals(18, docs.totalSize());
    }

}
