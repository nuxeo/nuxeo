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

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
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
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.listener.ElasticSearchInlineListener;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.core:security-policy-contrib.xml")
public class TestSecurityPolicy {

    @Inject
    protected WorkManager workManager;

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

    protected void buildDocs() {
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

    @Test
    public void shouldWorkWithSecurityPolicy() throws Exception {
        buildAndIndexDocs();
        grantBrowsePermToUser("/folder", "toto");

        // As administrator I can see all docs
        DocumentModelList docs = ess.query(new NxQueryBuilder(session).nxql("select * from Document"));
        Assert.assertEquals(6, docs.totalSize());

        // As user File document are not denied
        try (CloseableCoreSession restrictedSession = CoreInstance.openCoreSession(null, "toto")) {
            docs = ess.query(new NxQueryBuilder(restrictedSession).nxql("select * from Document"));
            Assert.assertEquals(1, docs.size());
            Assert.assertEquals(1, docs.totalSize());
        }
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
