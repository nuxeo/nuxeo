/*
 * (C) Copyright 2014-2026 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.search;

import static java.util.Calendar.JANUARY;
import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import jakarta.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.trash.TrashService;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.search.client.repository.IgnoreIfRepositorySearchClient;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.CoreSearchFeature;
import org.nuxeo.runtime.test.runner.ConditionalIgnore;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features(CoreSearchFeature.class)
@ConditionalIgnore(condition = IgnoreIfRepositorySearchClient.class)
public class TestSearchCompareWithRepositoryQueryAndFetch {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected SearchService searchService;

    @Inject
    protected TrashService trashService;

    @Inject
    protected TransactionalFeature txFeature;

    @Before
    public void initWorkingDocuments() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2000, JANUARY, 2, 3, 4, 5);
        cal.set(Calendar.MILLISECOND, 6);
        for (int i = 0; i < 5; i++) {
            String name = "file" + i;
            DocumentModel doc = session.createDocumentModel("/", name, "File");
            doc.setPropertyValue("dc:title", "File" + i);
            doc.setPropertyValue("dc:nature", "Nature" + i);
            doc.setPropertyValue("dc:rights", "Rights" + i % 2);
            doc.setPropertyValue("dc:issued", cal);
            doc = session.createDocument(doc);
        }
        for (int i = 5; i < 10; i++) {
            String name = "note" + i;
            DocumentModel doc = session.createDocumentModel("/", name, "Note");
            doc.setPropertyValue("dc:title", "Note" + i);
            doc.setPropertyValue("note:note", "Content" + i);
            doc.setPropertyValue("dc:nature", "Nature" + i);
            doc.setPropertyValue("dc:rights", "Rights" + i % 2);
            doc = session.createDocument(doc);
        }

        DocumentModel doc = session.createDocumentModel("/", "hidden", "HiddenFolder");
        doc.setPropertyValue("dc:title", "HiddenFolder");
        doc = session.createDocument(doc);

        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder.setPropertyValue("dc:title", "Folder");
        folder = session.createDocument(folder);

        DocumentModel file = session.getDocument(new PathRef("/file3"));
        DocumentModel proxy = session.publishDocument(file, folder);

        trashService.trashDocument(session.getDocument(new PathRef("/file1")));
        trashService.trashDocument(session.getDocument(new PathRef("/note5")));

        session.checkIn(new PathRef("/file2"), VersioningOption.MINOR, "for testing");

        // wait for async jobs
        txFeature.nextTransaction();
    }

    protected String getDigest(IterableQueryResult docs) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Serializable> doc : docs) {
            List<String> keys = new ArrayList<>(doc.keySet());
            Collections.sort(keys);
            Map<String, Serializable> sortedMap = new LinkedHashMap<>();
            for (String key : keys) {
                Serializable value = doc.get(key);
                if (value instanceof Calendar) {
                    // ISO 8601
                    value = String.format("%tFT%<tT.%<tL%<tz", (Calendar) value);
                }
                if (key.equals("ecm:repository")) {
                    // search service has extra key in the result set, ignore it
                    continue;
                } else if (coreFeature.getStorageConfiguration().isDBS()) {
                    if (key.equals("ecm:name") || key.equals("ecm:parentId")) {
                        // MongoDB has extra keys in the result set, ignore them
                        continue;
                    }
                    if (value == null) {
                        // MongoDB returns explicit nulls
                        continue;
                    }
                }
                sortedMap.put(key, value);
            }
            sb.append(sortedMap.entrySet());
            sb.append("\n");
        }
        return sb.toString();
    }

    protected void assertSameDocumentLists(IterableQueryResult expected, IterableQueryResult actual) {
        assertEquals(getDigest(expected), getDigest(actual));
    }

    protected void compareSearchAndCore(String nxql) {
        try (IterableQueryResult coreResult = session.queryAndFetch(nxql, NXQL.NXQL);
                IterableQueryResult searchResult = searchService.search(
                        SearchQuery.builder(nxql, session).scrollSize(20).build()).getHitsAsIterator()) {
            assertSameDocumentLists(coreResult, searchResult);
        }
    }

    @Test
    public void testSimpleSearchWithSort() {
        compareSearchAndCore("select ecm:uuid, dc:title, dc:nature from Document order by ecm:uuid");
        compareSearchAndCore("select ecm:uuid, dc:title from Document where ecm:isTrashed = 0 order by ecm:uuid");
        compareSearchAndCore("select ecm:uuid, dc:nature from File order by dc:nature, ecm:uuid");
    }
}
