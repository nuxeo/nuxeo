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
 *     Benoit Delbosc
 */

package org.nuxeo.elasticsearch.test;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
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

/**
 * Test "on the fly" indexing via the listener system
 */
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
public class TestFulltextEnabled {

    private static final String IDX_NAME = "nxutest";

    private static final String TYPE_NAME = "doc";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected WorkManager workManager;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected ElasticSearchAdmin esa;

    private int commandProcessed;

    // Number of processed command since the startTransaction
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

    public void sleepForFulltext() {
        coreFeature.getStorageConfiguration().sleepForFulltext();
    }

    protected void startTransaction() {
        if (!TransactionHelper.isTransactionActive()) {
            TransactionHelper.startTransaction();
        }
        Assert.assertEquals(0, esa.getPendingWorkerCount());
        commandProcessed = esa.getTotalCommandProcessed();
    }

    @Before
    public void setupIndex() throws Exception {
        esa.initIndexes(true);
    }

    @Test
    public void testFulltext() throws Exception {
        createFileWithBlob();
        // binary fulltext is extracted and searcheable with ES
        String nxql = "SELECT * FROM Document WHERE ecm:fulltext='search'";
        DocumentModelList esRet = ess.query(new NxQueryBuilder(session).nxql(nxql));
        Assert.assertEquals(1, esRet.totalSize());

        // binary fulltext is also searcheable with VCS
        sleepForFulltext();
        DocumentModelList coreRet = session.query(nxql);
        Assert.assertEquals(1, coreRet.totalSize());
    }

    @Test
    public void testFulltextOnProxy() throws Exception {
        DocumentModel doc = createFileWithBlob();
        createSectionAndPublishFile(doc);
        // binary fulltext is extracted and searcheable with ES
        String nxql = "SELECT * FROM Document WHERE ecm:fulltext='search' AND ecm:isProxy = 1";
        DocumentModelList esRet = ess.query(new NxQueryBuilder(session).nxql(nxql));
        Assert.assertEquals(1, esRet.totalSize());

        // binary fulltext is also searcheable with VCS
        sleepForFulltext();
        DocumentModelList coreRet = session.query(nxql);
        Assert.assertEquals(1, coreRet.totalSize());
    }

    protected DocumentModel createFileWithBlob() throws Exception {
        startTransaction();
        // this is to prevent race condition that happen NXP-16169
        ElasticSearchInlineListener.useSyncIndexing.set(true);
        DocumentModel doc = session.createDocumentModel("/", "myFile", "File");
        BlobHolder holder = doc.getAdapter(BlobHolder.class);
        holder.setBlob(new StringBlob("You know for search"));
        session.createDocument(doc);
        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        // we need to wait for the async fulltext indexing
        WorkManager wm = Framework.getService(WorkManager.class);
        waitForCompletion();
        startTransaction();

        // There is one doc
        DocumentModelList ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document"));
        Assert.assertEquals(1, ret.totalSize());

        return ret.get(0);
    }

    protected void createSectionAndPublishFile(DocumentModel doc) throws Exception {
        // Create a Section
        DocumentModel section = session.createDocumentModel("/", "section", "Folder");
        section = session.createDocument(section);

        // Publish Document
        session.publishDocument(doc, section);
        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        // we need to wait for the async fulltext indexing
        WorkManager wm = Framework.getService(WorkManager.class);
        waitForCompletion();
        startTransaction();

        // There is one doc
        DocumentModelList ret = ess.query(
                new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:isProxy = 1"));
        Assert.assertEquals(1, ret.totalSize());
    }

}
