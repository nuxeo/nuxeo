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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({RepositoryElasticSearchFeature.class})
@LocalDeploy({"org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml", "org.nuxeo.elasticsearch" +
        ".core:security-policy-contrib.xml"})
//@LocalDeploy({"org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml"})
public class TestSecurityPolicy {

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

    protected void buildDocs() throws ClientException {
        DocumentModel doc = session.createDocumentModel("/", "folder", "Folder");
        doc.setPropertyValue("dc:title", "folder");
        session.createDocument(doc);
        for (int i = 0; i < 5; i++) {
            String name = "file" + i;
            doc = session.createDocumentModel("/folder", name, "File");
            doc.setPropertyValue("dc:title", "File" + i);
            session.createDocument(doc);
        }
    }

    protected void buildAndIndexDocs() throws Exception {
        startTransaction();
        buildDocs();
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(6);
        startTransaction();
    }

    protected CoreSession getRestrictedSession(String userName) {
        RepositoryManager rm = Framework.getLocalService(RepositoryManager.class);
        Map<String, Serializable> ctx = new HashMap<>();
        ctx.put("principal", new UserPrincipal(userName, null, false, false));
        return CoreInstance.openCoreSession(rm.getDefaultRepositoryName(), ctx);
    }

    @Test
    public void shouldWorkWithSecurityPolicy() throws Exception {
        buildAndIndexDocs();
        grantBrowsePermToUser("/folder", "toto");

        // As administrator I can see all docs
        DocumentModelList docs = ess.query(new NxQueryBuilder(session).nxql("select * from Document"));
        Assert.assertEquals(6, docs.totalSize());

        // As user File document are not denied
        CoreSession restrictedSession = getRestrictedSession("toto");
        docs = ess.query(new NxQueryBuilder(restrictedSession).nxql("select * from Document"));
        Assert.assertEquals(1, docs.size());
        Assert.assertEquals(1, docs.totalSize());
        restrictedSession.close();

    }

    protected void grantBrowsePermToUser(String path, String username) throws Exception {
        DocumentRef ref = new PathRef(path);
        ACP acp = new ACPImpl();
        ACL acl = ACPImpl.newACL(ACL.LOCAL_ACL);
        acl.add(new ACE(username, SecurityConstants.READ, true));
        acp.addACL(acl);
        session.setACP(ref, acp, true);
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();
    }

}
