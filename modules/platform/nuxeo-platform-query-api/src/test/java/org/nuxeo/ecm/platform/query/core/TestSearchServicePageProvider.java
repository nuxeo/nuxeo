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
 *     Nuxeo
 */
package org.nuxeo.ecm.platform.query.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreSearchFeature;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.SearchServicePageProvider;
import org.nuxeo.runtime.test.runner.ConsoleLogLevelThreshold;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@SuppressWarnings("unchecked")
@RunWith(FeaturesRunner.class)
@Features(CoreSearchFeature.class)
@Deploy("org.nuxeo.ecm.core.query.test:OSGI-INF/test-aggregate-schemas-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.query.api.test:test-searchservice-pageprovider-contrib.xml")
public class TestSearchServicePageProvider {

    @Inject
    protected CoreSession session;

    @Inject
    protected PageProviderService pageProviderService;

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void ICanUseANxqlPageProvider() {
        PageProviderDefinition ppdef = pageProviderService.getPageProviderDefinition("NXQL_PP_PATTERN");
        assertNotNull(ppdef);

        HashMap<String, Serializable> props = new HashMap<>();
        props.put(SearchServicePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        long pageSize = 5;
        SearchServicePageProvider pp = (SearchServicePageProvider) pageProviderService.getPageProvider(
                "NXQL_PP_PATTERN", ppdef, null, null, pageSize, 0L, props);
        assertNotNull(pp);

        // create 10 docs
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i, "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc = session.createDocument(doc);
        }
        txFeature.nextTransaction();

        // get current page
        List<DocumentModel> p = pp.getCurrentPage();
        assertEquals(10, pp.getResultsCount());
        assertNotNull(p);
        assertEquals(pageSize, p.size());
        assertEquals(2, pp.getNumberOfPages());
        DocumentModel doc = p.get(0);
        assertEquals("TestMe9", doc.getTitle());

        assertTrue(pp.isLastPageAvailable());
        assertTrue(pp.isNextPageAvailable());

        pp.nextPage();
        p = pp.getCurrentPage();
        assertEquals(pageSize, p.size());
        doc = p.get((int) pageSize - 1);
        assertEquals("TestMe0", doc.getTitle());

        pageSize = 10000;
        ppdef = pageProviderService.getPageProviderDefinition("NXQL_PP_PATTERN2");
        assertNotNull(ppdef);
        pp = (SearchServicePageProvider) pageProviderService.getPageProvider("NXQL_PP_PATTERN2", ppdef, null, null,
                pageSize, 0L, props);
        assertNotNull(pp);
        p = pp.getCurrentPage();
        assertEquals(10, pp.getResultsCount());
        assertEquals(10, p.size());
        doc = p.get(0);
        assertEquals("TestMe9", doc.getTitle());

    }

    @Test
    public void ICanUseANxqlPageProviderWithParameters() {
        PageProviderDefinition ppdef = pageProviderService.getPageProviderDefinition("nxql_search");
        assertNotNull(ppdef);
        HashMap<String, Serializable> props = new HashMap<>();
        props.put(SearchServicePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        long pageSize = 5;
        PageProvider<?> pp = pageProviderService.getPageProvider("nxql_search", ppdef, null, null, pageSize, 0L, props);
        // create 10 docs
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i, "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc = session.createDocument(doc);
        }
        txFeature.nextTransaction();

        // get current page
        String[] params = { "Select * from File where dc:title LIKE 'Test%'" };
        pp.setParameters(params);
        List<DocumentModel> p = (List<DocumentModel>) pp.getCurrentPage();
        assertEquals(10, pp.getResultsCount());
        assertNotNull(p);
        assertEquals(pageSize, p.size());
        assertEquals(2, pp.getNumberOfPages());
        p.get(0);
    }

    @Test
    public void ICanUseANxqlPageProviderWithFixedPart() {
        PageProviderDefinition ppdef = pageProviderService.getPageProviderDefinition("NXQL_PP_FIXED_PART");
        assertNotNull(ppdef);
        HashMap<String, Serializable> props = new HashMap<>();
        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        String[] sources = { "Source1", "Source2" };
        model.setProperty("advanced_search", "source_agg", sources);
        props.put(SearchServicePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        long pageSize = 5;
        PageProvider<?> pp = pageProviderService.getPageProvider("NXQL_PP_FIXED_PART", ppdef, model, null, pageSize, 0L,
                props);
        // create 10 docs
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i, "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc = session.createDocument(doc);
        }

        txFeature.nextTransaction();

        String[] params = { session.getRootDocument().getId() };
        pp.setParameters(params);

        // get current page
        List<DocumentModel> p = (List<DocumentModel>) pp.getCurrentPage();
        assertEquals(10, pp.getResultsCount());
        assertNotNull(p);
        assertEquals(pageSize, p.size());
        assertEquals(2, pp.getNumberOfPages());
        p.get(0);
    }

    @Test
    @ConsoleLogLevelThreshold("ERROR")
    public void ICanUseInvalidPageProvider() {
        PageProviderDefinition ppdef = pageProviderService.getPageProviderDefinition("INVALID_PP");
        assertNotNull(ppdef);
        HashMap<String, Serializable> props = new HashMap<>();
        props.put(SearchServicePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        PageProvider<?> pp = pageProviderService.getPageProvider("INVALID_PP", ppdef, null, null, 0L, 0L, props);
        assertNotNull(pp);
        List<?> p = pp.getCurrentPage();
        assertNotNull(p);
        assertEquals(0, p.size());
        assertEquals(
                "Failed to execute query: SELECT * FROM Document WHERE ORDER BY dc:title, Syntax error: Invalid token <ORDER BY> at offset 29",
                pp.getErrorMessage());
    }

    @Test
    public void testMaxResultWindow() {
        PageProviderDefinition ppdef = pageProviderService.getPageProviderDefinition("NXQL_PP_PATTERN");
        HashMap<String, Serializable> props = new HashMap<>();
        props.put(SearchServicePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        long pageSize = 2;
        SearchServicePageProvider pp = (SearchServicePageProvider) pageProviderService.getPageProvider(
                "NXQL_PP_PATTERN", ppdef, null, null, pageSize, 0L, props);
        pp.setMaxResultWindow(6);
        assertEquals(6, pp.getMaxResultWindow());
        // create 10 docs
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i, "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc = session.createDocument(doc);
        }
        txFeature.nextTransaction();

        // get current page
        pp.getCurrentPage();
        assertEquals(10, pp.getResultsCount());
        assertEquals(5, pp.getNumberOfPages());
        assertTrue(pp.isNextPageAvailable());
        // last page is not accessible
        assertFalse(pp.isLastPageAvailable());
        // only 3 pages are navigable
        assertEquals(3, pp.getPageLimit());
        // page 2
        pp.nextPage();
        assertTrue(pp.isNextPageAvailable());
        // page 3 reach the max result window of 6 docs
        pp.nextPage();
        assertFalse(pp.isNextPageAvailable());
        assertFalse(pp.isLastPageAvailable());
    }

    /**
     * Testing an ES page provider when not specifying limit. This shouldn't crash and be boxed by
     * {@link SearchServicePageProvider#MAX_RESULTS_PROPERTY}.
     *
     * @since 10.3
     */
    @Test
    public void iCanPerformUnlimitedQuery() {
        // create 10 docs
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i, "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc = session.createDocument(doc);
        }
        txFeature.nextTransaction();

        PageProviderDefinition ppdef = pageProviderService.getPageProviderDefinition("NXQL_PP_UNLIMITED");
        HashMap<String, Serializable> props = new HashMap<>();
        props.put(SearchServicePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        PageProvider<?> pp = pageProviderService.getPageProvider("NXQL_PP_UNLIMITED", ppdef, null, null, null, 0L,
                props);
        List<?> page = pp.getCurrentPage();
        // here we test that ES doesn't throw an exception + we're able to retrieve something
        assertFalse(page.isEmpty());
    }

    @Test
    public void ICanUseANxqlPageProviderWithUnrestrictedSession() {

        PageProviderDefinition ppdef = pageProviderService.getPageProviderDefinition("UNRESTRICTED_PP");

        HashMap<String, Serializable> props = new HashMap<>();
        CoreSession bobSession = CoreInstance.getCoreSession(session.getRepositoryName(), "bob");
        props.put(SearchServicePageProvider.CORE_SESSION_PROPERTY, (Serializable) bobSession);
        SearchServicePageProvider pp = (SearchServicePageProvider) pageProviderService.getPageProvider(
                "UNRESTRICTED_PP", ppdef, null, null, null, 0L, props);

        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i, "File");
            session.createDocument(doc);
        }

        txFeature.nextTransaction();

        List<DocumentModel> docs = pp.getCurrentPage();
        assertEquals(10, docs.size());
    }

    /**
     * @since 2021.8
     */
    @Test
    public void testPageProviderScroller() {
        PageProvider<?> pageProvider = pageProviderService.getPageProvider("NXQL_PP_PATTERN", null, null, null, null);
        assertEquals("search", pageProvider.getScroller());
    }

}
