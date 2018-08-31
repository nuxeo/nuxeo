/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.elasticsearch.test;

import static org.junit.Assume.assumeTrue;

import java.io.Serializable;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
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
import org.nuxeo.ecm.core.api.trash.TrashService;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.listener.ElasticSearchInlineListener;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
public class TestTreeIndexing {

    private static final String IDX_NAME = "nxutest";

    @Inject
    protected WorkManager workManager;

    @Inject
    protected BulkService bulk;

    @Inject
    CoreSession session;

    @Inject
    ElasticSearchService ess;

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
        bulk.await(Duration.ofSeconds(20));
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

    @Before
    public void setUpMapping() throws Exception {
        esa.initIndexes(true);
    }

    protected void buildTree() {
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
        SearchResponse searchResponse = searchAll();
        Assert.assertEquals(10, searchResponse.getHits().getTotalHits());
    }

    protected SearchResponse searchAll() {
        SearchRequest request = new SearchRequest(IDX_NAME).searchType(SearchType.DFS_QUERY_THEN_FETCH)
                                                           .source(new SearchSourceBuilder().from(0).size(60));
        return esa.getClient().search(request);
    }

    protected SearchResponse search(QueryBuilder query) {
        SearchRequest request = new SearchRequest(IDX_NAME).searchType(SearchType.DFS_QUERY_THEN_FETCH)
                                                           .source(new SearchSourceBuilder().from(0).size(60));
        request.source(new SearchSourceBuilder().query(query));
        return esa.getClient().search(request);
    }

    @Test
    public void shouldIndexTree() throws Exception {
        buildAndIndexTree();

        // check sub tree search
        SearchResponse searchResponse = search(QueryBuilders.prefixQuery("ecm:path", "/folder0/folder1/folder2"));
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
        SearchResponse searchResponse = searchAll();
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
        SearchResponse searchResponse = searchAll();
        Assert.assertEquals(10, searchResponse.getHits().getTotalHits());

        // check sub tree search
        searchResponse = search(QueryBuilders.prefixQuery("ecm:path", "/folder0/folder1/folder2"));
        Assert.assertEquals(0, searchResponse.getHits().getTotalHits());

        searchResponse = search(QueryBuilders.prefixQuery("ecm:path", "/folder0/folder1/folderA"));
        Assert.assertEquals(8, searchResponse.getHits().getTotalHits());

        searchResponse = search(QueryBuilders.prefixQuery("ecm:path", "/folder0/folder1"));
        Assert.assertEquals(9, searchResponse.getHits().getTotalHits());

    }

    protected CloseableCoreSession getRestrictedSession(String userName) {
        RepositoryManager rm = Framework.getService(RepositoryManager.class);
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
        try (CloseableCoreSession restrictedSession = getRestrictedSession("toto")) {
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
        }
    }

    @Test
    public void shouldDenyAccessOnUnsupportedACL() throws Exception {
        assumeTrue(session.isNegativeAclAllowed());

        buildAndIndexTree();
        DocumentModelList docs = ess.query(new NxQueryBuilder(session).nxql("select * from Document"));
        Assert.assertEquals(10, docs.totalSize());

        // check for user with no rights
        try (CloseableCoreSession restrictedSession = getRestrictedSession("toto")) {
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
        }
    }

    @Test
    public void shouldStoreOnlyEffectiveACEs() throws Exception {
        buildAndIndexTree();

        DocumentModelList docs = ess.query(new NxQueryBuilder(session).nxql("select * from Document"));
        Assert.assertEquals(10, docs.totalSize());

        try (CloseableCoreSession restrictedSession = getRestrictedSession("toto")) {
            docs = ess.query(new NxQueryBuilder(restrictedSession).nxql("select * from Document"));
            Assert.assertEquals(0, docs.totalSize());

            DocumentRef ref = new PathRef("/folder0");
            ACP acp = new ACPImpl();
            ACL acl = ACPImpl.newACL(ACL.LOCAL_ACL);
            acl.add(ACE.builder("toto", SecurityConstants.READ).build());
            acp.addACL(acl);
            session.setACP(ref, acp, true);

            TransactionHelper.commitOrRollbackTransaction();
            waitForCompletion();

            startTransaction();
            docs = ess.query(new NxQueryBuilder(restrictedSession).nxql("select * from Document order by dc:title"));
            Assert.assertEquals(10, docs.totalSize());

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

            TransactionHelper.commitOrRollbackTransaction();
            waitForCompletion();

            startTransaction();
            docs = ess.query(new NxQueryBuilder(restrictedSession).nxql("select * from Document order by dc:title"));
            Assert.assertEquals(0, docs.totalSize());
        }
    }

    @Test
    @Deploy("org.nuxeo.elasticsearch.core.test:dummy-bulk-login-config.xml")
    public void shouldReindexSubTreeInTrash() throws Exception {
        buildAndIndexTree();
        startTransaction();
        DocumentRef ref = new PathRef("/folder0/folder1/folder2");
        Assert.assertTrue(session.exists(ref));
        Framework.getService(TrashService.class).trashDocument(session.getDocument(ref));

        TransactionHelper.commitOrRollbackTransaction();
        // let BAF do its work
        waitForCompletion();
        // 1 moved event which triggers 1 recurse command -> 8 commands
        // 8 trashed events -> 7 commands (one of trashed events is merged into the resulted command from moved event)
        if (syncMode) {
            // in sync we split recursive update into 2 commands:
            // 1 sync non recurse + 1 async recursive
            assertNumberOfCommandProcessed(16);
        } else {
            assertNumberOfCommandProcessed(15);
        }

        startTransaction();
        DocumentModelList docs = ess.query(new NxQueryBuilder(session).nxql(
                "select * from Document where ecm:isTrashed = 0"));
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
