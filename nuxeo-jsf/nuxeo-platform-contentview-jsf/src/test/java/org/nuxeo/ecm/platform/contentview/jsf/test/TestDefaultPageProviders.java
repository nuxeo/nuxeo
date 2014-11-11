/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.contentview.jsf.test;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.el.ELException;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewService;
import org.nuxeo.ecm.platform.query.api.AbstractPageProvider;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageSelections;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryAndFetchPageProvider;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.platform.ui.web.jsf.MockFacesContext;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Anahide Tchertchian
 */
public class TestDefaultPageProviders extends SQLRepositoryTestCase {

    private static final Log log = LogFactory.getLog(TestDefaultPageProviders.class);

    ContentViewService service;

    MockFacesContext facesContext;

    DocumentModel searchDocument;

    private String dummyParam = UUID.randomUUID().toString();

    @Before
    public void setUp() throws Exception {
        super.setUp();

        deployContrib("org.nuxeo.ecm.platform.query.api",
                "OSGI-INF/pageprovider-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.contentview.jsf",
                "OSGI-INF/contentview-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.contentview.jsf.test",
                "test-contentview-contrib.xml");

        openSession();

        searchDocument = session.createDocumentModel("File");

        final DocumentModel rootDoc = session.getRootDocument();

        // set mock faces context for needed properties resolution
        facesContext = new MockFacesContext() {
            @Override
            @SuppressWarnings("rawtypes")
            public Object evaluateExpressionGet(FacesContext context,
                    String expression, Class expectedType) throws ELException {
                if ("#{documentManager}".equals(expression)) {
                    return session;
                }
                if ("#{searchDocument}".equals(expression)) {
                    return searchDocument;
                }
                if ("#{dummy.param}".equals(expression)) {
                    dummyParam = UUID.randomUUID().toString();
                    return dummyParam;
                }
                if ("#{currentDocument.id}".equals(expression)) {
                    return rootDoc.getId();
                } else {
                    log.error("Cannot evaluate expression: " + expression);
                }
                return null;
            }
        };
        facesContext.setCurrent();
        assertNotNull(FacesContext.getCurrentInstance());

        service = Framework.getService(ContentViewService.class);
        assertNotNull(service);

        createTestDocuments();
    }

    @After
    public void tearDown() throws Exception {
        closeSession();
        facesContext.relieveCurrent();
        super.tearDown();
    }

    protected void createTestDocuments() throws ClientException {
        DocumentModel root = session.getRootDocument();
        // create docs in descending order so that docs are not ordered by
        // title by default
        for (int i = 4; i >= 0; i--) {
            DocumentModel doc = session.createDocumentModel("Folder");
            doc.setPropertyValue("dc:title", "Document number" + i); // no space
            doc.setPathInfo(root.getPathAsString(), "doc_" + i);
            session.createDocument(doc);
        }
        session.save();
        // wait for fulltext indexing
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
        DatabaseHelper.DATABASE.sleepForFulltext();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCoreQuery() throws Exception {
        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN");
        assertNotNull(contentView);

        String parentIdParam = session.getRootDocument().getId();
        assertEquals(parentIdParam, contentView.getCacheKey());

        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) contentView.getPageProviderWithParams(parentIdParam, dummyParam);
        checkCoreQuery(parentIdParam, pp);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCoreQueryReference() throws Exception {
        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN_REF");
        assertNotNull(contentView);

        String parentIdParam = session.getRootDocument().getId();
        assertEquals(parentIdParam, contentView.getCacheKey());

        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) contentView.getPageProviderWithParams(parentIdParam, dummyParam);
        checkCoreQuery(parentIdParam, pp);
    }

    protected void checkCoreQuery(String parentIdParam,
            PageProvider<DocumentModel> pp) throws Exception {

        assertNotNull(pp);

        assertEquals(-1, pp.getResultsCount());
        assertEquals(0, pp.getNumberOfPages());

        // init results
        List<DocumentModel> docs = pp.getCurrentPage();

        // check query
        assertTrue(pp instanceof CoreQueryDocumentPageProvider);
        assertEquals(
                String.format(
                        "SELECT * FROM Document WHERE ecm:parentId = '%s'"
                                + " AND ecm:isCheckedInVersion = 0"
                                + " AND ecm:mixinType != 'HiddenInNavigation'"
                                + " AND ecm:currentLifeCycleState != 'deleted'"
                                + " AND ecm:parentId != '%s'"
                                + " ORDER BY dc:title",
                        parentIdParam, dummyParam),
                ((CoreQueryDocumentPageProvider) pp).getCurrentQuery());

        assertEquals(5, pp.getResultsCount());
        assertEquals(3, pp.getNumberOfPages());
        assertFalse(pp.isPreviousPageAvailable());
        assertTrue(pp.isNextPageAvailable());

        assertNotNull(docs);
        assertEquals(2, docs.size());
        assertEquals("Document number0", docs.get(0).getPropertyValue(
                "dc:title"));
        assertEquals("Document number1", docs.get(1).getPropertyValue(
                "dc:title"));

        pp.nextPage();
        docs = pp.getCurrentPage();

        assertEquals(
                String.format(
                        "SELECT * FROM Document WHERE ecm:parentId = '%s'"
                                + " AND ecm:isCheckedInVersion = 0"
                                + " AND ecm:mixinType != 'HiddenInNavigation'"
                                + " AND ecm:currentLifeCycleState != 'deleted'"
                                + " AND ecm:parentId != '%s'"
                                + " ORDER BY dc:title",
                        parentIdParam, dummyParam),
                ((CoreQueryDocumentPageProvider) pp).getCurrentQuery());

        assertEquals(5, pp.getResultsCount());
        assertEquals(3, pp.getNumberOfPages());
        assertTrue(pp.isPreviousPageAvailable());
        assertTrue(pp.isNextPageAvailable());

        assertNotNull(docs);
        assertEquals(2, docs.size());
        assertEquals("Document number2", docs.get(0).getPropertyValue(
                "dc:title"));
        assertEquals("Document number3", docs.get(1).getPropertyValue(
                "dc:title"));

        // test selection
        pp.setSelectedEntries(Arrays.asList(new DocumentModel[] { docs.get(1) }));
        PageSelections<DocumentModel> selections = pp.getCurrentSelectPage();
        assertNotNull(selections);
        assertEquals(2, selections.getSize());
        assertFalse(selections.isSelected());
        assertEquals("Document number2",
                selections.getEntries().get(0).getData().getPropertyValue(
                        "dc:title"));
        assertFalse(selections.getEntries().get(0).isSelected());
        assertEquals("Document number3",
                selections.getEntries().get(1).getData().getPropertyValue(
                        "dc:title"));
        assertTrue(selections.getEntries().get(1).isSelected());
    }

    // same test but taking XML parameters instead of passing them through API
    // calls
    @SuppressWarnings("unchecked")
    @Test
    public void testCoreQueryWithXMLParameters() throws Exception {
        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN");
        assertNotNull(contentView);

        String parentIdParam = session.getRootDocument().getId();
        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) contentView.getPageProvider();
        checkCoreQueryWithXMLParameter(parentIdParam, pp);

        // Test that a new page provider is returned when one of the parameter changes.
        // In this case, it's dummy.param that always changes.
        PageProvider<DocumentModel> pp2 = (PageProvider<DocumentModel>) contentView.getPageProvider();
        assertTrue(!pp2.equals(pp));
        assertTrue(!pp2.getParameters()[1].equals(pp.getParameters()[1]));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCoreQueryWithXMLParametersReference() throws Exception {
        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN_REF");
        assertNotNull(contentView);

        String parentIdParam = session.getRootDocument().getId();
        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) contentView.getPageProvider();
        checkCoreQueryWithXMLParameter(parentIdParam, pp);
    }

    protected void checkCoreQueryWithXMLParameter(String parentIdParam,
            PageProvider<DocumentModel> pp) throws Exception {
        assertNotNull(pp);

        assertEquals(-1, pp.getResultsCount());
        assertEquals(0, pp.getNumberOfPages());

        // init results
        List<DocumentModel> docs = pp.getCurrentPage();

        // check query
        assertTrue(pp instanceof CoreQueryDocumentPageProvider);
        assertEquals(
                String.format(
                        "SELECT * FROM Document WHERE ecm:parentId = '%s'"
                                + " AND ecm:isCheckedInVersion = 0"
                                + " AND ecm:mixinType != 'HiddenInNavigation'"
                                + " AND ecm:currentLifeCycleState != 'deleted'"
                                + " AND ecm:parentId != '%s'"
                                + " ORDER BY dc:title",
                        parentIdParam, dummyParam),
                ((CoreQueryDocumentPageProvider) pp).getCurrentQuery());

        assertEquals(5, pp.getResultsCount());
        assertEquals(3, pp.getNumberOfPages());
        assertFalse(pp.isPreviousPageAvailable());
        assertTrue(pp.isNextPageAvailable());

        assertNotNull(docs);
        assertEquals(2, docs.size());
        assertEquals("Document number0", docs.get(0).getPropertyValue(
                "dc:title"));
        assertEquals("Document number1", docs.get(1).getPropertyValue(
                "dc:title"));

        pp.nextPage();
        docs = pp.getCurrentPage();

        assertEquals(
                String.format(
                        "SELECT * FROM Document WHERE ecm:parentId = '%s'"
                                + " AND ecm:isCheckedInVersion = 0"
                                + " AND ecm:mixinType != 'HiddenInNavigation'"
                                + " AND ecm:currentLifeCycleState != 'deleted'"
                                + " AND ecm:parentId != '%s'"
                                + " ORDER BY dc:title",
                        parentIdParam, dummyParam),
                ((CoreQueryDocumentPageProvider) pp).getCurrentQuery());

        assertEquals(5, pp.getResultsCount());
        assertEquals(3, pp.getNumberOfPages());
        assertTrue(pp.isPreviousPageAvailable());
        assertTrue(pp.isNextPageAvailable());

        assertNotNull(docs);
        assertEquals(2, docs.size());
        assertEquals("Document number2", docs.get(0).getPropertyValue(
                "dc:title"));
        assertEquals("Document number3", docs.get(1).getPropertyValue(
                "dc:title"));

        // test selection
        pp.setSelectedEntries(Arrays.asList(new DocumentModel[] { docs.get(1) }));
        PageSelections<DocumentModel> selections = pp.getCurrentSelectPage();
        assertNotNull(selections);
        assertEquals(2, selections.getSize());
        assertFalse(selections.isSelected());
        assertEquals("Document number2",
                selections.getEntries().get(0).getData().getPropertyValue(
                        "dc:title"));
        assertFalse(selections.getEntries().get(0).isSelected());
        assertEquals("Document number3",
                selections.getEntries().get(1).getData().getPropertyValue(
                        "dc:title"));
        assertTrue(selections.getEntries().get(1).isSelected());
    }

    @Test
    public void testCoreQueryMaxResults() throws Exception {
        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN");
        assertNotNull(contentView);

        String parentIdParam = session.getRootDocument().getId();
        assertEquals(parentIdParam, contentView.getCacheKey());

        CoreQueryDocumentPageProvider pp = (CoreQueryDocumentPageProvider) contentView.getPageProviderWithParams(
                parentIdParam, dummyParam);
        pp.setMaxResults(2);
        assertEquals(2, pp.getPageSize());

        // page 1/3
        List<DocumentModel> docs = pp.getCurrentPage();
        assertEquals(AbstractPageProvider.UNKNOWN_SIZE_AFTER_QUERY,
                pp.getResultsCount());

        assertFalse(pp.isPreviousPageAvailable());
        assertFalse(pp.isLastPageAvailable());
        assertTrue(pp.isNextPageAvailable());

        assertEquals(0, pp.getNumberOfPages());

        assertNotNull(docs);
        assertEquals(2, docs.size());

        // page 2/3
        pp.nextPage();
        docs = pp.getCurrentPage();
        assertTrue(pp.isPreviousPageAvailable());
        assertFalse(pp.isLastPageAvailable());
        assertTrue(pp.isNextPageAvailable());

        assertNotNull(docs);
        assertEquals(2, docs.size());

        // page 3/3
        pp.nextPage();
        docs = pp.getCurrentPage();
        assertTrue(pp.isPreviousPageAvailable());
        assertFalse(pp.isLastPageAvailable());
        // last page detected because there are less than pagesize docs
        assertFalse(pp.isNextPageAvailable());

        assertNotNull(docs);
        assertEquals(1, docs.size());

        // page 4/3
        // dummy case
        pp.nextPage();
        docs = pp.getCurrentPage();
        assertTrue(pp.isPreviousPageAvailable());
        assertFalse(pp.isLastPageAvailable());
        assertFalse(pp.isNextPageAvailable());
        assertNotNull(docs);
        assertEquals(0, docs.size());

        // borderline case
        pp.setPageSize(5);
        pp.setMaxResults(3);
        pp.firstPage();
        pp.refresh();

        // page 1/2
        docs = pp.getCurrentPage();
        assertEquals(AbstractPageProvider.UNKNOWN_SIZE_AFTER_QUERY,
                pp.getResultsCount());

        assertFalse(pp.isPreviousPageAvailable());
        assertFalse(pp.isLastPageAvailable());
        // correct because we don't know if there is another page
        assertTrue(pp.isNextPageAvailable());

        assertEquals(0, pp.getNumberOfPages());
        assertNotNull(docs);
        assertEquals(5, docs.size());

        // page 2/2
        // there is no more results, user see an empty page
        pp.nextPage();
        docs = pp.getCurrentPage();
        assertTrue(pp.isPreviousPageAvailable());
        assertFalse(pp.isLastPageAvailable());
        assertFalse(pp.isNextPageAvailable());
        assertNotNull(docs);
        assertEquals(0, docs.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCoreQueryAndFetch() throws Exception {
        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN_FETCH");
        assertNotNull(contentView);

        String parentIdParam = session.getRootDocument().getId();
        PageProvider<Map<String, Serializable>> pp = (PageProvider<Map<String, Serializable>>) contentView.getPageProviderWithParams(parentIdParam);
        checkCoreQueryAndFetch(parentIdParam, pp);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCoreQueryAndFetchReference() throws Exception {
        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN_FETCH_REF");
        assertNotNull(contentView);

        String parentIdParam = session.getRootDocument().getId();
        PageProvider<Map<String, Serializable>> pp = (PageProvider<Map<String, Serializable>>) contentView.getPageProviderWithParams(parentIdParam);
        checkCoreQueryAndFetch(parentIdParam, pp);
    }

    protected void checkCoreQueryAndFetch(String parentIdParam,
            PageProvider<Map<String, Serializable>> pp) throws Exception {
        assertNotNull(pp);

        assertEquals(-1, pp.getResultsCount());
        assertEquals(0, pp.getNumberOfPages());

        // init results
        List<Map<String, Serializable>> docs = pp.getCurrentPage();

        assertEquals(5, pp.getResultsCount());
        assertEquals(3, pp.getNumberOfPages());
        assertTrue(pp.isNextPageAvailable());

        assertNotNull(docs);
        assertEquals(2, docs.size());
        assertEquals(1, docs.get(0).size());
        assertEquals("Document number0", docs.get(0).get("dc:title"));
        assertEquals(1, docs.get(1).size());
        assertEquals("Document number1", docs.get(1).get("dc:title"));

        // check query
        assertTrue(pp instanceof CoreQueryAndFetchPageProvider);
        assertEquals(
                String.format(
                        "SELECT dc:title FROM Document WHERE ecm:parentId = '%s'"
                                + " AND ecm:isCheckedInVersion = 0"
                                + " AND ecm:mixinType != 'HiddenInNavigation'"
                                + " AND ecm:currentLifeCycleState != 'deleted' ORDER BY dc:title",
                        parentIdParam),
                ((CoreQueryAndFetchPageProvider) pp).getCurrentQuery());

        pp.nextPage();
        docs = pp.getCurrentPage();

        assertEquals(
                String.format(
                        "SELECT dc:title FROM Document WHERE ecm:parentId = '%s'"
                                + " AND ecm:isCheckedInVersion = 0"
                                + " AND ecm:mixinType != 'HiddenInNavigation'"
                                + " AND ecm:currentLifeCycleState != 'deleted' ORDER BY dc:title",
                        parentIdParam),
                ((CoreQueryAndFetchPageProvider) pp).getCurrentQuery());

        assertEquals(5, pp.getResultsCount());
        assertEquals(3, pp.getNumberOfPages());
        assertTrue(pp.isPreviousPageAvailable());
        assertTrue(pp.isNextPageAvailable());

        assertNotNull(docs);
        assertEquals(2, docs.size());
        assertEquals(1, docs.get(0).size());
        assertEquals("Document number2", docs.get(0).get("dc:title"));
        assertEquals(1, docs.get(1).size());
        assertEquals("Document number3", docs.get(1).get("dc:title"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCoreQueryAndFetchWithError() throws Exception {

        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN_FETCH");
        assertNotNull(contentView);

        // do not pass params => query will not be built correctly
        PageProvider<Map<String, Serializable>> pp = (PageProvider<Map<String, Serializable>>) contentView.getPageProvider();
        checkCoreQueryAndFetchWithError(pp);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCoreQueryAndFetchWithErrorReference() throws Exception {

        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN_FETCH_REF");
        assertNotNull(contentView);

        // do not pass params => query will not be built correctly
        PageProvider<Map<String, Serializable>> pp = (PageProvider<Map<String, Serializable>>) contentView.getPageProvider();
        checkCoreQueryAndFetchWithError(pp);
    }

    protected void checkCoreQueryAndFetchWithError(
            PageProvider<Map<String, Serializable>> pp) throws Exception {
        assertNotNull(pp);

        assertEquals(-1, pp.getResultsCount());
        assertEquals(0, pp.getNumberOfPages());
        assertNull(pp.getError());
        assertNull(pp.getErrorMessage());

        // init results
        pp.getCurrentPage();

        assertEquals(-1, pp.getResultsCount());
        assertNotNull(pp.getError());
        assertEquals(
                "Failed to execute query: NXQL: SELECT dc:title FROM Document "
                        + "WHERE ecm:parentId = ORDER BY dc:title: "
                        + "org.nuxeo.ecm.core.query.QueryParseException: "
                        + "Syntax error: Invalid token <ORDER BY> "
                        + "at offset 51 in query: SELECT dc:title FROM "
                        + "Document WHERE ecm:parentId = ORDER BY dc:title",
                pp.getErrorMessage());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCoreQueryWithSearchDocument() throws Exception {
        if (!database.supportsMultipleFulltextIndexes()) {
            return;
        }
        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT");
        assertNotNull(contentView);

        // leave default values on doc for now: will filter on all docs with
        // given parent
        String parentIdParam = session.getRootDocument().getId();
        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) contentView.getPageProviderWithParams(parentIdParam);
        checkCoreQueryWithSearchDocument(parentIdParam, pp);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCoreQueryWithSearchDocumentReference() throws Exception {
        if (!database.supportsMultipleFulltextIndexes()) {
            return;
        }
        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT_REF");
        assertNotNull(contentView);

        // leave default values on doc for now: will filter on all docs with
        // given parent
        String parentIdParam = session.getRootDocument().getId();
        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) contentView.getPageProviderWithParams(parentIdParam);
        checkCoreQueryWithSearchDocument(parentIdParam, pp);
    }

    protected void checkCoreQueryWithSearchDocument(String parentIdParam,
            PageProvider<DocumentModel> pp) throws Exception {
        assertNotNull(pp);

        assertEquals(-1, pp.getResultsCount());
        assertEquals(0, pp.getNumberOfPages());

        // init results
        List<DocumentModel> docs = pp.getCurrentPage();

        // check query
        assertTrue(pp instanceof CoreQueryDocumentPageProvider);
        assertEquals(
                String.format(
                        "SELECT * FROM Document WHERE ecm:parentId = '%s'"
                                + " AND ecm:isCheckedInVersion = 0"
                                + " AND ecm:mixinType != 'HiddenInNavigation'"
                                + " AND ecm:currentLifeCycleState != 'deleted' ORDER BY dc:title",
                        parentIdParam),
                ((CoreQueryDocumentPageProvider) pp).getCurrentQuery());

        assertEquals(5, pp.getResultsCount());
        assertEquals(3, pp.getNumberOfPages());
        assertFalse(pp.isPreviousPageAvailable());
        assertTrue(pp.isNextPageAvailable());

        assertNotNull(docs);
        assertEquals(2, docs.size());
        assertEquals("Document number0", docs.get(0).getPropertyValue(
                "dc:title"));
        assertEquals("Document number1", docs.get(1).getPropertyValue(
                "dc:title"));

        // fill search document with some properDocumentModelties
        searchDocument.setPropertyValue("dc:title", "number0");

        docs = pp.getCurrentPage();

        assertEquals(
                String.format(
                        "SELECT * FROM Document WHERE ecm:fulltext.dc:title = 'number0'"
                                + " AND (ecm:parentId = '%s'"
                                + " AND ecm:isCheckedInVersion = 0"
                                + " AND ecm:mixinType != 'HiddenInNavigation'"
                                + " AND ecm:currentLifeCycleState != 'deleted') ORDER BY dc:title",
                        parentIdParam),
                ((CoreQueryDocumentPageProvider) pp).getCurrentQuery());

        assertEquals(1, pp.getResultsCount());
        assertEquals(1, pp.getNumberOfPages());
        assertFalse(pp.isPreviousPageAvailable());
        assertFalse(pp.isNextPageAvailable());

        assertNotNull(docs);
        assertEquals(1, docs.size());
        assertEquals("Document number0", docs.get(0).getPropertyValue(
                "dc:title"));

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCoreQueryWithSearchDocumentWithWhereClause()
            throws Exception {
        ContentView contentView = service.getContentView("QUERY_WITH_SUBCLAUSE");
        assertNotNull(contentView);

        // leave default values on doc for now: will filter on all docs with
        // given parent
        String parentIdParam = session.getRootDocument().getId();
        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) contentView.getPageProviderWithParams(parentIdParam);
        checkCoreQueryWithSearchDocumentWithWhereClause(parentIdParam, pp);
    }

    protected void checkCoreQueryWithSearchDocumentWithWhereClause(
            String parentIdParam, PageProvider<DocumentModel> pp)
            throws Exception {
        // init results
        List<DocumentModel> docs = pp.getCurrentPage();

        // check query
        assertTrue(pp instanceof CoreQueryDocumentPageProvider);
        assertEquals(
                String.format(
                        "SELECT * FROM Document WHERE ecm:parentId = '%s'"
                                + " AND ecm:isCheckedInVersion = 0"
                                + " AND ecm:mixinType != 'HiddenInNavigation'"
                                + " AND ecm:currentLifeCycleState != 'deleted' ORDER BY dc:title",
                        parentIdParam),
                ((CoreQueryDocumentPageProvider) pp).getCurrentQuery());
    }

}
