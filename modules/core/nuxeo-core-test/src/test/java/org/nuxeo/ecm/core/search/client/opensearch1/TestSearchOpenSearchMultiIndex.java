/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.search.client.opensearch1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.search.BaseCoreSearchFeature.assertIndexedSince;
import static org.nuxeo.ecm.core.search.BaseCoreSearchFeature.assertNotIndexed;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.search.SearchService;
import org.nuxeo.ecm.core.test.CoreSearchFeature;
import org.nuxeo.runtime.test.runner.ConditionalIgnore;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Checks the routing of ongoing indexing when a repository has more than one index: sync events are indexed on the
 * default index (synchronously) and non-default indexes are fed asynchronously.
 *
 * @since 2025.22
 */
@RunWith(FeaturesRunner.class)
@Features(CoreSearchFeature.class)
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/search/test-opensearch1-two-indexes-contrib.xml")
@ConditionalIgnore(condition = IgnoreIfNotOpenSearchSearchClient.class)
public class TestSearchOpenSearchMultiIndex {

    protected static final String DEFAULT_INDEX = "enhanced";

    protected static final String SECONDARY_INDEX = "secondary";

    @Inject
    protected CoreSession session;

    @Inject
    protected SearchService searchService;

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void testRepositoryHasTwoIndexes() {
        assertEquals(DEFAULT_INDEX, searchService.getDefaultIndexName("test"));
        List<String> indexes = searchService.getIndexNames("test");
        assertTrue("Expected both indexes, got: " + indexes,
                indexes.containsAll(List.of(DEFAULT_INDEX, SECONDARY_INDEX)));
    }

    @Test
    public void testSyncEventIndexedOnBothDefaultAndSecondaryIndex() {
        DocumentModel doc = session.createDocumentModel("/", "testDoc", "File");
        doc.setPropertyValue("dc:title", "TestMe");
        doc = session.createDocument(doc);
        String docId = doc.getId();
        long t1 = System.currentTimeMillis();

        // wait for async indexing to complete (the sync part already ran on the default index at commit time)
        txFeature.nextTransaction();

        // the default index provides real-time search-after-save
        assertIndexedSince(DEFAULT_INDEX, docId, t1);
        // the non-default index is fed asynchronously, it must eventually contain the document too
        assertIndexedSince(SECONDARY_INDEX, docId, t1);
    }

    @Test
    public void testDeleteEventUnindexedOnBothIndexes() {
        DocumentModel doc = session.createDocumentModel("/", "testDoc", "File");
        doc.setPropertyValue("dc:title", "TestMe");
        doc = session.createDocument(doc);
        String docId = doc.getId();
        long t1 = System.currentTimeMillis();
        txFeature.nextTransaction();
        assertIndexedSince(DEFAULT_INDEX, docId, t1);
        assertIndexedSince(SECONDARY_INDEX, docId, t1);

        session.removeDocument(doc.getRef());
        txFeature.nextTransaction();

        assertNotIndexed(DEFAULT_INDEX, docId);
        assertNotIndexed(SECONDARY_INDEX, docId);
    }
}
