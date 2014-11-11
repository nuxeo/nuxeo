/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bdelbosc
 */
package org.nuxeo.elasticsearch.test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.listener.ElasticSearchInlineListener;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@LocalDeploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
public class TestTreeIndexing {

    private static final String IDX_NAME = "nxutest";

    private static final String TYPE_NAME = "doc";

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    ElasticSearchAdmin esa;

    private int commandProcessed;
    private boolean syncMode = false;

    public void startCountingCommandProcessed() {
        Assert.assertEquals(0, esa.getPendingCommands());
        Assert.assertEquals(0, esa.getPendingDocs());
        commandProcessed = esa.getTotalCommandProcessed();
    }

    public void assertNumberOfCommandProcessed(int processed) throws Exception {
        Assert.assertEquals(processed, esa.getTotalCommandProcessed()
                - commandProcessed);
    }

    /**
     * Wait for sync and async job and refresh the index
     */
    public void waitForIndexing() throws Exception {
        for (int i = 0; (i < 100) && esa.isIndexingInProgress(); i++) {
            Thread.sleep(100);
        }
        Assert.assertFalse("Strill indexing in progress",
                esa.isIndexingInProgress());
        esa.refresh();
    }

    public void activateSynchronousMode() throws Exception {
        ElasticSearchInlineListener.useSyncIndexing.set(true);
        syncMode = true;
    }

    @After
    public void disableSynchronousMode() {
        ElasticSearchInlineListener.useSyncIndexing.set(false);
        syncMode = false;
    }

    public void startTransaction() {
        if (syncMode) {
            ElasticSearchInlineListener.useSyncIndexing.set(true);
        }
        if (!TransactionHelper.isTransactionActive()) {
            TransactionHelper.startTransaction();
        }
    }

    protected void buildTree() throws ClientException {
        String root = "/";
        for (int i = 0; i < 10; i++) {
            String name = "folder" + i;
            DocumentModel doc = session.createDocumentModel(root, name,
                    "Folder");
            doc.setPropertyValue("dc:title", "Folder" + i);
            session.createDocument(doc);
            root = root + name + "/";
        }
    }

    protected void buildAndIndexTree() throws Exception {
        if (!TransactionHelper.isTransactionActive()) {
            TransactionHelper.startTransaction();
        }

        startCountingCommandProcessed();
        buildTree();
        TransactionHelper.commitOrRollbackTransaction();
        waitForIndexing();
        assertNumberOfCommandProcessed(10);

        startTransaction();
        // check indexing at ES level
        SearchResponse searchResponse = esa.getClient().prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setFrom(0)
                .setSize(60).execute().actionGet();
        Assert.assertEquals(10, searchResponse.getHits().getTotalHits());
    }

    @Test
    public void shouldIndexTree() throws Exception {
        buildAndIndexTree();

        // check sub tree search
        SearchResponse searchResponse = esa
                .getClient()
                .prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(
                        QueryBuilders.prefixQuery("ecm:path",
                                "/folder0/folder1/folder2")).execute()
                .actionGet();
        Assert.assertEquals(8, searchResponse.getHits().getTotalHits());
    }

    @Test
    public void shouldUnIndexSubTree() throws Exception {
        buildAndIndexTree();

        DocumentRef ref = new PathRef("/folder0/folder1/folder2");
        Assert.assertTrue(session.exists(ref));

        startCountingCommandProcessed();
        session.removeDocument(ref);
        TransactionHelper.commitOrRollbackTransaction();
        waitForIndexing();
        assertNumberOfCommandProcessed(1);

        startTransaction();
        SearchResponse searchResponse = esa.getClient().prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setFrom(0)
                .setSize(60).execute().actionGet();
        Assert.assertEquals(2, searchResponse.getHits().getTotalHits());
    }

    @Test
    public void shouldIndexMovedSubTree() throws Exception {

        buildAndIndexTree();

        DocumentRef ref = new PathRef("/folder0/folder1/folder2");
        Assert.assertTrue(session.exists(ref));
        DocumentModel doc = session.getDocument(ref);

        // move in the same folder : rename
        session.move(ref, doc.getParentRef(), "folderA");

        startCountingCommandProcessed();
        TransactionHelper.commitOrRollbackTransaction();

        waitForIndexing();
        if (syncMode) {
            // in sync we split recursive update into 2 commands:
            // 1 sync non recurse + 1 async recursive
            assertNumberOfCommandProcessed(9);
        } else {
            assertNumberOfCommandProcessed(8);
        }

        startTransaction();

        SearchResponse searchResponse = esa.getClient().prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setFrom(0)
                .setSize(60).execute().actionGet();
        Assert.assertEquals(10, searchResponse.getHits().getTotalHits());

        // check sub tree search
        searchResponse = esa
                .getClient()
                .prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(
                        QueryBuilders.prefixQuery("ecm:path",
                                "/folder0/folder1/folder2")).execute()
                .actionGet();
        Assert.assertEquals(0, searchResponse.getHits().getTotalHits());

        searchResponse = esa
                .getClient()
                .prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(
                        QueryBuilders.prefixQuery("ecm:path",
                                "/folder0/folder1/folderA")).execute()
                .actionGet();
        Assert.assertEquals(8, searchResponse.getHits().getTotalHits());

        searchResponse = esa
                .getClient()
                .prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(
                        QueryBuilders.prefixQuery("ecm:path",
                                "/folder0/folder1")).execute().actionGet();
        Assert.assertEquals(9, searchResponse.getHits().getTotalHits());

    }

    protected CoreSession getRestrictedSession(String userName)
            throws Exception {
        RepositoryManager rm = Framework
                .getLocalService(RepositoryManager.class);
        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        ctx.put("principal", new UserPrincipal(userName, null, false, false));
        return rm.getDefaultRepository().open(ctx);
    }

    @Test
    public void shouldFilterTreeOnSecurity() throws Exception {

        buildAndIndexTree();

        DocumentModelList docs = ess.query(new NxQueryBuilder(session)
                .nxql("select * from Document")
                .limit(10));
        Assert.assertEquals(10, docs.totalSize());

        // check for user with no rights

        CoreSession restrictedSession = getRestrictedSession("toto");
        try {
            docs = ess
                    .query(new NxQueryBuilder(restrictedSession)
                            .nxql("select * from Document")
                            .limit(10));
            Assert.assertEquals(0, docs.totalSize());

            // add READ rights and check that user now has access

            DocumentRef ref = new PathRef("/folder0/folder1/folder2");
            ACP acp = new ACPImpl();
            ACL acl = ACPImpl.newACL(ACL.LOCAL_ACL);
            acl.add(new ACE("toto", SecurityConstants.READ, true));
            acp.addACL(acl);
            session.setACP(ref, acp, true);
            startCountingCommandProcessed();
            TransactionHelper.commitOrRollbackTransaction();

            waitForIndexing();
            if (syncMode) {
                // in sync we split recursive update into 2 commands:
                // 1 sync non recurse + 1 async recursive
                assertNumberOfCommandProcessed(9);
            } else {
                assertNumberOfCommandProcessed(8);
            }

            startTransaction();
            docs = ess
                    .query(new NxQueryBuilder(restrictedSession)
                            .nxql("select * from Document").limit(10));
            Assert.assertEquals(8, docs.totalSize());

            // block rights and check that blocking is taken into account

            ref = new PathRef(
                    "/folder0/folder1/folder2/folder3/folder4/folder5");
            acp = new ACPImpl();
            acl = ACPImpl.newACL(ACL.LOCAL_ACL);

            acl.add(new ACE(SecurityConstants.EVERYONE,
                    SecurityConstants.EVERYTHING, false));
            acl.add(new ACE("Administrator", SecurityConstants.EVERYTHING, true));
            acp.addACL(acl);

            session.setACP(ref, acp, true);

            session.save();
            startCountingCommandProcessed();
            TransactionHelper.commitOrRollbackTransaction();

            startTransaction();

            waitForIndexing();
            if (syncMode) {
                assertNumberOfCommandProcessed(6);
            } else {
                assertNumberOfCommandProcessed(5);
            }

            docs = ess
                    .query(new NxQueryBuilder(restrictedSession)
                            .nxql("select * from Document").limit(10));
            Assert.assertEquals(3, docs.totalSize());
        } finally {
            CoreInstance.getInstance().close(restrictedSession);
        }
    }

    @Test
    public void shouldDenyAccessOnUnsupportedACL() throws Exception {
        buildAndIndexTree();
        DocumentModelList docs = ess.query(new NxQueryBuilder(session)
                .nxql("select * from Document").limit(10));
        Assert.assertEquals(10, docs.totalSize());

        // check for user with no rights
        CoreSession restrictedSession = getRestrictedSession("toto");
        docs = ess.query(new NxQueryBuilder(restrictedSession)
                .nxql("select * from Document").limit(10));
        Assert.assertEquals(0, docs.totalSize());

        // add READ rights and check that user now has access
        DocumentRef ref = new PathRef("/folder0/folder1/folder2");
        ACP acp = new ACPImpl();
        ACL acl = ACPImpl.newACL(ACL.LOCAL_ACL);
        acl.add(new ACE("toto", SecurityConstants.READ, true));
        acp.addACL(acl);
        session.setACP(ref, acp, true);

        TransactionHelper.commitOrRollbackTransaction();
        waitForIndexing();

        startTransaction();
        docs = ess.query(new NxQueryBuilder(restrictedSession)
                .nxql("select * from Document order by dc:title")
                .limit(10));
        Assert.assertEquals(8, docs.totalSize());

        // Add an unsupported negative ACL
        ref = new PathRef("/folder0/folder1/folder2/folder3/folder4/folder5");
        acp = new ACPImpl();
        acl = ACPImpl.newACL(ACL.LOCAL_ACL);
        acl.add(new ACE("bob", SecurityConstants.EVERYTHING, false));

        acp.addACL(acl);
        session.setACP(ref, acp, true);
        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        waitForIndexing();

        startTransaction();
        docs = ess.query(new NxQueryBuilder(restrictedSession)
                .nxql("select * from Document order by dc:title")
                .limit(10));
        // can view folder2, folder3 and folder4
        Assert.assertEquals(3, docs.totalSize());
        CoreInstance.getInstance().close(restrictedSession);
    }

    @Test
    public void shouldReindexSubTreeInTrash() throws Exception {
        buildAndIndexTree();

        DocumentRef ref = new PathRef("/folder0/folder1/folder2");
        Assert.assertTrue(session.exists(ref));
        session.followTransition(ref, "delete");
        startCountingCommandProcessed();
        TransactionHelper.commitOrRollbackTransaction();
        // let the bulkLifeCycleChangeListener do its work
        WorkManager wm = Framework.getLocalService(WorkManager.class);
        Assert.assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));
        waitForIndexing();
        assertNumberOfCommandProcessed(8);

        startTransaction();
        DocumentModelList docs = ess
                .query(new NxQueryBuilder(session)
                        .nxql("select * from Document where ecm:currentLifeCycleState != 'deleted'")
                        .limit(20));
        // for (DocumentModel doc : docs) {
        // System.out.println(doc.getPathAsString());
        // }
        Assert.assertEquals(2, docs.totalSize());
    }

    @Test
    public void shouldIndexOnCopy() throws Exception {
        buildAndIndexTree();

        DocumentRef src = new PathRef("/folder0/folder1/folder2");
        DocumentRef dst = new PathRef("/folder0");
        session.copy(src, dst, "folder2-copy");

        TransactionHelper.commitOrRollbackTransaction();
        waitForIndexing();

        startTransaction();
        DocumentModelList docs = ess.query(new NxQueryBuilder(session)
                .nxql("select * from Document").limit(20));
        Assert.assertEquals(18, docs.totalSize());
    }

}
