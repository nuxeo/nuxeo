/*
 * (C) Copyright 2025-2026 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.search;

import static org.junit.Assert.assertEquals;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.MultiRepositorySearchFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features({ MultiRepositorySearchFeature.class })
public class TestSearchMultiRepository {

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected CoreSession defaultSession;

    @Inject
    @Named("other")
    protected CoreSession otherSession;

    @Inject
    protected SearchService searchService;

    @Before
    public void init() {
        createDocs(defaultSession);
        createDocs(otherSession);
        txFeature.nextTransaction();
    }

    protected void createDocs(CoreSession session) {
        // create 2 docs without indexing them
        DocumentModel doc = session.createDocumentModel("/", "my-folder", "Folder");
        doc.setPropertyValue("dc:title", "A folder");
        doc = session.createDocument(doc);
        doc = session.createDocumentModel("/my-folder/", "my-file", "File");
        doc.setPropertyValue("dc:title", "A file");
        session.createDocument(doc);
    }

    @Test
    public void testMultiSearch() {
        assertEquals(2, searchService.search(SearchQuery.builder("SELECT * from Document", defaultSession).build())
                                     .getHitsCount());
        assertEquals(2, searchService.search(SearchQuery.builder("SELECT * from Document", otherSession).build())
                                     .getHitsCount());
        var indexes = searchService.getRepositoryNames()
                                   .stream()
                                   .map(repository -> searchService.getDefaultIndexName(repository))
                                   .toList();
        assertEquals(2, indexes.size());
        var response = searchService.search(
                SearchQuery.builder("SELECT * from Document", defaultSession.getPrincipal()).index(indexes).build());
        assertEquals(4, response.getHitsCount());
        var defaultDocs = response.loadDocuments(defaultSession);
        assertEquals(4, defaultDocs.totalSize());
        assertEquals(4, defaultDocs.size());
        var otherDocs = response.loadDocuments(otherSession);
        assertEquals(4, otherDocs.totalSize());
        assertEquals(4, otherDocs.size());
    }
}
