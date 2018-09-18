/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Funsho David
 *
 */

package org.nuxeo.elasticsearch.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 9.1
 */
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.elasticsearch.core.test:elasticsearch-test-contrib.xml")
public class TestHighlight {

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected ElasticSearchAdmin esa;

    @Inject
    protected WorkManager workManager;

    public void waitForCompletion() throws Exception {
        workManager.awaitCompletion(20, TimeUnit.SECONDS);
        esa.prepareWaitForIndexing().get(20, TimeUnit.SECONDS);
        esa.refresh();
    }

    public void startTransaction() {
        if (!TransactionHelper.isTransactionActive()) {
            TransactionHelper.startTransaction();
        }
    }

    @Before
    public void setUpMapping() throws Exception {
        esa.initIndexes(true);
    }

    @Test
    public void testHighlight() throws Exception {

        DocumentModel doc = session.createDocumentModel("/", "highlight", "File");
        doc.setPropertyValue("dc:title", "Search me");
        BlobHolder holder = doc.getAdapter(BlobHolder.class);
        holder.setBlob(new StringBlob("you know for search"));
        session.createDocument(doc);
        session.save();

        DocumentModel doc2 = session.createDocumentModel("/", "highlight2", "File");
        BlobHolder holder2 = doc2.getAdapter(BlobHolder.class);
        holder2.setBlob(new StringBlob("test my search with highlight"));
        session.createDocument(doc2);
        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        TransactionHelper.startTransaction();

        SearchSourceBuilder request = new SearchSourceBuilder();
        List<String> highlightFields = Arrays.asList("dc:title.fulltext", "ecm:binarytext");

        NxQueryBuilder queryBuilder = new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE ecm:fulltext='search'").highlight(highlightFields);
        queryBuilder.updateRequest(request);
        DocumentModelList ret = ess.query(queryBuilder);

        assertEquals(2, ret.totalSize());

        Map<String, List<String>> highlights = (Map<String, List<String>>) ret.get(0).getContextData(
                PageProvider.HIGHLIGHT_CTX_DATA);
        assertEquals(2, highlights.size());
        assertTrue(highlights.containsKey("dc:title.fulltext"));
        assertTrue(highlights.containsKey("ecm:binarytext"));
        assertEquals("<em>Search</em> me", highlights.get("dc:title.fulltext").get(0));
        assertEquals("you know for <em>search</em>", highlights.get("ecm:binarytext").get(0));

        Map<String, List<String>> highlights2 = (Map<String, List<String>>) ret.get(1).getContextData(
                PageProvider.HIGHLIGHT_CTX_DATA);
        assertEquals("test my <em>search</em> with highlight", highlights2.get("ecm:binarytext").get(0));
    }

    @Test
    public void testMultipleHighlights() throws Exception {

        startTransaction();
        DocumentModel doc = session.createDocumentModel("/", "multipleHighlights", "File");
        BlobHolder holder = doc.getAdapter(BlobHolder.class);

        String content = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. "
                + "Donec sed diam nec ante auctor tempor. Aliquam placerat porta lectus sit amet convallis. "
                + "Nam vel tincidunt nunc. Nullam leo tortor, tristique non consectetur sit amet, auctor vel justo. "
                + "Donec euismod congue lacus, ac volutpat magna dapibus at. Cras tincidunt fringilla sem, "
                + "et rhoncus leo aliquet sit amet. Aenean vel euismod risus, eu rhoncus erat. Nulla neque dui, "
                + "egestas sit amet nibh eget, maximus vehicula nisi.\n"
                + "Aenean non eros id mauris imperdiet tristique. Fusce bibendum convallis magna in varius. "
                + "Nullam eu ornare libero. Maecenas dignissim gravida lobortis. Nunc id pellentesque lorem. "
                + "Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; "
                + "Fusce nec ipsum accumsan, tincidunt mi et, luctus turpis.\n"
                + "Suspendisse consequat erat vitae felis pulvinar congue. Phasellus ullamcorper, "
                + "massa at convallis molestie, diam turpis ornare diam, ac rutrum diam odio vel dolor. "
                + "Fusce facilisis a risus a molestie. Ut nulla diam, luctus et bibendum ut, gravida id justo. "
                + "Proin ipsum sem, rhoncus et nisi a, pellentesque consectetur quam. "
                + "Vestibulum dignissim pulvinar ipsum a accumsan. Nam nulla risus, pretium eget pellentesque pulvinar, "
                + "scelerisque vel ante. Vivamus viverra erat diam, non mollis arcu elementum in. "
                + "Nunc rutrum feugiat orci et auctor. Aliquam a quam felis. Sed aliquet vel orci ut tincidunt. "
                + "Aenean id porttitor nisi.\n"
                + "Aliquam erat volutpat. Aliquam et metus at arcu lobortis gravida in nec mauris. Aenean quam risus, "
                + "vestibulum sed mattis ac, lobortis quis nunc. Nam eu est nunc. Proin ac libero vehicula, "
                + "mollis turpis quis, tempus sem. Donec vehicula, ante eget porttitor hendrerit, "
                + "orci nibh rhoncus turpis, vel tempor turpis dui eget turpis. Mauris efficitur purus nibh, "
                + "et tincidunt leo iaculis sit amet. Suspendisse at augue ut eros congue posuere. "
                + "Morbi at suscipit augue. Morbi tincidunt eros eu rutrum semper.\n"
                + "Nam auctor, enim vel lacinia vehicula, leo leo porttitor massa, in blandit tortor ante quis ligula."
                + " Proin et interdum metus. Cras feugiat consectetur ornare. Phasellus ante nibh, "
                + "tincidunt ut venenatis sit amet, condimentum ac lacus. Maecenas ullamcorper aliquet turpis, "
                + "eu rutrum purus tempus in. Vivamus volutpat erat a odio pharetra, sed volutpat elit congue. "
                + "Mauris sit amet condimentum mi. Pellentesque est libero, tempus et elementum id, imperdiet ut risus."
                + " Suspendisse eu lorem metus. Mauris tempus quis lectus sit amet hendrerit.";
        holder.setBlob(new StringBlob(content));
        session.createDocument(doc);
        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        TransactionHelper.startTransaction();

        SearchSourceBuilder request = new SearchSourceBuilder();

        List<String> highlightFields = Arrays.asList("dc:title", "ecm:binarytext");

        NxQueryBuilder queryBuilder = new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE ecm:fulltext='vehicula'").highlight(highlightFields);
        queryBuilder.updateRequest(request);
        DocumentModelList ret = ess.query(queryBuilder);

        assertEquals(1, ret.totalSize());

        Map<String, List<String>> highlights = (Map<String, List<String>>) ret.get(0).getContextData(
                PageProvider.HIGHLIGHT_CTX_DATA);
        assertEquals(1, highlights.size());
        assertEquals(4, highlights.get("ecm:binarytext").size());
        assertEquals(
                "Nulla neque dui, egestas sit amet nibh eget, maximus <em>vehicula</em> nisi.",
                highlights.get("ecm:binarytext").get(0));
        assertEquals(
                "Proin ac libero <em>vehicula</em>, mollis turpis quis, tempus sem.",
                highlights.get("ecm:binarytext").get(1));
        assertEquals(
                "Donec <em>vehicula</em>, ante eget porttitor hendrerit, orci nibh rhoncus turpis, vel tempor turpis dui eget turpis",
                highlights.get("ecm:binarytext").get(2));
        assertEquals(
                "Nam auctor, enim vel lacinia <em>vehicula</em>, leo leo porttitor massa, in blandit tortor ante quis ligula.",
                highlights.get("ecm:binarytext").get(3));

    }

}
