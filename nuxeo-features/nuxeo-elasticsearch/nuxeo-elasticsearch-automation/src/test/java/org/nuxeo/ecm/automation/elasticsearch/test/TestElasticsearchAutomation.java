/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.automation.elasticsearch.test;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.ElasticSearchConstants;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;


@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.elasticsearch.core")
@Deploy("org.nuxeo.elasticsearch.automation")
@Deploy("org.nuxeo.elasticsearch.core.test:elasticsearch-test-contrib.xml")
@Deploy("org.nuxeo.ecm.automation.elasticsearch.test:chain-test-contrib.xml")
public class TestElasticsearchAutomation {

    private static final String INDEX_CHAIN = "indexAndRefresh";

    @Inject
    CoreSession coreSession;

    @Inject
    ElasticSearchService ess;

    @Inject
    ElasticSearchAdmin esa;

    @Inject
    AutomationService automationService;

    protected DocumentRef rootRef;

    public void waitForIndexing() throws Exception {
        Framework.getService(WorkManager.class).awaitCompletion(20, TimeUnit.SECONDS);
        esa.prepareWaitForIndexing().get(20, TimeUnit.SECONDS);
        esa.refresh();
    }

    @Before
    public void initRepo() throws Exception {
        // reset index
        esa.initIndexes(true);
        // create 2 docs without indexing them
        DocumentModel doc = coreSession.createDocumentModel("/", "my-folder", "Folder");
        doc.setPropertyValue("dc:title", "A folder");
        doc.putContextData(ElasticSearchConstants.DISABLE_AUTO_INDEXING, Boolean.TRUE);
        doc = coreSession.createDocument(doc);
        rootRef = doc.getRef();

        doc = coreSession.createDocumentModel("/my-folder/", "my-file", "File");
        doc.setPropertyValue("dc:title", "A file");
        doc.putContextData(ElasticSearchConstants.DISABLE_AUTO_INDEXING, Boolean.TRUE);
        coreSession.createDocument(doc);
        coreSession.save();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        waitForIndexing();
        // nothing indexed because of disable indexing flag
        assertEquals(0, ess.query(new NxQueryBuilder(coreSession).nxql("SELECT * from Document")).totalSize());
    }


    @Test
    public void testIndexingAll() throws Exception {
        OperationContext ctx = new OperationContext(coreSession);
        automationService.run(ctx, INDEX_CHAIN);

        assertEquals(2, ess.query(new NxQueryBuilder(coreSession).nxql("SELECT * from Document")).totalSize());
    }

    @Test
    public void testIndexingFromRoot() throws Exception {
        OperationContext ctx = new OperationContext(coreSession);
        ctx.setInput(rootRef);
        automationService.run(ctx, INDEX_CHAIN);

        assertEquals(2, ess.query(new NxQueryBuilder(coreSession).nxql("SELECT * from Document")).totalSize());
    }

    @Test
    public void testIndexingFromPath() throws Exception {
        // first index all
        OperationContext ctx = new OperationContext(coreSession);
        automationService.run(ctx, INDEX_CHAIN);
        waitForIndexing();

        // then reindex from path, so we have a 2 commands: delete + insert
        ctx.setInput(rootRef);
        automationService.run(ctx, INDEX_CHAIN);
        waitForIndexing();

        assertEquals(2, ess.query(new NxQueryBuilder(coreSession).nxql("SELECT * from Document")).totalSize());
    }


    @Test
    public void testIndexingFromNxql() throws Exception {
        OperationContext ctx = new OperationContext(coreSession);
        ctx.setInput("SELECT ecm:uuid FROM Document WHERE ecm:primaryType = 'File'");
        automationService.run(ctx, INDEX_CHAIN);

        assertEquals(1, ess.query(new NxQueryBuilder(coreSession).nxql("SELECT * from Document")).totalSize());
    }



}
