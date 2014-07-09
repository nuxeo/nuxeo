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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
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
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * @since 5.4.2
 */
public class TestContentViewState extends SQLRepositoryTestCase {

    ContentViewService service;

    MockFacesContext facesContext;

    DocumentModel currentDocument;

    DocumentModel searchDocument;

    @Override
    @Before
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
        searchDocument.setPropertyValue("dc:modified", getModifiedDate());

        currentDocument = session.getRootDocument();

        // set mock faces context for needed properties resolution
        facesContext = new MockFacesContext();
        facesContext.mapVariable("documentManager", session);
        facesContext.mapVariable("searchDocument", searchDocument);
        facesContext.mapVariable("currentDocument", currentDocument);
        facesContext.setCurrent();
        assertNotNull(FacesContext.getCurrentInstance());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        closeSession();
        if (facesContext != null) {
            facesContext.relieveCurrent();
        }
        super.tearDown();
    }

    protected Calendar getModifiedDate() {
        Calendar modified = Calendar.getInstance();
        modified.setTimeInMillis(1397662663000L);
        return modified;
    }

    @Test
    public void testSaveContentView() throws Exception {
        assertNull(service.saveContentView(null));

        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN");
        assertNotNull(contentView);

        // test bare state
        ContentViewState state = service.saveContentView(contentView);
        assertNotNull(state);
        assertEquals("CURRENT_DOCUMENT_CHILDREN", state.getContentViewName());
        assertNull(state.getCurrentPage());
        assertNull(state.getPageSize());

        Object[] queryParams = state.getQueryParameters();
        assertNotNull(queryParams);
        assertEquals(2, queryParams.length);
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

    @Test
    public void testRestoreContentView() throws Exception {
        assertNull(service.restoreContentView(null));

        // dummy state, to test errors
        ContentViewState dummyState = new ContentViewStateImpl();
        dummyState.setContentViewName("DUMMY_TEST_CONTENT_VIEW");
        try {
            service.restoreContentView(dummyState);
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

        ContentView contentView = service.restoreContentView(state);
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

    @Test
    public void testSaveJSONContentView() throws Exception {
        assertNull(service.saveContentView(null));

        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN");
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
        JSONAssert.assertEquals(expectedJson, json, true);

        String encodedJson = JSONContentViewState.toJSON(state, true);
        assertEquals(
                "H4sIAAAAAAAAAF1Qy07DMBD8lz1bLXD0DSWVqFRK1NcFVZFxlsaSYwd7rRKi%2FjvrpFw4eTyamZ3dEbR3hI5OBq9b1SFIKI673Wp7qMu34viaQfGy3pRMgYBeXXBvflj2JECnENhaMQfyQcBXwjBUKnAMYYgg34EwUt2rLKtNA2cBEVXQbel16pgE6ZK1zPpAa%2Ffps2mcfoW3qXNcp9GSDFmEWfUcNbrGuAtICglvHBkwJksbNfjEgSO4eY%2FmPqO2JlI2CJiDJFj1gXbxX7CwcwTrgnLRKsLDbMiTBBi%2BVaWo5YBlxnHZDfld9FN4bP212J9W3z3XvLf7KzdvM51ET7B%2BhPPtFy6T5JJ%2BAQAA",
                encodedJson);
    }

    @Test
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
        ContentViewState state = JSONContentViewState.fromJSON(json, false);
        checkContentViewState(state);

        String encodedJson = "H4sIAAAAAAAAAF1Qy07DMBD8lz1bLXD0DSWVqFRK1NcFVZFxlsaSYwd7rRKi%2FjvrpFw4eTyamZ3dEbR3hI5OBq9b1SFIKI673Wp7qMu34viaQfGy3pRMgYBeXXBvflj2JECnENhaMQfyQcBXwjBUKnAMYYgg34EwUt2rLKtNA2cBEVXQbel16pgE6ZK1zPpAa%2Ffps2mcfoW3qXNcp9GSDFmEWfUcNbrGuAtICglvHBkwJksbNfjEgSO4eY%2FmPqO2JlI2CJiDJFj1gXbxX7CwcwTrgnLRKsLDbMiTBBi%2BVaWo5YBlxnHZDfld9FN4bP212J9W3z3XvLf7KzdvM51ET7B%2BhPPtFy6T5JJ%2BAQAA";
        state = JSONContentViewState.fromJSON(encodedJson, true);
        checkContentViewState(state);
    }

    @Test
    public void testSaveContentViewWithSearchDoc() throws Exception {
        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT");
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
        assertEquals(getModifiedDate(),
                searchDoc.getPropertyValue("dc:modified"));
        assertNull(searchDoc.getPropertyValue("dc:description"));
        assertNull(state.getSortInfos());

        // init provider with search doc, result columns and test save again
        contentView.setSearchDocumentModel(searchDocument);
        contentView.getPageProvider();
        contentView.setCurrentResultLayoutColumns(Arrays.asList(new String[] { "column_1" }));
        state = service.saveContentView(contentView);
        checkContentViewStateWithSearchDoc(state, true, true);
    }

    protected void checkContentViewStateWithSearchDoc(ContentViewState state,
            boolean withQueryParams, boolean withSortInfos)
            throws ClientException {
        assertNotNull(state);
        assertEquals("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT",
                state.getContentViewName());
        assertEquals(new Long(0), state.getCurrentPage());
        assertEquals(new Long(2), state.getPageSize());

        Object[] queryParams = state.getQueryParameters();
        if (withQueryParams) {
            assertNotNull(queryParams);
            assertEquals(0, queryParams.length);
        } else {
            assertNull(queryParams);
        }

        List<String> resultColumns = state.getResultColumns();
        assertNotNull(resultColumns);
        assertEquals(1, resultColumns.size());
        assertEquals("column_1", resultColumns.get(0));
        assertNull(state.getResultLayout());

        DocumentModel searchDoc = state.getSearchDocumentModel();
        assertNotNull(searchDoc);
        assertEquals("search keywords", searchDoc.getPropertyValue("dc:title"));
        assertEquals(getModifiedDate(),
                searchDoc.getPropertyValue("dc:modified"));
        assertNull(searchDoc.getPropertyValue("dc:description"));

        List<SortInfo> sortInfos = state.getSortInfos();
        if (withSortInfos) {
            assertNotNull(sortInfos);
            assertEquals(1, sortInfos.size());
            assertEquals("dc:title", sortInfos.get(0).getSortColumn());
            assertTrue(sortInfos.get(0).getSortAscending());
        } else {
            assertNull(sortInfos);
        }
    }

    @Test
    public void testRestoreContentViewWithSearchDoc() throws Exception {
        ContentViewService service = Framework.getService(ContentViewService.class);
        assertNotNull(service);

        assertNull(service.restoreContentView(null));

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

        ContentView contentView = service.restoreContentView(state);
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

    /**
     * Non regression test for NXP-11419, showing an issue when restoring with
     * a search doc and a current page > 0
     */
    @Test
    public void testRestoreContentViewWithSearchDocAndCurrentPage()
            throws Exception {
        ContentViewService service = Framework.getService(ContentViewService.class);
        assertNotNull(service);

        assertNull(service.restoreContentView(null));

        // build state
        ContentViewState state = new ContentViewStateImpl();
        state.setContentViewName("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT");
        // set current page to the first one
        state.setCurrentPage(new Long(1));
        state.setPageSize(new Long(2));
        state.setResultColumns(Arrays.asList(new String[] { "column_1" }));
        state.setResultLayout(new ContentViewLayoutImpl("document_listing",
                "label.document_listing.layout", true, "/icons/myicon.png",
                true));
        List<SortInfo> sortInfos = new ArrayList<SortInfo>();
        sortInfos.add(new SortInfo("dc:modified", false));
        state.setSortInfos(sortInfos);
        state.setSearchDocumentModel(searchDocument);

        ContentView contentView = service.restoreContentView(state);
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
        assertEquals(new Long(1), new Long(pp.getCurrentPageIndex()));
    }

    @Test
    public void testSaveJSONContentViewWithSearchDoc() throws Exception {
        assertNull(service.saveContentView(null));

        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT");
        assertNotNull(contentView);

        // set some blob properties on the searchDocument ( has the schema
        // files)
        // to check that there are ignored during serialization
        Map<String, Serializable> file = new HashMap<String, Serializable>();
        ArrayList<Map<String, Serializable>> files = new ArrayList<Map<String, Serializable>>();
        // Attach one file to the list
        File tmpFile = File.createTempFile("test", ".txt");
        tmpFile.deleteOnExit();
        FileUtils.writeFile(tmpFile, "Content");
        FileBlob blob = new FileBlob(tmpFile);
        file.put("file", blob);
        file.put("filename", "initial_name.txt");
        files.add(file);
        searchDocument.setPropertyValue("files:files", files);

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
                + "\"searchDocument\":{\"type\":\"File\",\"properties\":{\"dc:modified\":\"2014-04-16T15:37:43+0000\",\"dc:title\":\"search keywords\",\"files\":[]}},"
                + "\"sortInfos\":[{\"sortColumn\":\"dc:title\",\"sortAscending\":true}],"
                + "\"resultLayout\":null," + "\"resultColumns\":[\"column_1\"]"
                + "}";
        JSONAssert.assertEquals(expectedJson, json, true);

        String encodedJson = JSONContentViewState.toJSON(state, true);
        assertEquals(
                "H4sIAAAAAAAAAD2Q3WqDQBCFX6XMbQ1oYlvYu6ApCmkajGkvShDRMV2qu3Z%2FCFZ8985Gkr2aPXPmO8OMUElhUJgPjpdd2SEwiI5ZttnlRfweHd9cESXpNiap%2BEzzpDhs1lmU3LvgQV%2Be8cD%2FaHbpQWWVIt6eNGC%2BB78W1bAvFbENKg3s6%2BSBxlJV37GsbEdeYCOYoXfZr7xFR1SyR2U4aterK9bJmjcca7Is%2FSBc%2BOEieM6DJ7Z6YeHq0adHY2Q03LQONCc8%2FOBwkarW1GwIfY2fJlpAKpOKRjphvP4i2dpO0OQdMrvWukJRc3EGZpTFibZXqG1rtuUgLe0ubNvetBnioHRXVxYBnKZ%2F2JEe%2BWUBAAA%3D",
                encodedJson);
    }

    @Test
    public void testSaveJSONContentViewWithPathParam() throws Exception {
        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_WITH_PATH_PARAM");
        assertNotNull(contentView);
        ContentViewState state = service.saveContentView(contentView);

        String json = JSONContentViewState.toJSON(state, false);

        String expectedJson = "{"
                + "\"contentViewName\":\"CURRENT_DOCUMENT_WITH_PATH_PARAM\","
                + "\"queryParameters\":[\"/\"]," + "\"searchDocument\":null,"
                + "\"sortInfos\":[]," + "\"resultLayout\":null" + "}";
        JSONAssert.assertEquals(expectedJson, json, true);

        String encodedJson = JSONContentViewState.toJSON(state, true);
        assertEquals(
                "H4sIAAAAAAAAAKtWSs7PK0nNKwnLTC33S8xNVbJScg4NCnL1C4l38XcO9QUxwj1DPOIDHMFEkKOvko5SYWlqUWVAYhFQQ0lqUbGSVbSSvlKsjlJxamJRcoZLfnJpLtBMJau80pwcoGh%2BUYlnXlo%2BSB1QUVFqcWlOiU9iZX4pVEktAMoXe7GHAAAA",
                encodedJson);
    }

    @Test
    public void testRestoreJSONContentViewWithSearchDoc() throws Exception {
        assertNull(service.saveContentView(null));
        String json = "{"
                + "\"contentViewName\":\"CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT\","
                + "\"pageSize\":2,"
                + "\"currentPage\":0,"
                + "\"queryParameters\":[],"
                + "\"searchDocument\":{\"type\":\"File\",\"properties\":{\"dc:modified\":\"2014-04-16T17:37:43+0200\",\"dc:title\":\"search keywords\"}},"
                + "\"sortInfos\":[{\"sortColumn\":\"dc:title\",\"sortAscending\":true}],"
                + "\"resultLayout\":null," + "\"resultColumns\":[\"column_1\"]"
                + "}";
        ContentViewState state = JSONContentViewState.fromJSON(json, false);
        checkContentViewStateWithSearchDoc(state, false, true);

        String encodedJson = "H4sIAAAAAAAAAD2QUWuDMBSF%2F8q4r7OgrayQt6IdCl1XrN0eRhGJ1y5MExcTihP%2F%2B24q7dvNued855IRuJIGpfkQeN2XLQKD6JRl231exO%2FR6c0NUZLuYpKKzzRPiuN2k0XJYwsedOUFj%2BKPsksPuNWaeAfSgPke%2FFrUw6HUxDaoe2BfZw96LDX%2FjhW3LXmBjWCGznW%2FigYdUasOtRHYu13FWasqUQusyLL0g3Dhh4vgJQ%2FWbLVm4erZX%2Fo%2BxchohGkcaG54%2BsHhqnTV07Im9K1%2BmugApU0qa%2BWE8faKVGNbSckHZHZteo6yEvICzGiLE12vsbeN2ZWDsnS7tE1z12aIg9K%2FurEI4Dz9A2Tpu0RlAQAA";

        state = JSONContentViewState.fromJSON(encodedJson, true);
        checkContentViewStateWithSearchDoc(state, false, true);
    }

}