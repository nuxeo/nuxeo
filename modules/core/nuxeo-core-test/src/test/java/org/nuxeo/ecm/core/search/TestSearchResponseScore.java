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
package org.nuxeo.ecm.core.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.nuxeo.ecm.platform.query.api.PageProvider.SCORE_CTX_DATA;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests that {@link SearchResponseImpl#loadDocuments} correctly attaches the relevance score as context data.
 *
 * @since 2025.22
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestSearchResponseScore {

    @Inject
    protected CoreSession session;

    @Test
    public void testScoreAttachedToDocument() {
        var doc = session.createDocumentModel("/", "testScore", "File");
        doc = session.createDocument(doc);
        session.save();

        var hit = SearchHit.builder("idx", doc.getId())
                           .repository(session.getRepositoryName())
                           .docId(doc.getId())
                           .score(1.5)
                           .build();

        var docs = SearchResponse.builder(List.of(hit)).total(1).build().loadDocuments(session);

        assertEquals(1, docs.size());
        assertEquals(1.5, (Double) docs.get(0).getContextData(SCORE_CTX_DATA), 0.0);
    }

    @Test
    public void testNoScoreWhenNotSet() {
        var doc = session.createDocumentModel("/", "testNoScore", "File");
        doc = session.createDocument(doc);
        session.save();

        var hit = SearchHit.builder("idx", doc.getId())
                           .repository(session.getRepositoryName())
                           .docId(doc.getId())
                           .build();

        var docs = SearchResponse.builder(List.of(hit)).total(1).build().loadDocuments(session);

        assertEquals(1, docs.size());
        assertNull(docs.get(0).getContextData(SCORE_CTX_DATA));
    }
}
