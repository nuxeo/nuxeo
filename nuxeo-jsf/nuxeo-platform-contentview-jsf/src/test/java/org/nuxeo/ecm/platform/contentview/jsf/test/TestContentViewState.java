/*
 * (C) Copyright 2011-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.contentview.jsf.test;

import static java.nio.charset.StandardCharsets.UTF_8;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
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
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * @since 5.4.2
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.contentview.jsf")
@Deploy("org.nuxeo.ecm.platform.contentview.jsf.test:test-contentview-contrib.xml")
public class TestContentViewState {

    @Inject
    ContentViewService service;

    @Inject
    protected CoreSession session;

    MockFacesContext facesContext;

    DocumentModel currentDocument;

    DocumentModel searchDocument;

    Boolean booleanParam = Boolean.FALSE;

    List<String> listParam = Arrays.asList("approved", "validated");

    String ENC_CURRENT_DOC_CHILDREN = "H4sIAAAAAAAAAI1Qy27CMBD8lz1b0PboWxWQikRpxOuCULR1FmLJsVM%2FeBTx710n9NJTTx6vZmdm5wbK2Ug2bjWdF9gSSCg2y%2BV0sa4mH8XmPYPibTaf8AgEdHik0ruTrsn%2Fk77S30x7EaCS9%2BxU8gzkk4CvRP5aomeZSD6A3EGkEKsOM63SNYgDmkDCJmPEDrDr2Jl4DCc0usbIeL8XEAi9aiZOpZYXQfZ8CM7HmT24LHzrf4UzqbUcuVYy6mgIBtZrUGRrbY8go090Z0lPIZk4x6tLLHgDO9xaPzwqo0PMCwIGIQkGP8mM%2FhJGZpBgnkcbDGdeDwvZSYDm%2BkuMDQuMMw7j9prfUdeLh8adi9V2euk45iPdb7jhmr421cPqGTg5XUil3Izsy7v%2FAB8GOT7iAQAA";

    String ENC_CURRENT_DOC_CHILDREN_WITH_SEARCH_DOC = "H4sIAAAAAAAAAKWR3U4CMRCFX8XMrUuyC6hJ78iCgQSR8KMXxmw23Vls7LY4bcWV7Ls7BeUF7NX09JxvTtIjSGs8Gv%2Bk8LAoGwQB%2BXa1miw2xfgx3z7EIZ%2FO5mOWiufZZlqsJ6NVPr28QgL7codLsp%2BqQvoPY62%2BOdtPQAYi7rRkDUSawEdAapclMdsjORAvrwk4LEm%2Bja0MDXtBHMG3%2B7j7XmmMRLJ7JK%2FQxbeaxd9gJUVjK1UrrNjdT7NhLx32sttNdiMGd2I4uE75wMnoldeReV529Y7twVLloOu4gCU%2FM7WN2OPpllsdGsP2S%2FLsGjmJplJmB8JTwI5LELqg%2FbxsbeDuJmj9p50hEcp%2FE8ciAw7gF8rgY%2BW61A67H%2B4OaOu6AQAA";

    @Before
    public void setUp() {
        searchDocument = session.createDocumentModel("File");
        searchDocument.setPropertyValue("dc:title", "search keywords");
        searchDocument.setPropertyValue("dc:modified", getModifiedDate());

        currentDocument = session.getRootDocument();

        // set mock faces context for needed properties resolution
        facesContext = new MockFacesContext();
        facesContext.mapVariable("documentManager", session);
        facesContext.mapVariable("searchDocument", searchDocument);
        facesContext.mapVariable("currentDocument", currentDocument);
        facesContext.mapVariable("booleanParam", booleanParam);
        facesContext.mapVariable("listParam", listParam);
        facesContext.setCurrent();
        assertNotNull(FacesContext.getCurrentInstance());
    }

    @After
    public void tearDown() {
        if (facesContext != null) {
            facesContext.relieveCurrent();
        }
    }

    protected Calendar getModifiedDate() {
        Calendar modified = Calendar.getInstance();
        modified.setTimeInMillis(1397662663000L);
        return modified;
    }

    @Test
    public void testSaveContentView() {
        assertNull(service.saveContentView(null));

        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN");
        assertNotNull(contentView);

        // test bare state
        ContentViewState state = service.saveContentView(contentView);
        assertNotNull(state);
        assertEquals("CURRENT_DOCUMENT_CHILDREN", state.getContentViewName());
        assertNull(state.getPageProviderName());
        assertNull(state.getCurrentPage());
        assertNull(state.getPageSize());

        Object[] queryParams = state.getQueryParameters();
        assertNotNull(queryParams);
        assertEquals(4, queryParams.length);
        assertEquals(currentDocument.getId(), queryParams[0]);
        assertEquals(booleanParam, queryParams[1]);
        assertNull(queryParams[2]);
        assertEquals(listParam, queryParams[3]);
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
        contentView.getPageProviderWithParams("test_parent_id", booleanParam, null, listParam);
        contentView.setCurrentResultLayoutColumns(Collections.singletonList("column_1"));
        state = service.saveContentView(contentView);
        checkContentViewState(state, true);
    }

    protected void checkContentViewState(ContentViewState state, boolean withPP) {
        assertNotNull(state);
        assertEquals("CURRENT_DOCUMENT_CHILDREN", state.getContentViewName());
        if (withPP) {
            assertEquals("CURRENT_DOCUMENT_CHILDREN", state.getPageProviderName());
        } else {
            assertNull(state.getPageProviderName());
        }
        assertEquals(Long.valueOf(0), state.getCurrentPage());
        assertEquals(Long.valueOf(2), state.getPageSize());

        Object[] queryParams = state.getQueryParameters();
        assertNotNull(queryParams);
        assertEquals(4, queryParams.length);
        assertEquals("test_parent_id", queryParams[0]);
        assertEquals(booleanParam, queryParams[1]);
        assertNull(queryParams[2]);
        assertEquals(listParam, queryParams[3]);

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
    public void testRestoreContentView() {
        assertNull(service.restoreContentView(null));

        // dummy state, to test errors
        ContentViewState dummyState = new ContentViewStateImpl();
        dummyState.setContentViewName("DUMMY_TEST_CONTENT_VIEW");
        try {
            service.restoreContentView(dummyState);
            fail("Should have raised a client exception");
        } catch (NuxeoException e) {
            assertEquals("Unknown content view with name 'DUMMY_TEST_CONTENT_VIEW'", e.getMessage());
        }

        // build state
        ContentViewState state = new ContentViewStateImpl();
        state.setContentViewName("CURRENT_DOCUMENT_CHILDREN");
        state.setPageProviderName("CURRENT_DOCUMENT_CHILDREN");
        state.setCurrentPage(Long.valueOf(0));
        state.setPageSize(Long.valueOf(2));
        Object[] queryParams = new Object[] { "test_parent_id", booleanParam, null, listParam };
        state.setQueryParameters(queryParams);
        state.setResultColumns(Collections.singletonList("column_1"));
        state.setResultLayout(new ContentViewLayoutImpl("document_listing", "label.document_listing.layout", true,
                "/icons/myicon.png", true));
        List<SortInfo> sortInfos = new ArrayList<>();
        sortInfos.add(new SortInfo("dc:modified", false));
        state.setSortInfos(sortInfos);

        ContentView contentView = service.restoreContentView(state);
        assertNotNull(contentView);
        assertEquals("CURRENT_DOCUMENT_CHILDREN", contentView.getName());
        assertEquals(Long.valueOf(2), contentView.getCurrentPageSize());

        PageProvider<?> pp = contentView.getCurrentPageProvider();
        assertNotNull(pp);
        assertNull(pp.getSearchDocumentModel());
        assertEquals(Long.valueOf(0), Long.valueOf(pp.getCurrentPageIndex()));

        queryParams = pp.getParameters();
        assertNotNull(queryParams);
        assertEquals(4, queryParams.length);
        assertEquals("test_parent_id", queryParams[0]);
        assertEquals(booleanParam, queryParams[1]);
        assertNull(queryParams[2]);
        assertEquals(listParam, queryParams[3]);

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

        assertTrue(contentView.isExecuted());
    }

    @Test
    public void testSaveJSONContentView() throws Exception {
        assertNull(service.saveContentView(null));

        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN");
        assertNotNull(contentView);

        // init provider, result columns and save
        contentView.getPageProviderWithParams("test_parent_id", booleanParam, null, listParam);
        contentView.setCurrentResultLayoutColumns(Collections.singletonList("column_1"));
        ContentViewState state = service.saveContentView(contentView);

        String json = JSONContentViewState.toJSON(state, false);
        String expectedJson = "{" //
                + "\"contentViewName\":\"CURRENT_DOCUMENT_CHILDREN\"," //
                + "\"pageProviderName\":\"CURRENT_DOCUMENT_CHILDREN\"," //
                + "\"pageSize\":2," //
                + "\"currentPage\":0," //
                + "\"queryParameters\":[\"test_parent_id\",false,null,[\"approved\", \"validated\"]]," //
                + "\"searchDocument\":null," + "\"sortInfos\":[{\"sortColumn\":\"dc:title\",\"sortAscending\":true}],"
                + "\"resultLayout\":{\"name\":\"document_listing\",\"title\":\"label.document_listing.layout\",\"translateTitle\":true,\"iconPath\":\"/icons/myicon.png\",\"showCSVExport\":true},"
                + "\"resultColumns\":[\"column_1\"]," + "\"executed\":false" + "}";
        JSONAssert.assertEquals(expectedJson, json, true);

        String encodedJson = JSONContentViewState.toJSON(state, true);
        assertEquals(ENC_CURRENT_DOC_CHILDREN, encodedJson);
    }

    @Test
    public void testRestoreJSONContentViewWithNullArray() throws Exception {
        assertNull(service.saveContentView(null));

        String json = "{" //
                + "\"contentViewName\":\"SEARCH_DOCUMENT\"," //
                + "\"pageSize\":2," //
                + "\"currentPage\":0," //
                + "\"queryParameters\":[]," //
                + "\"searchDocument\":{\"type\":\"File\",\n" //
                + "    \"properties\":{\"dc:contributors\":[null]}" //
                + "}," //
                + "\"sortInfos\":[]," //
                + "\"resultLayout\":{\"name\":\"document_listing\",\"title\":\"label.document_listing.layout\",\"translateTitle\":true,\"iconPath\":\"/icons/myicon.png\",\"showCSVExport\":true},"
                + "\"resultColumns\":[\"column_1\"]" //
                + "}";
        ContentViewState state = JSONContentViewState.fromJSON(json, false);
        assertNotNull(state);
        assertEquals("SEARCH_DOCUMENT", state.getContentViewName());
        DocumentModel searchDoc = state.getSearchDocumentModel();
        assertNotNull(searchDoc);
        String[] searchProp = (String[]) searchDoc.getPropertyValue("dc:contributors");
        assertNull("Null object should be ignored, so array is empty, therefore null property", searchProp);
    }

    @Test
    public void testRestoreJSONContentView() throws Exception {
        assertNull(service.saveContentView(null));

        String json = "{" + "\"contentViewName\":\"CURRENT_DOCUMENT_CHILDREN\"," + "\"pageSize\":2,"
                + "\"currentPage\":0,"
                + "\"queryParameters\":[\"test_parent_id\",false,null,[\"approved\", \"validated\"]],"
                + "\"searchDocument\":null," + "\"sortInfos\":[{\"sortColumn\":\"dc:title\",\"sortAscending\":true}],"
                + "\"resultLayout\":{\"name\":\"document_listing\",\"title\":\"label.document_listing.layout\",\"translateTitle\":true,\"iconPath\":\"/icons/myicon.png\",\"showCSVExport\":true},"
                + "\"resultColumns\":[\"column_1\"]" + "}";
        ContentViewState state = JSONContentViewState.fromJSON(json, false);
        checkContentViewState(state, false);

        state = JSONContentViewState.fromJSON(ENC_CURRENT_DOC_CHILDREN, true);
        checkContentViewState(state, true);
    }

    @Test
    public void testSaveContentViewWithSearchDoc() {
        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT");
        assertNotNull(contentView);

        // test bare state
        ContentViewState state = service.saveContentView(contentView);
        assertNotNull(state);
        assertEquals("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT", state.getContentViewName());
        assertNull(state.getPageProviderName());
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
        assertEquals(getModifiedDate(), searchDoc.getPropertyValue("dc:modified"));
        assertNull(searchDoc.getPropertyValue("dc:description"));
        assertNull(state.getSortInfos());

        // init provider with search doc, result columns and test save again
        contentView.setSearchDocumentModel(searchDocument);
        contentView.getPageProvider();
        contentView.setCurrentResultLayoutColumns(Collections.singletonList("column_1"));
        state = service.saveContentView(contentView);
        checkContentViewStateWithSearchDoc(state, true);
    }

    protected void checkContentViewStateWithSearchDoc(ContentViewState state, boolean withQueryParams) {
        assertNotNull(state);
        assertEquals("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT", state.getContentViewName());
        assertEquals("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT", state.getPageProviderName());
        assertEquals(Long.valueOf(0), state.getCurrentPage());
        assertEquals(Long.valueOf(2), state.getPageSize());

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
        assertEquals(getModifiedDate(), searchDoc.getPropertyValue("dc:modified"));
        assertNull(searchDoc.getPropertyValue("dc:description"));

        List<SortInfo> sortInfos = state.getSortInfos();
        assertNotNull(sortInfos);
        assertEquals(1, sortInfos.size());
        assertEquals("dc:title", sortInfos.get(0).getSortColumn());
        assertTrue(sortInfos.get(0).getSortAscending());
    }

    @Test
    public void testRestoreContentViewWithSearchDoc() {
        ContentViewService service = Framework.getService(ContentViewService.class);
        assertNotNull(service);

        assertNull(service.restoreContentView(null));

        // build state
        ContentViewState state = new ContentViewStateImpl();
        state.setContentViewName("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT");
        state.setCurrentPage(Long.valueOf(0));
        state.setPageSize(Long.valueOf(2));
        state.setResultColumns(Collections.singletonList("column_1"));
        state.setResultLayout(new ContentViewLayoutImpl("document_listing", "label.document_listing.layout", true,
                "/icons/myicon.png", true));
        List<SortInfo> sortInfos = new ArrayList<>();
        sortInfos.add(new SortInfo("dc:modified", false));
        state.setSortInfos(sortInfos);
        state.setSearchDocumentModel(searchDocument);

        ContentView contentView = service.restoreContentView(state);
        assertNotNull(contentView);

        assertEquals("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT", contentView.getName());
        assertEquals(Long.valueOf(2), contentView.getCurrentPageSize());

        PageProvider<?> pp = contentView.getCurrentPageProvider();
        assertNotNull(pp);

        DocumentModel searchDoc = pp.getSearchDocumentModel();
        assertNotNull(searchDoc);
        assertEquals("search keywords", searchDoc.getPropertyValue("dc:title"));
        assertNull(searchDoc.getPropertyValue("dc:description"));
        assertEquals(Long.valueOf(0), Long.valueOf(pp.getCurrentPageIndex()));

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
     * Non regression test for NXP-11419, showing an issue when restoring with a search doc and a current page > 0
     */
    @Test
    public void testRestoreContentViewWithSearchDocAndCurrentPage() {
        ContentViewService service = Framework.getService(ContentViewService.class);
        assertNotNull(service);

        assertNull(service.restoreContentView(null));

        // build state
        ContentViewState state = new ContentViewStateImpl();
        state.setContentViewName("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT");
        // set current page to the first one
        state.setCurrentPage(Long.valueOf(1));
        state.setPageSize(Long.valueOf(2));
        state.setResultColumns(Collections.singletonList("column_1"));
        state.setResultLayout(new ContentViewLayoutImpl("document_listing", "label.document_listing.layout", true,
                "/icons/myicon.png", true));
        List<SortInfo> sortInfos = new ArrayList<>();
        sortInfos.add(new SortInfo("dc:modified", false));
        state.setSortInfos(sortInfos);
        state.setSearchDocumentModel(searchDocument);

        ContentView contentView = service.restoreContentView(state);
        assertNotNull(contentView);

        assertEquals("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT", contentView.getName());
        assertEquals(Long.valueOf(2), contentView.getCurrentPageSize());

        PageProvider<?> pp = contentView.getCurrentPageProvider();
        assertNotNull(pp);

        DocumentModel searchDoc = pp.getSearchDocumentModel();
        assertNotNull(searchDoc);
        assertEquals("search keywords", searchDoc.getPropertyValue("dc:title"));
        assertNull(searchDoc.getPropertyValue("dc:description"));
        assertEquals(Long.valueOf(1), Long.valueOf(pp.getCurrentPageIndex()));
    }

    @Test
    public void testSaveJSONContentViewWithSearchDoc() throws Exception {
        assertNull(service.saveContentView(null));

        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT");
        assertNotNull(contentView);

        // set some blob properties on the searchDocument ( has the schema
        // files)
        // to check that there are ignored during serialization
        Map<String, Serializable> file = new HashMap<>();
        ArrayList<Map<String, Serializable>> files = new ArrayList<>();
        // Attach one file to the list
        File tmpFile = Framework.createTempFile("test", ".txt");
        Framework.trackFile(tmpFile, this);
        FileUtils.writeStringToFile(tmpFile, "Content", UTF_8);
        Blob blob = Blobs.createBlob(tmpFile);
        file.put("file", (Serializable) blob);
        files.add(file);
        searchDocument.setPropertyValue("files:files", files);

        // init provider with search doc, result columns and save
        contentView.setSearchDocumentModel(searchDocument);
        contentView.getPageProvider();
        contentView.setCurrentResultLayoutColumns(Collections.singletonList("column_1"));
        ContentViewState state = service.saveContentView(contentView);

        String json = JSONContentViewState.toJSON(state, false);
        String expectedJson = "{" + "\"contentViewName\":\"CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT\","
                + "\"pageProviderName\":\"CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT\"," + "\"pageSize\":2,"
                + "\"currentPage\":0," + "\"queryParameters\":[],"
                + "\"searchDocument\":{\"type\":\"File\",\"properties\":{\"dc:modified\":\"2014-04-16T15:37:43+0000\",\"dc:title\":\"search keywords\",\"files\":[]}},"
                + "\"sortInfos\":[{\"sortColumn\":\"dc:title\",\"sortAscending\":true}]," + "\"resultLayout\":null,"
                + "\"resultColumns\":[\"column_1\"]," + "\"executed\":false" + "}";
        JSONAssert.assertEquals(expectedJson, json, true);

        String encodedJson = JSONContentViewState.toJSON(state, true);
        assertEquals(ENC_CURRENT_DOC_CHILDREN_WITH_SEARCH_DOC, encodedJson);
    }

    @Test
    public void testRestoreJSONContentViewWithSearchDoc() throws Exception {
        assertNull(service.saveContentView(null));
        String json = "{" + "\"contentViewName\":\"CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT\","
                + "\"pageProviderName\":\"CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT\"," + "\"pageSize\":2,"
                + "\"currentPage\":0," + "\"queryParameters\":[],"
                + "\"searchDocument\":{\"type\":\"File\",\"properties\":{\"dc:modified\":\"2014-04-16T17:37:43+0200\",\"dc:title\":\"search keywords\"}},"
                + "\"sortInfos\":[{\"sortColumn\":\"dc:title\",\"sortAscending\":true}]," + "\"resultLayout\":null,"
                + "\"resultColumns\":[\"column_1\"]" + "}";
        ContentViewState state = JSONContentViewState.fromJSON(json, false);
        checkContentViewStateWithSearchDoc(state, false);

        state = JSONContentViewState.fromJSON(ENC_CURRENT_DOC_CHILDREN_WITH_SEARCH_DOC, true);
        checkContentViewStateWithSearchDoc(state, false);
    }

    @Test
    public void testSaveJSONContentViewWithPathParam() throws Exception {
        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_WITH_PATH_PARAM");
        assertNotNull(contentView);
        ContentViewState state = service.saveContentView(contentView);

        String json = JSONContentViewState.toJSON(state, false);

        String expectedJson = "{" + "\"contentViewName\":\"CURRENT_DOCUMENT_WITH_PATH_PARAM\","
                + "\"queryParameters\":[\"/\"]," + "\"searchDocument\":null," + "\"sortInfos\":[],"
                + "\"resultLayout\":null," + "\"executed\":false" + "}";
        JSONAssert.assertEquals(expectedJson, json, true);

        String encodedJson = JSONContentViewState.toJSON(state, true);
        assertEquals(
                "H4sIAAAAAAAAAD2MwQrCMBBE%2F2XPBe%2B5lVawYGsJrR6klBC3KKQJbnbRIv57Uw9ehsfwZj5gg2f0fH7gqzEzgoKi13rfdGN5Kvp6g0vVHcY2%2F4XOa8jgKUhLaygNGCmCusIOhgwiGrL3MliZ0ycoL86lNhBXfgqblyTCKI6PZgnyV%2FCNVhhvoCbjIn5X%2FrApwpgAAAA%3D",
                encodedJson);
    }

}
