/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.el.ELException;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewLayout;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewLayoutImpl;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewService;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewState;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewStateImpl;
import org.nuxeo.ecm.platform.contentview.json.JSONContentViewState;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.ui.web.jsf.MockFacesContext;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.4.2
 */
public class TestContentViewState extends SQLRepositoryTestCase {

    private static final Log log = LogFactory.getLog(TestContentViewState.class);

    ContentViewService service;

    MockFacesContext facesContext;

    DocumentModel currentDocument;

    DocumentModel searchDocument;

    @Override
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        super.setUp();

        deployContrib("org.nuxeo.ecm.platform.query.api",
                "OSGI-INF/pageprovider-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.contentview.jsf",
                "OSGI-INF/contentview-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.contentview.jsf.test",
                "test-contentview-contrib.xml");

        service = Framework.getService(ContentViewService.class);
        assertNotNull(service);

        openSession();
        searchDocument = session.createDocumentModel("File");
        searchDocument.setPropertyValue("dc:title", "search keywords");

        currentDocument = session.getRootDocument();

        // set mock faces context for needed properties resolution
        facesContext = new MockFacesContext() {
            @Override
            public Object evaluateExpressionGet(FacesContext context,
                    String expression, Class expectedType) throws ELException {
                if ("#{documentManager}".equals(expression)) {
                    return session;
                }
                if ("#{searchDocument}".equals(expression)) {
                    return searchDocument;
                }
                if ("#{currentDocument.id}".equals(expression)) {
                    return currentDocument.getId();
                } else {
                    log.error("Cannot evaluate expression: " + expression);
                }
                return null;
            }
        };
        facesContext.setCurrent();
        assertNotNull(FacesContext.getCurrentInstance());
    }

    @Override
    public void tearDown() throws Exception {
        facesContext.relieveCurrent();
        super.tearDown();
    }

    public void testSaveContentView() throws Exception {
        assertNull(service.saveContentView(null));

        ContentView contentView = service.getContentView(
                "CURRENT_DOCUMENT_CHILDREN", session);
        assertNotNull(contentView);

        // test bare state
        ContentViewState state = service.saveContentView(contentView);
        assertNotNull(state);
        assertEquals("CURRENT_DOCUMENT_CHILDREN", state.getContentViewName());
        assertNull(state.getCurrentPage());
        assertNull(state.getPageSize());
        Object[] queryParams = state.getQueryParameters();
        assertNotNull(queryParams);
        assertEquals(1, queryParams.length);
        assertEquals(currentDocument.getId(), queryParams[0]);
        assertNull(state.getResultColumns());
        ContentViewLayout resultLayout = state.getResultLayout();
        assertNotNull(resultLayout);
        assertEquals("document_listing", resultLayout.getName());
        assertEquals("label.document_listing.layout", resultLayout.getTitle());
        assertTrue(resultLayout.getTranslateTitle());
        assertEquals("/icons/myicon.png", resultLayout.getIconPath());
        assertTrue(resultLayout.getShowCSVExport());
        assertNull(state.getSearchDocumentModel());
        assertNull(state.getSortInfos());

        // init provider, result columns and test save again
        contentView.getPageProviderWithParams("test_parent_id");
        contentView.setCurrentResultLayoutColumns(Arrays.asList(new String[] { "column_1" }));
        state = service.saveContentView(contentView);
        checkContentViewState(state);
    }

    protected void checkContentViewState(ContentViewState state) {
        assertNotNull(state);
        assertEquals("CURRENT_DOCUMENT_CHILDREN", state.getContentViewName());
        assertEquals(new Long(0), state.getCurrentPage());
        assertEquals(new Long(2), state.getPageSize());
        Object[] queryParams = state.getQueryParameters();
        assertNotNull(queryParams);
        assertEquals(1, queryParams.length);
        assertEquals("test_parent_id", queryParams[0]);
        List<String> resultColumns = state.getResultColumns();
        assertNotNull(resultColumns);
        assertEquals(1, resultColumns.size());
        assertEquals("column_1", resultColumns.get(0));
        ContentViewLayout resultLayout = state.getResultLayout();
        assertNotNull(resultLayout);
        assertEquals("document_listing", resultLayout.getName());
        assertEquals("label.document_listing.layout", resultLayout.getTitle());
        assertTrue(resultLayout.getTranslateTitle());
        assertEquals("/icons/myicon.png", resultLayout.getIconPath());
        assertTrue(resultLayout.getShowCSVExport());
        assertNull(state.getSearchDocumentModel());
        List<SortInfo> sortInfos = state.getSortInfos();
        assertNotNull(sortInfos);
        assertEquals(1, sortInfos.size());
        assertEquals("dc:title", sortInfos.get(0).getSortColumn());
        assertTrue(sortInfos.get(0).getSortAscending());
    }

    public void testRestoreContentView() throws Exception {
        assertNull(service.restoreContentView(null, session));

        // dummy state, to test errors
        ContentViewState dummyState = new ContentViewStateImpl();
        dummyState.setContentViewName("DUMMY_TEST_CONTENT_VIEW");
        try {
            service.restoreContentView(dummyState, session);
            fail("Should have raised a client exception");
        } catch (ClientException e) {
            assertEquals(
                    "Unknown content view with name 'DUMMY_TEST_CONTENT_VIEW'",
                    e.getMessage());
        }

        // build state
        ContentViewState state = new ContentViewStateImpl();
        state.setContentViewName("CURRENT_DOCUMENT_CHILDREN");
        state.setCurrentPage(new Long(0));
        state.setPageSize(new Long(2));
        Object[] queryParams = new Object[] { "test_parent_id" };
        state.setQueryParameters(queryParams);
        state.setResultColumns(Arrays.asList(new String[] { "column_1" }));
        state.setResultLayout(new ContentViewLayoutImpl("document_listing",
                "label.document_listing.layout", true, "/icons/myicon.png",
                true));
        List<SortInfo> sortInfos = new ArrayList<SortInfo>();
        sortInfos.add(new SortInfo("dc:modified", false));
        state.setSortInfos(sortInfos);

        ContentView contentView = service.restoreContentView(state, session);
        assertNotNull(contentView);

        assertEquals("CURRENT_DOCUMENT_CHILDREN", contentView.getName());
        assertEquals(new Long(2), contentView.getCurrentPageSize());
        PageProvider<?> pp = contentView.getCurrentPageProvider();
        assertNotNull(pp);
        assertNull(pp.getSearchDocumentModel());
        assertEquals(new Long(0), new Long(pp.getCurrentPageIndex()));
        queryParams = pp.getParameters();
        assertNotNull(queryParams);
        assertEquals(1, queryParams.length);
        assertEquals("test_parent_id", queryParams[0]);
        sortInfos = pp.getSortInfos();
        assertNotNull(sortInfos);
        assertEquals(1, sortInfos.size());
        assertEquals("dc:modified", sortInfos.get(0).getSortColumn());
        assertFalse(sortInfos.get(0).getSortAscending());

        ContentViewLayout resultLayout = contentView.getCurrentResultLayout();
        assertNotNull(resultLayout);
        assertEquals("document_listing", resultLayout.getName());
        assertEquals("label.document_listing.layout", resultLayout.getTitle());
        assertTrue(resultLayout.getTranslateTitle());
        assertEquals("/icons/myicon.png", resultLayout.getIconPath());
        assertTrue(resultLayout.getShowCSVExport());

        List<String> resultColumns = contentView.getCurrentResultLayoutColumns();
        assertNotNull(resultColumns);
        assertEquals(1, resultColumns.size());
        assertEquals("column_1", resultColumns.get(0));
    }

    public void testSaveJSONContentView() throws Exception {
        assertNull(service.saveContentView(null));

        ContentView contentView = service.getContentView(
                "CURRENT_DOCUMENT_CHILDREN", session);
        assertNotNull(contentView);

        // init provider, result columns and save
        contentView.getPageProviderWithParams("test_parent_id");
        contentView.setCurrentResultLayoutColumns(Arrays.asList(new String[] { "column_1" }));
        ContentViewState state = service.saveContentView(contentView);

        String json = JSONContentViewState.toJSON(state, false);
        String expectedJson = "{"
                + "\"contentViewName\":\"CURRENT_DOCUMENT_CHILDREN\","
                + "\"pageSize\":2,"
                + "\"currentPage\":0,"
                + "\"queryParameters\":[\"test_parent_id\"],"
                + "\"searchDocument\":null,"
                + "\"sortInfos\":[{\"sortColumn\":\"dc:title\",\"sortAscending\":true}],"
                + "\"resultLayout\":{\"name\":\"document_listing\",\"title\":\"label.document_listing.layout\",\"translateTitle\":true,\"iconPath\":\"/icons/myicon.png\",\"showCSVExport\":true},"
                + "\"resultColumns\":[\"column_1\"]" + "}";
        assertEquals(expectedJson, json);

        String encodedJson = JSONContentViewState.toJSON(state, true);
        assertEquals(
                "H4sIAAAAAAAAAF1Qy07DMBD8lz1bLXD0DSWVqFRK1NcFVZFxlsaSYwd7rRKi%2FjvrpFw4eTyamZ3dEbR3hI5OBq9b1SFIKI673Wp7qMu34viaQfGy3pRMgYBeXXBvflj2JECnENhaMQfyQcBXwjBUKnAMYYgg34EwUt2rLKtNA2cBEVXQbel16pgE6ZK1zPpAa%2Ffps2mcfoW3qXNcp9GSDFmEWfUcNbrGuAtICglvHBkwJksbNfjEgSO4eY%2FmPqO2JlI2CJiDJFj1gXbxX7CwcwTrgnLRKsLDbMiTBBi%2BVaWo5YBlxnHZDfld9FN4bP212J9W3z3XvLf7KzdvM51ET7B%2BhPPtFy6T5JJ%2BAQAA",
                encodedJson);
    }

    public void testRestoreJSONContentView() throws Exception {
        assertNull(service.saveContentView(null));

        String json = "{"
                + "\"contentViewName\":\"CURRENT_DOCUMENT_CHILDREN\","
                + "\"pageSize\":2,"
                + "\"currentPage\":0,"
                + "\"queryParameters\":[\"test_parent_id\"],"
                + "\"searchDocument\":null,"
                + "\"sortInfos\":[{\"sortColumn\":\"dc:title\",\"sortAscending\":true}],"
                + "\"resultLayout\":{\"name\":\"document_listing\",\"title\":\"label.document_listing.layout\",\"translateTitle\":true,\"iconPath\":\"/icons/myicon.png\",\"showCSVExport\":true},"
                + "\"resultColumns\":[\"column_1\"]" + "}";
        ContentViewState state = JSONContentViewState.fromJSON(json, false,
                session);
        checkContentViewState(state);

        String encodedJson = "H4sIAAAAAAAAAF1Qy07DMBD8lz1bLXD0DSWVqFRK1NcFVZFxlsaSYwd7rRKi%2FjvrpFw4eTyamZ3dEbR3hI5OBq9b1SFIKI673Wp7qMu34viaQfGy3pRMgYBeXXBvflj2JECnENhaMQfyQcBXwjBUKnAMYYgg34EwUt2rLKtNA2cBEVXQbel16pgE6ZK1zPpAa%2Ffps2mcfoW3qXNcp9GSDFmEWfUcNbrGuAtICglvHBkwJksbNfjEgSO4eY%2FmPqO2JlI2CJiDJFj1gXbxX7CwcwTrgnLRKsLDbMiTBBi%2BVaWo5YBlxnHZDfld9FN4bP212J9W3z3XvLf7KzdvM51ET7B%2BhPPtFy6T5JJ%2BAQAA";
        state = JSONContentViewState.fromJSON(encodedJson, true, session);
        checkContentViewState(state);
    }

    public void testSaveContentViewWithSearchDoc() throws Exception {
        ContentView contentView = service.getContentView(
                "CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT", session);
        assertNotNull(contentView);

        // test bare state
        ContentViewState state = service.saveContentView(contentView);
        assertNotNull(state);
        assertEquals("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT",
                state.getContentViewName());
        assertNull(state.getCurrentPage());
        assertNull(state.getPageSize());
        Object[] queryParams = state.getQueryParameters();
        assertNotNull(queryParams);
        assertEquals(0, queryParams.length);
        assertNull(state.getResultColumns());
        assertNull(state.getResultLayout());
        DocumentModel searchDoc = state.getSearchDocumentModel();
        assertNotNull(searchDoc);
        assertEquals("search keywords", searchDoc.getPropertyValue("dc:title"));
        assertNull(searchDoc.getPropertyValue("dc:description"));
        assertNull(state.getSortInfos());

        // init provider with search doc, result columns and test save again
        contentView.setSearchDocumentModel(searchDocument);
        contentView.getPageProvider();
        contentView.setCurrentResultLayoutColumns(Arrays.asList(new String[] { "column_1" }));
        state = service.saveContentView(contentView);
        checkContentViewStateWithSearchDoc(state);
    }

    protected void checkContentViewStateWithSearchDoc(ContentViewState state)
            throws ClientException {
        assertNotNull(state);
        assertEquals("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT",
                state.getContentViewName());
        assertEquals(new Long(0), state.getCurrentPage());
        assertEquals(new Long(2), state.getPageSize());
        Object[] queryParams = state.getQueryParameters();
        assertNotNull(queryParams);
        assertEquals(0, queryParams.length);
        List<String> resultColumns = state.getResultColumns();
        assertNotNull(resultColumns);
        assertEquals(1, resultColumns.size());
        assertEquals("column_1", resultColumns.get(0));
        assertNull(state.getResultLayout());
        DocumentModel searchDoc = state.getSearchDocumentModel();
        assertNotNull(searchDoc);
        assertEquals("search keywords", searchDoc.getPropertyValue("dc:title"));
        assertNull(searchDoc.getPropertyValue("dc:description"));

        List<SortInfo> sortInfos = state.getSortInfos();
        assertNotNull(sortInfos);
        assertEquals(1, sortInfos.size());
        assertEquals("dc:title", sortInfos.get(0).getSortColumn());
        assertTrue(sortInfos.get(0).getSortAscending());
    }

    public void testRestoreContentViewWithSearchDoc() throws Exception {
        ContentViewService service = Framework.getService(ContentViewService.class);
        assertNotNull(service);

        assertNull(service.restoreContentView(null, session));

        // build state
        ContentViewState state = new ContentViewStateImpl();
        state.setContentViewName("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT");
        state.setCurrentPage(new Long(0));
        state.setPageSize(new Long(2));
        state.setResultColumns(Arrays.asList(new String[] { "column_1" }));
        state.setResultLayout(new ContentViewLayoutImpl("document_listing",
                "label.document_listing.layout", true, "/icons/myicon.png",
                true));
        List<SortInfo> sortInfos = new ArrayList<SortInfo>();
        sortInfos.add(new SortInfo("dc:modified", false));
        state.setSortInfos(sortInfos);
        state.setSearchDocumentModel(searchDocument);

        ContentView contentView = service.restoreContentView(state, session);
        assertNotNull(contentView);

        assertEquals("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT",
                contentView.getName());
        assertEquals(new Long(2), contentView.getCurrentPageSize());
        PageProvider<?> pp = contentView.getCurrentPageProvider();
        assertNotNull(pp);
        DocumentModel searchDoc = pp.getSearchDocumentModel();
        assertNotNull(searchDoc);
        assertEquals("search keywords", searchDoc.getPropertyValue("dc:title"));
        assertNull(searchDoc.getPropertyValue("dc:description"));
        assertEquals(new Long(0), new Long(pp.getCurrentPageIndex()));
        Object[] queryParams = pp.getParameters();
        assertNotNull(queryParams);
        assertEquals(0, queryParams.length);
        sortInfos = pp.getSortInfos();
        assertNotNull(sortInfos);
        assertEquals(1, sortInfos.size());
        assertEquals("dc:modified", sortInfos.get(0).getSortColumn());
        assertFalse(sortInfos.get(0).getSortAscending());

        ContentViewLayout resultLayout = contentView.getCurrentResultLayout();
        assertNotNull(resultLayout);
        assertEquals("document_listing", resultLayout.getName());
        assertEquals("label.document_listing.layout", resultLayout.getTitle());
        assertTrue(resultLayout.getTranslateTitle());
        assertEquals("/icons/myicon.png", resultLayout.getIconPath());
        assertTrue(resultLayout.getShowCSVExport());

        List<String> resultColumns = contentView.getCurrentResultLayoutColumns();
        assertNotNull(resultColumns);
        assertEquals(1, resultColumns.size());
        assertEquals("column_1", resultColumns.get(0));
    }

    public void testSaveJSONContentViewWithSearchDoc() throws Exception {
        assertNull(service.saveContentView(null));

        ContentView contentView = service.getContentView(
                "CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT", session);
        assertNotNull(contentView);

        // init provider with search doc, result columns and save
        contentView.setSearchDocumentModel(searchDocument);
        contentView.getPageProvider();
        contentView.setCurrentResultLayoutColumns(Arrays.asList(new String[] { "column_1" }));
        ContentViewState state = service.saveContentView(contentView);

        String json = JSONContentViewState.toJSON(state, false);
        String expectedJson = "{"
                + "\"contentViewName\":\"CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT\","
                + "\"pageSize\":2,"
                + "\"currentPage\":0,"
                + "\"queryParameters\":[],"
                + "\"searchDocument\":{\"type\":\"File\",\"properties\":{\"dc:title\":\"search keywords\"}},"
                + "\"sortInfos\":[{\"sortColumn\":\"dc:title\",\"sortAscending\":true}],"
                + "\"resultLayout\":null," + "\"resultColumns\":[\"column_1\"]"
                + "}";
        assertEquals(expectedJson, json);

        String encodedJson = JSONContentViewState.toJSON(state, true);
        assertEquals(
                "H4sIAAAAAAAAAD2QTU%2FDMAyG%2FwryuYfBMbcpHWqlsU3dBgc0VVFqRkSaFCfRVKr%2BdxwqdnPej8dWJtDeRXTx1eBtp3oEAfLcNJvdqS338vySB1nV25Kl9q0%2BVe1xs25kdXehgEFd8Wh%2BuPtUgE5EzDuwBmJVwHdCGg%2BKmB2RAoj3SwEBFenP0uvUcxbEBHEc8u5nYzETyQ9I0WDIXqdFNNFmfyk%2BfOF489QFmGeGeYq1%2B%2FCZPf29pLepdxy%2FN5fUOmh0nXFXEJESznwJYUg2btXoE9%2FhkrX%2F2gLJUP6jPLaPcJl%2FAbtiVTcxAQAA",
                encodedJson);
    }

    public void testRestoreJSONContentViewWithSearchDoc() throws Exception {
        assertNull(service.saveContentView(null));
        String json = "{"
                + "\"contentViewName\":\"CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT\","
                + "\"pageSize\":2,"
                + "\"currentPage\":0,"
                + "\"queryParameters\":[],"
                + "\"searchDocument\":{\"type\":\"File\",\"properties\":{\"dc:title\":\"search keywords\"}},"
                + "\"sortInfos\":[{\"sortColumn\":\"dc:title\",\"sortAscending\":true}],"
                + "\"resultLayout\":null," + "\"resultColumns\":[\"column_1\"]"
                + "}";
        ContentViewState state = JSONContentViewState.fromJSON(json, false,
                session);
        checkContentViewStateWithSearchDoc(state);

        String encodedJson = "H4sIAAAAAAAAAD2QTU%2FDMAyG%2FwryuYfBMbcpHWqlsU3dBgc0VVFqRkSaFCfRVKr%2BdxwqdnPej8dWJtDeRXTx1eBtp3oEAfLcNJvdqS338vySB1nV25Kl9q0%2BVe1xs25kdXehgEFd8Wh%2BuPtUgE5EzDuwBmJVwHdCGg%2BKmB2RAoj3SwEBFenP0uvUcxbEBHEc8u5nYzETyQ9I0WDIXqdFNNFmfyk%2BfOF489QFmGeGeYq1%2B%2FCZPf29pLepdxy%2FN5fUOmh0nXFXEJESznwJYUg2btXoE9%2FhkrX%2F2gLJUP6jPLaPcJl%2FAbtiVTcxAQAA";

        state = JSONContentViewState.fromJSON(encodedJson, true, session);
        checkContentViewStateWithSearchDoc(state);
    }

}
