/*
 * (C) Copyright 2021 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.elasticsearch.test.bulk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.elasticsearch.bulk.IndexAction.INDEX_UPDATE_ALIAS_PARAM;
import static org.nuxeo.elasticsearch.bulk.IndexAction.REFRESH_INDEX_PARAM;

import java.time.Duration;

import javax.inject.Inject;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.bulk.IndexAction;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class, CoreBulkFeature.class })
@Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestBulkIndex {

    // field bigger than a record
    protected static final int BIG_FIELD_SIZE = 1_200_000;

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected ElasticSearchAdmin esa;

    @Inject
    protected BulkService bulkService;

    @Inject
    protected TransactionalFeature txFeature;

    @Before
    public void initWorkingDocuments() {
        if (!TransactionHelper.isTransactionActive()) {
            TransactionHelper.startTransaction();
        }
        for (int i = 0; i < 20; i++) {
            String name = "file" + i;
            DocumentModel doc = session.createDocumentModel("/", name, "File");
            if (i == 0) {
                // create a huge field to make the doc bigger than a record
                doc.setPropertyValue("dc:title", new String(new char[BIG_FIELD_SIZE]).replace('\0', 'X'));
            } else {
                doc.setPropertyValue("dc:title", "File" + i);
            }
            session.createDocument(doc);
        }
        txFeature.nextTransaction();
    }

    protected void addADocument(String id) {
        String name = "file" + id;
        DocumentModel doc = session.createDocumentModel("/", name, "File");
        doc.setPropertyValue("dc:title", "File" + id);
        session.createDocument(doc);
        txFeature.nextTransaction();
    }

    protected void deleteADocument(String id) {
        DocumentRef ref = new PathRef("/", "file" + id);
        session.removeDocument(ref);
        txFeature.nextTransaction();
    }

    @Test
    public void testIndexAction() throws InterruptedException {
        esa.initIndexes(true);
        String commandId = bulkService.submit(
                new BulkCommand.Builder(IndexAction.ACTION_NAME, "SELECT * FROM Document").param(REFRESH_INDEX_PARAM,
                        true).param(INDEX_UPDATE_ALIAS_PARAM, true).batch(2).bucket(2).build());
        assertTrue("command timeout", bulkService.await(commandId, Duration.ofSeconds(60)));
        BulkStatus status = bulkService.getStatus(commandId);
        assertEquals(BulkStatus.State.COMPLETED, status.getState());
    }

    @Test
    @Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-write-alias2-contrib.xml")
    public void testBulkReindexWithAlias() throws Exception {
        String repo = esa.getRepositoryNames().iterator().next();
        String searchAlias = esa.getIndexNameForRepository(repo);
        String searchIndex = esa.getClient().getFirstIndexForAlias(searchAlias);
        String writeAlias = esa.getWriteIndexName(searchAlias);
        String writeIndex = esa.getClient().getFirstIndexForAlias(writeAlias);

        // we have distinct aliases for search and write
        assertNotEquals(searchAlias, writeAlias);
        // both aliases point to the same index
        assertEquals(searchIndex, writeIndex);
        // there is no secondary write index
        String secondaryWriteIndex = esa.getSecondaryWriteIndexName(searchAlias);
        assertNull(secondaryWriteIndex);
        // docs are searchable
        long totalDocs = countDocs(searchAlias);
        assertEquals(20, totalDocs);

        // simulate a bulk reindex by creating a new write index
        esa.initRepositoryIndexWithAliases(session.getRepositoryName());
        // aliases are pointing to different indexes
        searchIndex = esa.getClient().getFirstIndexForAlias(searchAlias);
        writeIndex = esa.getClient().getFirstIndexForAlias(writeAlias);
        assertNotEquals(searchIndex, writeIndex);
        // there is a secondary write index
        secondaryWriteIndex = esa.getSecondaryWriteIndexName(searchAlias);
        assertNotNull(secondaryWriteIndex);
        // which is the current search index
        assertEquals(searchIndex, secondaryWriteIndex);
        // the new write index is empty
        assertEquals(0, countDocs(writeAlias));

        // Run a bulk index command to populate the new write index
        // we explicitly ask to not update alias on completion to be able to test update during reindexing
        String commandId = bulkService.submit(
                new BulkCommand.Builder(IndexAction.ACTION_NAME, "SELECT * FROM Document").param(REFRESH_INDEX_PARAM,
                        true).param(INDEX_UPDATE_ALIAS_PARAM, false).build());
        assertTrue("Command timeout", bulkService.await(commandId, Duration.ofSeconds(60)));
        BulkStatus status = bulkService.getStatus(commandId);
        assertEquals(BulkStatus.State.COMPLETED, status.getState());

        // make sure that aliases are not updated
        searchIndex = esa.getClient().getFirstIndexForAlias(searchAlias);
        writeIndex = esa.getClient().getFirstIndexForAlias(writeAlias);
        secondaryWriteIndex = esa.getSecondaryWriteIndexName(searchAlias);
        assertNotEquals(searchIndex, writeIndex);
        assertNotNull(secondaryWriteIndex);
        assertEquals(searchIndex, secondaryWriteIndex);
        // the refresh is done in async after the bulk command has completed, so we need to do it explicitly
        esa.getClient().refresh(writeAlias);
        // new write index contains all the docs
        assertEquals(totalDocs, countDocs(writeAlias));

        // a new document is indexed on both indexes
        addADocument("foo");
        assertEquals(totalDocs + 1, countDocs(writeAlias));
        assertEquals(totalDocs + 1, countDocs(searchAlias));

        // removing a document updates both indexes
        deleteADocument("foo");
        assertEquals(totalDocs, countDocs(searchAlias));
        assertEquals(totalDocs, countDocs(writeAlias));

        // sync alias to terminate the bulk re indexing simulation
        esa.syncSearchAndWriteAlias(searchAlias);
        searchIndex = esa.getClient().getFirstIndexForAlias(searchAlias);
        writeIndex = esa.getClient().getFirstIndexForAlias(writeAlias);
        // both alias now point to the same index, there is no more secondary write index
        assertEquals(searchIndex, writeIndex);
        secondaryWriteIndex = esa.getSecondaryWriteIndexName(searchAlias);
        assertNull(secondaryWriteIndex);
    }

    protected SearchResponse searchAll(String indexName) {
        SearchRequest request = new SearchRequest(indexName).searchType(SearchType.DFS_QUERY_THEN_FETCH)
                                                            .source(new SearchSourceBuilder().from(0).size(60));
        return esa.getClient().search(request);
    }

    protected long countDocs(String indexName) {
        return searchAll(indexName).getHits().getTotalHits();
    }

}
