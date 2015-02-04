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

import static org.junit.Assume.assumeTrue;

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
import org.nuxeo.ecm.core.event.EventService;
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
    CoreSession session;

    @Inject
    ElasticSearchService ess;

    @Inject
    protected WorkManager workManager;

    @Inject
    ElasticSearchAdmin esa;

    private boolean syncMode = false;

    private int commandProcessed;

    public void assertNumberOfCommandProcessed(int processed) throws Exception {
        Assert.assertEquals(processed, esa.getTotalCommandProcessed() - commandProcessed);
    }

    /**
     * Wait for async worker completion then wait for indexing completion
     */
    public void waitForCompletion() throws Exception {
        workManager.awaitCompletion(20, TimeUnit.SECONDS);
        esa.prepareWaitForIndexing().get(20, TimeUnit.SECONDS);
        esa.refresh();
    }

    public void startTransaction() {
        if (syncMode) {
            ElasticSearchInlineListener.useSyncIndexing.set(true);
        }
        if (!TransactionHelper.isTransactionActive()) {
            TransactionHelper.startTransaction();
        }
        Assert.assertEquals(0, esa.getPendingWorkerCount());
        Assert.assertEquals(0, esa.getPendingCommandCount());
        commandProcessed = esa.getTotalCommandProcessed();
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

    protected void buildTree() throws ClientException {
        String root = "/";
        for (int i = 0; i < 10; i++) {
            String name = "folder" + i;
            DocumentModel doc = session.createDocumentModel(root, name, "Folder");
            doc.setPropertyValue("dc:title", "Folder" + i);
            session.createDocument(doc);
            root = root + name + "/";
        }
    }

    protected void buildAndIndexTree() throws Exception {
        startTransaction();
        buildTree();
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(10);

        startTransaction();
        // check indexing at ES level
        SearchResponse searchResponse = esa.getClient().prepareSearch(IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(10, searchResponse.getHits().getTotalHits());
    }

    @Test
    public void shouldIndexTree() throws Exception {
        buildAndIndexTree();

        // check sub tree search
        SearchResponse searchResponse = esa.getClient().prepareSearch(IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                QueryBuilders.prefixQuery("ecm:path", "/folder0/folder1/folder2")).execute().actionGet();
        Assert.assertEquals(8, searchResponse.getHits().getTotalHits());
    }

    @Test
    public void shouldUnIndexSubTree() throws Exception {
        buildAndIndexTree();

        DocumentRef ref = new PathRef("/folder0/folder1/folder2");
        Assert.assertTrue(session.exists(ref));

        startTransaction();
        session.removeDocument(ref);
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(1);

        startTransaction();
        SearchResponse searchResponse = esa.getClient().prepareSearch(IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(2, searchResponse.getHits().getTotalHits());
    }

    @Test
    public void shouldIndexMovedSubTree() throws Exception {
        buildAndIndexTree();
        startTransaction();
        DocumentRef ref = new PathRef("/folder0/folder1/folder2");
        Assert.assertTrue(session.exists(ref));
        DocumentModel doc = session.getDocument(ref);

        // move in the same folder : rename
        session.move(ref, doc.getParentRef(), "folderA");

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        if (syncMode) {
            // in sync we split recursive update into 2 commands:
            // 1 sync non recurse + 1 async recursive
            assertNumberOfCommandProcessed(9);
        } else {
            assertNumberOfCommandProcessed(8);
        }

        startTransaction();
        SearchResponse searchResponse = esa.getClient().prepareSearch(IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(10, searchResponse.getHits().getTotalHits());

        // check sub tree search
        searchResponse = esa.getClient().prepareSearch(IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                QueryBuilders.prefixQuery("ecm:path", "/folder0/folder1/folder2")).execute().actionGet();
        Assert.assertEquals(0, searchResponse.getHits().getTotalHits());

        searchResponse = esa.getClient().prepareSearch(IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                QueryBuilders.prefixQuery("ecm:path", "/folder0/folder1/folderA")).execute().actionGet();
        Assert.assertEquals(8, searchResponse.getHits().getTotalHits());

        searchResponse = esa.getClient().prepareSearch(IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(QueryBuilders.prefixQuery("ecm:path", "/folder0/folder1")).execute().actionGet();
        Assert.assertEquals(9, searchResponse.getHits().getTotalHits());

    }

    protected CoreSession getRestrictedSession(String userName) {
        RepositoryManager rm = Framework.getLocalService(RepositoryManager.class);
        Map<String, Serializable> ctx = new HashMap<>();
        ctx.put("principal", new UserPrincipal(userName, null, false, false));
        return CoreInstance.openCoreSession(rm.getDefaultRepositoryName(), ctx);
    }

    @Test
    public void shouldFilterTreeOnSecurity() throws Exception {

        buildAndIndexTree();

        DocumentModelList docs = ess.query(new NxQueryBuilder(session).nxql("select * from Document"));
        Assert.assertEquals(10, docs.totalSize());

        // check for user with no rights
        startTransaction();
        CoreSession restrictedSession = getRestrictedSession("toto");
        try {
            docs = ess.query(new NxQueryBuilder(restrictedSession).nxql("select * from Document"));
            Assert.assertEquals(0, docs.totalSize());

            // add READ rights and check that user now has access

            DocumentRef ref = new PathRef("/folder0/folder1/folder2");
            ACP acp = new ACPImpl();
            ACL acl = ACPImpl.newACL(ACL.LOCAL_ACL);
            acl.add(new ACE("toto", SecurityConstants.READ, true));
            acp.addACL(acl);
            session.setACP(ref, acp, true);

            TransactionHelper.commitOrRollbackTransaction();
            waitForCompletion();
            if (syncMode) {
                // in sync we split recursive update into 2 commands:
                // 1 sync non recurse + 1 async recursive
                assertNumberOfCommandProcessed(9);
            } else {
                assertNumberOfCommandProcessed(8);
            }

            startTransaction();
            docs = ess.query(new NxQueryBuilder(restrictedSession).nxql("select * from Document"));
            Assert.assertEquals(8, docs.totalSize());

            // block rights and check that blocking is taken into account

            ref = new PathRef("/folder0/folder1/folder2/folder3/folder4/folder5");
            acp = new ACPImpl();
            acl = ACPImpl.newACL(ACL.LOCAL_ACL);

            acl.add(new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false));
            acl.add(new ACE("Administrator", SecurityConstants.EVERYTHING, true));
            acp.addACL(acl);

            session.setACP(ref, acp, true);

            session.save();
            TransactionHelper.commitOrRollbackTransaction();
            waitForCompletion();
            if (syncMode) {
                assertNumberOfCommandProcessed(6);
            } else {
                assertNumberOfCommandProcessed(5);
            }
            startTransaction();
            docs = ess.query(new NxQueryBuilder(restrictedSession).nxql("select * from Document"));
            Assert.assertEquals(3, docs.totalSize());
        } finally {
            restrictedSession.close();
        }
    }

    @Test
    public void shouldDenyAccessOnUnsupportedACL() throws Exception {
        assumeTrue(session.isNegativeAclAllowed());

        buildAndIndexTree();
        DocumentModelList docs = ess.query(new NxQueryBuilder(session).nxql("select * from Document"));
        Assert.assertEquals(10, docs.totalSize());

        // check for user with no rights
        CoreSession restrictedSession = getRestrictedSession("toto");
        docs = ess.query(new NxQueryBuilder(restrictedSession).nxql("select * from Document"));
        Assert.assertEquals(0, docs.totalSize());

        // add READ rights and check that user now has access
        DocumentRef ref = new PathRef("/folder0/folder1/folder2");
        ACP acp = new ACPImpl();
        ACL acl = ACPImpl.newACL(ACL.LOCAL_ACL);
        acl.add(new ACE("toto", SecurityConstants.READ, true));
        acp.addACL(acl);
        session.setACP(ref, acp, true);

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();

        startTransaction();
        docs = ess.query(new NxQueryBuilder(restrictedSession).nxql("select * from Document order by dc:title"));
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
        waitForCompletion();

        startTransaction();
        docs = ess.query(new NxQueryBuilder(restrictedSession).nxql("select * from Document order by dc:title"));
        // can view folder2, folder3 and folder4
        Assert.assertEquals(3, docs.totalSize());

        restrictedSession.close();
    }

    @Test
    public void shouldReindexSubTreeInTrash() throws Exception {
        buildAndIndexTree();
        startTransaction();
        DocumentRef ref = new PathRef("/folder0/folder1/folder2");
        Assert.assertTrue(session.exists(ref));
        session.followTransition(ref, "delete");

        TransactionHelper.commitOrRollbackTransaction();
        // let the bulkLifeCycleChangeListener do its work
        waitForCompletion();
        assertNumberOfCommandProcessed(8);

        startTransaction();
        DocumentModelList docs = ess.query(new NxQueryBuilder(session).nxql("select * from Document where ecm:currentLifeCycleState != 'deleted'"));
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
        waitForCompletion();

        startTransaction();
        DocumentModelList docs = ess.query(new NxQueryBuilder(session).nxql("select * from Document"));
        Assert.assertEquals(18, docs.totalSize());
    }

}
