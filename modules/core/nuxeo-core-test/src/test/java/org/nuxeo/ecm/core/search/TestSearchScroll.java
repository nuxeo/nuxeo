/*
 * (C) Copyright 2016-2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.core.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.logging.log4j.core.LogEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.CoreSearchFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Tests the scroll search API exposed by {@link SearchService}.
 *
 * @since 8.3
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreSearchFeature.class, LogCaptureFeature.class })
public class TestSearchScroll {

    @Inject
    protected CoreSession session;

    @Inject
    protected SearchService searchService;

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected LogCaptureFeature.Result logCaptureResult;

    @Test
    public void testScroll() {
        int nbDocs = 100;
        buildAndIndexTree(nbDocs);

        // Initial search request, includes the first batch of results
        String query = "select * from Document order by dc:title";
        var res = searchService.search(
                SearchQuery.builder(query, session).scrollSize(20).scrollKeepAlive(Duration.ofSeconds(10_000)).build());
        assertNotNull(res);
        assertNotNull(res.getScrollContext());
        assertEquals(10_000, res.getScrollContext().searchQuery().getScrollKeepAlive().toSeconds());
        assertNotNull(res.getScrollContext().scrollId());

        // Next result batches
        int totalDocCount = 0;
        List<String> docPaths = new ArrayList<>();
        DocumentModelList docs = res.loadDocuments(session);
        while (!docs.isEmpty()) {
            int hitCount = docs.size();
            assertEquals(20, hitCount);
            totalDocCount += hitCount;
            docPaths.addAll(docs.stream().map(DocumentModel::getPathAsString).toList());
            res = searchService.searchScroll(res.getScrollContext());
            docs = res.loadDocuments(session);
        }
        assertEquals(nbDocs, totalDocCount);

        // Check order
        assertEquals(session.query(query).stream().map(DocumentModel::getPathAsString).collect(Collectors.toList()),
                docPaths);
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN")
    public void testDoubleClearScroll() {
        buildAndIndexTree(5);
        var res = searchService.search(SearchQuery.builder("select * from Document", session)
                                                  .scrollSize(2)
                                                  .scrollKeepAlive(Duration.ofSeconds(60))
                                                  .build());
        var scrollContext = res.getScrollContext();
        assertNotNull(scrollContext);
        assertNotNull(scrollContext.scrollId());

        assertTrue("First clearSearchScroll must succeed", searchService.clearSearchScroll(scrollContext));
        // Second clear of the same already-freed scroll must be swallowed gracefully.
        assertTrue("Second clearSearchScroll on already-freed scroll must succeed",
                searchService.clearSearchScroll(scrollContext));
        List<LogEvent> events = logCaptureResult.getCaughtEvents();
        assertTrue("Unexpected WARN logged: " + events, events.isEmpty());
    }

    protected void buildAndIndexTree(int docCount) {
        String root = "/";
        for (int i = 0; i < docCount; i++) {
            String name = "folder" + i;
            DocumentModel doc = session.createDocumentModel(root, name, "Folder");
            doc.setPropertyValue("dc:title", "Folder" + i);
            session.createDocument(doc);
        }
        txFeature.nextTransaction();
    }
}
