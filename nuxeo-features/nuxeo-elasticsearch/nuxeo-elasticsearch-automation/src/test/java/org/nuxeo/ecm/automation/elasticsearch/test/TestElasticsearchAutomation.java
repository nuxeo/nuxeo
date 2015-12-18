/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.automation.elasticsearch.test;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.elasticsearch.Indexing;
import org.nuxeo.ecm.automation.elasticsearch.WaitForIndexing;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
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
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({"org.nuxeo.ecm.automation.core", "org.nuxeo.elasticsearch.core", "org.nuxeo.elasticsearch.automation"})
@LocalDeploy({"org.nuxeo.ecm.automation.elasticsearch.test:elasticsearch-test-contrib.xml",
        "org.nuxeo.ecm.automation.elasticsearch.test:chain-test-contrib.xml"})
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
    public void testIndexingFromNxql() throws Exception {
        OperationContext ctx = new OperationContext(coreSession);
        ctx.setInput("SELECT ecm:uuid FROM Document WHERE ecm:primaryType = 'File'");
        automationService.run(ctx, INDEX_CHAIN);

        assertEquals(1, ess.query(new NxQueryBuilder(coreSession).nxql("SELECT * from Document")).totalSize());
    }



}
