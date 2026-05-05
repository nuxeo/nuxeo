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
package org.nuxeo.ecm.platform.query.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.MultiRepositorySearchFeature;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.SearchServicePageProvider;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@SuppressWarnings("unchecked")
@RunWith(FeaturesRunner.class)
@Features({ MultiRepositorySearchFeature.class })
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.query.api.test:test-searchservice-pageprovider-contrib.xml")
public class TestSearchServicePageProviderMultiRepository {

    @Inject
    protected CoreSession session;

    @Inject
    @Named("other")
    protected CoreSession otherSession;

    @Inject
    protected PageProviderService pageProviderService;

    @Inject
    protected TransactionalFeature txFeature;

    protected void createDocs() {
        var doc = session.createDocumentModel("/", "my-file", "File");
        doc.setPropertyValue("dc:title", "file A repo1");
        session.createDocument(doc);

        doc = session.createDocumentModel("/", "my-file", "File");
        doc.setPropertyValue("dc:title", "file C repo1");
        session.createDocument(doc);
        txFeature.nextTransaction();

        doc = otherSession.createDocumentModel("/", "my-file", "File");
        doc.setPropertyValue("dc:title", "file B repo2");
        otherSession.createDocument(doc);
        txFeature.nextTransaction();
    }

    @Test
    public void testMultiRepositorySearch() {
        createDocs();
        SearchServicePageProvider pp = getPP("SEARCH_ALL_REPOSITORIES_PP");
        List<DocumentModel> p = pp.getCurrentPage();
        assertNotNull(p);
        assertEquals(3, pp.getResultsCount());
        assertEquals(1, pp.getNumberOfPages());
        DocumentModel doc1 = p.get(0);
        DocumentModel doc2 = p.get(1);
        DocumentModel doc3 = p.get(2);
        assertEquals("file A repo1", doc1.getTitle());
        assertEquals("file B repo2", doc2.getTitle());
        assertEquals("file C repo1", doc3.getTitle());
    }

    protected SearchServicePageProvider getPP(String name) {
        PageProviderDefinition ppdef = pageProviderService.getPageProviderDefinition(name);
        assertNotNull(ppdef);
        HashMap<String, Serializable> props = new HashMap<>();
        props.put(SearchServicePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        SearchServicePageProvider pp = (SearchServicePageProvider) pageProviderService.getPageProvider(name, ppdef,
                null, null, 10L, 0L, props);
        assertNotNull(pp);
        return pp;
    }

}
