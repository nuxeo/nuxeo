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
 *     Delbosc Benoit
 */

package org.nuxeo.elasticsearch.test;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.tag" })
@LocalDeploy("org.nuxeo.elasticsearch.core:elasticsearch-test-mapping-contrib.xml")
public class TestMapping {

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    ElasticSearchAdmin esa;

    private int commandProcessed;

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
        Assert.assertFalse("Still indexing in progress",
                esa.isIndexingInProgress());
        esa.refresh();
    }

    public void startTransaction() {
        if (!TransactionHelper.isTransactionActive()) {
            TransactionHelper.startTransaction();
        }
    }

    @After
    public void cleanupIndexed() throws Exception {
        esa.initIndexes(true);
    }

    @Test
    public void testIlikeSearch() throws Exception {
        startTransaction();
        DocumentModel doc = session
                .createDocumentModel("/", "testDoc1", "File");
        doc.setPropertyValue("dc:title", "upper case");
        doc.setPropertyValue("dc:description", "UPPER CASE DESC");
        doc = session.createDocument(doc);

        doc = session.createDocumentModel("/", "testDoc2", "File");
        doc.setPropertyValue("dc:title", "mixed case");
        doc.setPropertyValue("dc:description", "MiXeD cAsE dEsC");
        doc = session.createDocument(doc);

        doc = session.createDocumentModel("/", "testDoc3", "File");
        doc.setPropertyValue("dc:title", "lower case");
        doc.setPropertyValue("dc:description", "lower case desc");
        doc = session.createDocument(doc);

        startCountingCommandProcessed();
        TransactionHelper.commitOrRollbackTransaction();
        waitForIndexing();
        assertNumberOfCommandProcessed(3);

        startTransaction();
        DocumentModelList ret = ess
                .query(new NxQueryBuilder(session)
                        .nxql("SELECT * FROM Document WHERE dc:description LIKE '%case%'"));
        Assert.assertEquals(3, ret.totalSize());

        ret = ess
                .query(new NxQueryBuilder(session)
                        .nxql("SELECT * FROM Document WHERE dc:description LIKE 'upper%'"));
        Assert.assertEquals(1, ret.totalSize());

        ret = ess
                .query(new NxQueryBuilder(session)
                        .nxql("SELECT * FROM Document WHERE dc:description = 'mixed case desc'"));
        Assert.assertEquals(1, ret.totalSize());
    }

}
