/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.ui.web.restAPI;

import java.net.URLEncoder;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.types.core")
public class TestOpenSearchRestlet extends AbstractRestletTest {

    protected static final String ENDPOINT = "/opensearch";

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected EventService eventService;

    protected String repositoryName;

    protected DocumentModel root;

    protected DocumentModel doc;

    @Before
    public void before() {
        repositoryName = session.getRepositoryName();
        root = session.getRootDocument();
        doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("dc:title", "hello world");
        doc.setPropertyValue("dc:description", "this is my doc");
        doc = session.createDocument(doc);
        session.save();
        txFeature.nextTransaction();
        eventService.waitForAsyncCompletion(10_000);
    }

    @Test
    public void testSearch() throws Exception {
        String query = "hello";
        String encodedQuery = URLEncoder.encode(query, "UTF-8");
        String path = ENDPOINT + "?q=" + encodedQuery;
        String expected = XML //
                + "<rss xmlns:opensearch=\"http://a9.com/-/spec/opensearch/1.1/\"" //
                + " xmlns:atom=\"http://www.w3.org/2005/Atom\"" //
                + " version=\"2.0\">" //
                + "<channel>" //
                + "<title>Nuxeo EP OpenSearch channel for " + query + "</title>" //
                + "<link>http://localhost:" + PORT + "/restAPI/opensearch?q=" + encodedQuery + "</link>" //
                + "<opensearch:totalResults>1</opensearch:totalResults>" //
                + "<opensearch:startIndex>0</opensearch:startIndex>" //
                + "<opensearch:itemsPerPage>1</opensearch:itemsPerPage>" //
                + "<opensearch:Query role=\"request\" searchTerms=\"" + query + "\" startPage=\"1\"/>" //
                + "<item>" //
                + "<title>" + doc.getTitle() + "</title>" //
                + "<description>" + doc.getPropertyValue("dc:description") + "</description>" //
                + "<link>http://localhost:" + PORT + "/nxpath/" + repositoryName + doc.getPathAsString() + "</link>"
                + "</item>" //
                + "</channel>" //
                + "</rss>";
        executeRequest(path, expected);
    }

}
