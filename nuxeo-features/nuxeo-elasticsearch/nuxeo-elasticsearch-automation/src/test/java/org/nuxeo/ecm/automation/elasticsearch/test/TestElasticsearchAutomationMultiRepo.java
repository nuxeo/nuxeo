/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.automation.elasticsearch.test;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.elasticsearch.ElasticsearchBulkIndexOperation;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.elasticsearch.ElasticSearchConstants;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.elasticsearch.test.RepositoryLightElasticSearchFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features(RepositoryLightElasticSearchFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.elasticsearch.automation")
@Deploy("org.nuxeo.ecm.automation.elasticsearch.test:test-second-repository-contrib.xml")
public class TestElasticsearchAutomationMultiRepo {

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected CoreSession defaultSession;

    protected CloseableCoreSession secondSession;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected ElasticSearchAdmin esa;

    @Inject
    protected AutomationService automationService;

    @Before
    public void init() {
        secondSession = CoreInstance.openCoreSession("second");

        // reset index
        esa.initIndexes(true);
        createDocs(defaultSession);
        createDocs(secondSession);
    }
    
    @After
    public void cleanup() {
        if (secondSession != null) {
            CoreInstance.closeCoreSession(secondSession);
        }
    }

    protected void createDocs(CoreSession session) {
        // create 2 docs without indexing them
        DocumentModel doc = session.createDocumentModel("/", "my-folder", "Folder");
        doc.setPropertyValue("dc:title", "A folder");
        doc.putContextData(ElasticSearchConstants.DISABLE_AUTO_INDEXING, Boolean.TRUE);
        doc = session.createDocument(doc);

        doc = session.createDocumentModel("/my-folder/", "my-file", "File");
        doc.setPropertyValue("dc:title", "A file");
        doc.putContextData(ElasticSearchConstants.DISABLE_AUTO_INDEXING, Boolean.TRUE);
        session.createDocument(doc);

        txFeature.nextTransaction();

        // nothing indexed because of disabled indexing flag
        assertEquals(0, ess.query(new NxQueryBuilder(session).nxql("SELECT * from Document")).totalSize());
    }

    @Test
    public void testIndexingAllOnAllRepositoriesBulkService() throws Exception {
        try (OperationContext defaultCtx = new OperationContext(defaultSession);
                OperationContext secondCtx = new OperationContext(secondSession)) {
            automationService.run(defaultCtx, ElasticsearchBulkIndexOperation.ID);
            automationService.run(secondCtx, ElasticsearchBulkIndexOperation.ID);

            // will wait for bulk, wait for es indexing, do an es refresh
            txFeature.nextTransaction();

            assertEquals(2, ess.query(new NxQueryBuilder(defaultSession).nxql("SELECT * from Document")).totalSize());
            assertEquals(2, ess.query(new NxQueryBuilder(secondSession).nxql("SELECT * from Document")).totalSize());
        }
    }

}
