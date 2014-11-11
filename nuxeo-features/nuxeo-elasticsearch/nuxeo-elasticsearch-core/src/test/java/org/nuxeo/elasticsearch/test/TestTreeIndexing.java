package org.nuxeo.elasticsearch.test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
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
import org.nuxeo.elasticsearch.ElasticSearchComponent;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
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
    protected ElasticSearchAdmin esa;

    @Inject
    protected ElasticSearchIndexing esi;

    protected void buildTree() throws ClientException {
        String root = "/";
        for (int i = 0; i < 10; i++) {
            String name = "folder" + i;
            DocumentModel doc = session.createDocumentModel(root, name,
                    "Folder");
            doc.setPropertyValue("dc:title", "Folder" + i);
            doc = session.createDocument(doc);
            root = root + name + "/";
        }
    }

    protected void waitForAsyncIndexing() throws Exception {
        // wait for indexing
        WorkManager wm = Framework.getLocalService(WorkManager.class);
        Assert.assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));
        Assert.assertEquals(0, esa.getPendingCommands());
        Assert.assertEquals(0, esa.getPendingDocs());
    }

    protected void buildAndIndexTree() throws Exception {

        if (!TransactionHelper.isTransactionActive()) {
            TransactionHelper.startTransaction();
        }

        // build the tree
        buildTree();

        TransactionHelper.commitOrRollbackTransaction();

        Assert.assertTrue(esa.getPendingCommands() > 1);
        Assert.assertTrue(esa.getPendingDocs() > 1);

        waitForAsyncIndexing();

        esa.refresh();

        TransactionHelper.startTransaction();

        // check indexing
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
        session.removeDocument(ref);

        TransactionHelper.commitOrRollbackTransaction();

        Assert.assertEquals(1, esa.getPendingDocs());
        // async command is not yet scheduled
        Assert.assertEquals(1, esa.getPendingCommands());

        waitForAsyncIndexing();

        esa.refresh();

        TransactionHelper.startTransaction();

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

        TransactionHelper.commitOrRollbackTransaction();

        Assert.assertEquals(1, esa.getPendingDocs());
        Assert.assertEquals(1, esa.getPendingCommands());

        waitForAsyncIndexing();

        esa.refresh();

        TransactionHelper.startTransaction();

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
        ctx.put("principal", new UserPrincipal("toto", null, false, false));
        return rm.getDefaultRepository().open(ctx);
    }

    @Test
    public void shouldFilterTreeOnSecurity() throws Exception {

        buildAndIndexTree();

        DocumentModelList docs = ess.query(session, "select * from Document",
                10, 0);
        Assert.assertEquals(10, docs.size());

        // check for user with no rights

        CoreSession restrictedSession = getRestrictedSession("toto");
        docs = ess.query(restrictedSession, "select * from Document", 10, 0);
        Assert.assertEquals(0, docs.size());

        // add READ rights and check that user now has access

        DocumentRef ref = new PathRef("/folder0/folder1/folder2");
        ACP acp = new ACPImpl();
        ACL acl = ACPImpl.newACL(ACL.LOCAL_ACL);
        acl.add(new ACE("toto", SecurityConstants.READ, true));
        acp.addACL(acl);
        session.setACP(ref, acp, true);

        TransactionHelper.commitOrRollbackTransaction();

        Assert.assertEquals(1, esa.getPendingDocs());
        Assert.assertEquals(1, esa.getPendingCommands());

        waitForAsyncIndexing();

        esa.refresh();

        TransactionHelper.startTransaction();

        docs = ess.query(restrictedSession, "select * from Document", 10, 0);
        Assert.assertEquals(8, docs.size());

        // block rights and check that blocking is taken into account

        ref = new PathRef("/folder0/folder1/folder2/folder3/folder4/folder5");
        acp = new ACPImpl();
        acl = ACPImpl.newACL(ACL.LOCAL_ACL);

        acl.add(new ACE(SecurityConstants.EVERYONE,
                SecurityConstants.EVERYTHING, false));
        acl.add(new ACE("Administrator", SecurityConstants.EVERYTHING, true));
        acp.addACL(acl);

        session.setACP(ref, acp, true);

        session.save();

        TransactionHelper.commitOrRollbackTransaction();

        TransactionHelper.startTransaction();

        Assert.assertEquals(1, esa.getPendingDocs());
        Assert.assertEquals(1, esa.getPendingCommands());

        waitForAsyncIndexing();
        esa.refresh();

        docs = ess.query(restrictedSession, "select * from Document", 10, 0);
        Assert.assertEquals(3, docs.size());

        CoreInstance.getInstance().close(restrictedSession);
    }

    @Test
    public void shouldDenyAccessOnUnsupportedACL() throws Exception {
        buildAndIndexTree();
        DocumentModelList docs = ess.query(session, "select * from Document",
                10, 0);
        Assert.assertEquals(10, docs.size());

        // check for user with no rights
        CoreSession restrictedSession = getRestrictedSession("toto");
        docs = ess.query(restrictedSession, "select * from Document", 10, 0);
        Assert.assertEquals(0, docs.size());

        // add READ rights and check that user now has access
        DocumentRef ref = new PathRef("/folder0/folder1/folder2");
        ACP acp = new ACPImpl();
        ACL acl = ACPImpl.newACL(ACL.LOCAL_ACL);
        acl.add(new ACE("toto", SecurityConstants.READ, true));
        acp.addACL(acl);
        session.setACP(ref, acp, true);

        TransactionHelper.commitOrRollbackTransaction();
        waitForAsyncIndexing();
        esa.refresh();

        TransactionHelper.startTransaction();
        docs = ess.query(restrictedSession,
                "select * from Document order by dc:title", 10, 0);
        Assert.assertEquals(8, docs.size());

        // Add an unsupported negative ACL
        ref = new PathRef("/folder0/folder1/folder2/folder3/folder4/folder5");
        acp = new ACPImpl();
        acl = ACPImpl.newACL(ACL.LOCAL_ACL);
        acl.add(new ACE("bob", SecurityConstants.EVERYTHING, false));

        acp.addACL(acl);
        session.setACP(ref, acp, true);
        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        waitForAsyncIndexing();
        esa.refresh();

        TransactionHelper.startTransaction();
        docs = ess.query(restrictedSession,
                "select * from Document order by dc:title", 10, 0);
        // can view folder2, folder3 and folder4
        Assert.assertEquals(3, docs.size());

        CoreInstance.getInstance().close(restrictedSession);
    }

    @Test
    public void shouldReindexSubTreeInTrash() throws Exception {

        buildAndIndexTree();

        DocumentRef ref = new PathRef("/folder0/folder1/folder2");
        Assert.assertTrue(session.exists(ref));
        session.followTransition(ref, "delete");

        TransactionHelper.commitOrRollbackTransaction();

        Assert.assertEquals(1, esa.getPendingDocs());
        // async command is not yet scheduled
        Assert.assertEquals(1, esa.getPendingCommands());

        waitForAsyncIndexing();

        esa.refresh();

        TransactionHelper.startTransaction();

        DocumentModelList docs = ess
                .query(session,
                        "select * from Document where ecm:currentLifeCycleState != 'deleted'",
                        20, 0);

        for (DocumentModel doc : docs) {
            System.out.println(doc.getPathAsString());
        }
        Assert.assertEquals(2, docs.size());

    }

}
