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
 *     Nuxeo
 */

package org.nuxeo.elasticsearch.test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.tag.TagService;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.listener.ElasticSearchInlineListener;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

import static org.nuxeo.elasticsearch.ElasticSearchConstants.INDEX_BULK_MAX_SIZE_PROPERTY;

/**
 * Test "on the fly" indexing via the listener system
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class, LogCaptureFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.tag", "org.nuxeo.ecm.platform.ws", "org.nuxeo.ecm.automation.core" })
@LocalDeploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
public class TestReindex {

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected ElasticSearchIndexing esi;

    @Inject
    protected TrashService trashService;

    @Inject
    ElasticSearchAdmin esa;

    @Inject
    protected TagService tagService;

    @Inject
    protected WorkManager workManager;

    @Inject
    LogCaptureFeature.Result logCaptureResult;

    private boolean syncMode = false;

    private int commandProcessed;

    private Priority consoleThresold;

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

    protected void startTransaction() {
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

    @Before
    public void setupIndex() throws Exception {
        esa.initIndexes(true);
    }

    @After
    public void disableSynchronousMode() {
        ElasticSearchInlineListener.useSyncIndexing.set(false);
        syncMode = false;
    }

    @Test
    public void shouldReindexDocument() throws Exception {
        buildDocs();
        startTransaction();

        String nxql = "SELECT * FROM Document, Relation order by ecm:uuid";
        ElasticSearchService ess = Framework.getLocalService(ElasticSearchService.class);
        DocumentModelList coreDocs = session.query(nxql);
        DocumentModelList docs = ess.query(new NxQueryBuilder(session).nxql(nxql).limit(100));

        Assert.assertEquals(coreDocs.totalSize(), docs.totalSize());
        Assert.assertEquals(getDigest(coreDocs), getDigest(docs));
        // can not do that because of NXP-16154
        // Assert.assertEquals(getDigest(coreDocs), 42, docs.totalSize());
        esa.initIndexes(true);
        esa.refresh();
        DocumentModelList docs2 = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document"));
        Assert.assertEquals(0, docs2.totalSize());
        esi.runReindexingWorker(session.getRepositoryName(), "SELECT * FROM Document");
        esi.runReindexingWorker(session.getRepositoryName(), "SELECT * FROM Relation");
        waitForCompletion();
        docs2 = ess.query(new NxQueryBuilder(session).nxql(nxql).limit(100));

        Assert.assertEquals(getDigest(coreDocs), getDigest(docs2));

    }

    private void buildDocs() throws Exception {
        startTransaction();

        DocumentModel folder = session.createDocumentModel("/", "section", "Folder");
        folder = session.createDocument(folder);
        session.saveDocument(folder);
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i, "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            BlobHolder holder = doc.getAdapter(BlobHolder.class);
            holder.setBlob(new StringBlob("You know for search" + i));
            doc = session.createDocument(doc);
            tagService.tag(session, doc.getId(), "mytag" + i, "Administrator");
        }
        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();

        for (int i = 0; i < 5; i++) {
            DocumentModel doc = session.getDocument(new PathRef("/testDoc" + i));
            doc.setPropertyValue("dc:description", "Description TestMe" + i);
            doc = session.saveDocument(doc);
            DocumentModel proxy = session.publishDocument(doc, folder);
            if (i % 2 == 0) {
                trashService.trashDocuments(Arrays.asList(doc));
            }
        }
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
    }

    protected String getDigest(DocumentModelList docs) {
        StringBuilder sb = new StringBuilder();
        for (DocumentModel doc : docs) {
            String nameOrTitle = doc.getName();
            if (nameOrTitle == null || nameOrTitle.isEmpty()) {
                nameOrTitle = doc.getTitle();
            }
            sb.append(doc.getType() + " " + doc.isProxy() + " " + doc.getId() + " ");
            sb.append(nameOrTitle);
            sb.append("\n");
        }
        return sb.toString();
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN", loggerName = "org.nuxeo.elasticsearch.core.ElasticSearchIndexingImpl")
    public void shouldReindexDocumentWithSmallBulkSize() throws Exception {
        try {
            hideWarningFromConsoleLog();
            System.setProperty(INDEX_BULK_MAX_SIZE_PROPERTY, "4096");
            shouldReindexDocument();
            List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
            Assert.assertFalse("Expecting warn message", events.isEmpty());
            Assert.assertTrue(events.get(0).getRenderedMessage().contains("Max bulk size reached"));
        } finally {
            restoreConsoleLog();
            System.clearProperty(INDEX_BULK_MAX_SIZE_PROPERTY);
        }
    }

    protected void hideWarningFromConsoleLog() {
        Logger rootLogger = Logger.getRootLogger();
        ConsoleAppender consoleAppender = (ConsoleAppender) rootLogger.getAppender("CONSOLE");
        consoleThresold = consoleAppender.getThreshold();
        consoleAppender.setThreshold(Level.ERROR);
    }

    protected void restoreConsoleLog() {
        if (consoleThresold == null) {
            return;
        }
        Logger rootLogger = Logger.getRootLogger();
        ConsoleAppender consoleAppender = (ConsoleAppender) rootLogger.getAppender("CONSOLE");
        consoleAppender.setThreshold(consoleThresold);
        consoleThresold = null;
    }

}