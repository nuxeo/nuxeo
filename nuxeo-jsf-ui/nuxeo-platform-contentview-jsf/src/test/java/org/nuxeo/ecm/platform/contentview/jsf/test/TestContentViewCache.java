/*
 * (C) Copyright 2010-2018 Nuxeo (http://nuxeo.com/) and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.faces.context.FacesContext;
import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewCache;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewLayout;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewLayoutImpl;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewService;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.ui.web.jsf.MockFacesContext;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author Anahide Tchertchian
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.contentview.jsf")
@Deploy("org.nuxeo.ecm.platform.contentview.jsf.test:test-contentview-contrib.xml")
public class TestContentViewCache {

    @Inject
    ContentViewService service;

    @Inject
    protected CoreSession session;

    MockFacesContext facesContext;

    DocumentModel container1;

    DocumentModel container2;

    String dummyParam = UUID.randomUUID().toString();

    Boolean booleanParam = Boolean.FALSE;

    List<String> listParam = Arrays.asList("approved", "validated");

    @Before
    public void setUp() {
        // set mock faces context for needed properties resolution
        facesContext = new MockFacesContext();
        facesContext.mapExpression("#{dummy.param}", dummyParam);
        facesContext.mapExpression("#{booleanParam}", booleanParam);
        facesContext.mapExpression("#{listParam}", listParam);
        facesContext.setCurrent();
        assertNotNull(FacesContext.getCurrentInstance());

        facesContext.mapVariable("documentManager", session);

        DocumentModel root = session.getRootDocument();
        container1 = session.createDocumentModel("Folder");
        container1.setPropertyValue("dc:title", "Container 1");
        container1.setPathInfo(root.getPathAsString(), "container_1");
        container1 = session.createDocument(container1);
        createTestDocuments(container1);

        container2 = session.createDocumentModel("Folder");
        container2.setPropertyValue("dc:title", "Container 2");
        container2.setPathInfo(root.getPathAsString(), "container_2");
        container2 = session.createDocument(container2);
        createTestDocuments(container2);
    }

    @After
    public void tearDown() {
        if (facesContext != null) {
            facesContext.relieveCurrent();
        }
    }

    protected void createTestDocuments(DocumentModel container) {
        // create docs in descending order so that docs are not ordered by
        // title by default
        for (int i = 4; i >= 0; i--) {
            DocumentModel doc = session.createDocumentModel("Folder");
            doc.setPropertyValue("dc:title", container.getTitle() + ": Document number " + i);
            doc.setPathInfo(container.getPathAsString(), "doc_" + i);
            session.createDocument(doc);
        }
        session.save();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testContentViewCache() {
        ContentViewCache cache = new ContentViewCache();

        DocumentModel currentDocument = container1;
        facesContext.mapVariable("currentDocument", currentDocument);
        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN");
        assertNotNull(contentView);

        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) contentView.getPageProvider();
        assertNotNull(pp);

        assertEquals(-1, pp.getResultsCount());
        assertEquals(0, pp.getNumberOfPages());

        // init results
        pp.getCurrentPage();

        assertEquals(0, pp.getCurrentPageIndex());
        assertEquals(5, pp.getResultsCount());
        assertTrue(pp.isNextPageAvailable());
        pp.nextPage();
        assertEquals(1, pp.getCurrentPageIndex());
        assertEquals(5, pp.getResultsCount());
        assertEquals("document_listing", contentView.getCurrentResultLayout().getName());
        ContentViewLayout layout = new ContentViewLayoutImpl("document_listing_2", null, false, null, false);
        contentView.setCurrentResultLayout(layout);

        cache.add(contentView);

        contentView = cache.get("CURRENT_DOCUMENT_CHILDREN");
        assertNotNull(contentView);
        pp = (PageProvider<DocumentModel>) contentView.getCurrentPageProvider();
        assertNotNull(pp);
        assertEquals(1, pp.getCurrentPageIndex());
        assertEquals(5, pp.getResultsCount());
        assertEquals("document_listing_2", contentView.getCurrentResultLayout().getName());

        currentDocument = container2;
        facesContext.mapVariable("currentDocument", currentDocument);
        contentView = cache.get("CURRENT_DOCUMENT_CHILDREN");
        assertNull(contentView);

        currentDocument = container1;
        facesContext.mapVariable("currentDocument", currentDocument);
        contentView = cache.get("CURRENT_DOCUMENT_CHILDREN");
        assertNotNull(contentView);
        pp = (PageProvider<DocumentModel>) contentView.getCurrentPageProvider();
        assertNotNull(pp);
        assertEquals(1, pp.getCurrentPageIndex());
        assertEquals(5, pp.getResultsCount());
        assertEquals("document_listing_2", contentView.getCurrentResultLayout().getName());

        cache.refreshOnEvent("documentChildrenChanged");
        contentView = cache.get("CURRENT_DOCUMENT_CHILDREN");
        assertNotNull(contentView);
        pp = (PageProvider<DocumentModel>) contentView.getCurrentPageProvider();
        assertNotNull(pp);
        assertEquals(1, pp.getCurrentPageIndex());
        assertEquals(-1, pp.getResultsCount());
        // init results
        pp.getCurrentPage();
        assertEquals(5, pp.getResultsCount());
    }

    /**
     * Non-regression test for NXP-13604: check that a page provider triggering a call to
     * {@link PageProvider#getCurrentPage()} on refresh is not refreshed more than once.
     *
     * @since 5.9.2
     */
    @Test
    public void testContentViewCacheRefreshAndRewind() {
        ContentViewCache cache = new ContentViewCache();
        ContentView cv = service.getContentView("MOCK_DAM_CV");
        assertNotNull(cv);

        MockDAMPageProvider pp = (MockDAMPageProvider) cv.getPageProvider();
        assertNotNull(pp);

        // one refresh already, when retrieving page size on page provider,
        // refresh is triggered.
        assertEquals(1, pp.getGetCounter());
        // init results
        pp.getCurrentPage();
        assertEquals(1, pp.getGetCounter());

        cache.add(cv);

        cv.getCurrentPageProvider().getCurrentPage();
        assertEquals(1, ((MockDAMPageProvider) cv.getCurrentPageProvider()).getGetCounter());

        cache.refreshAndRewindAll();

        cv.getCurrentPageProvider().getCurrentPage();
        assertEquals(2, ((MockDAMPageProvider) cv.getCurrentPageProvider()).getGetCounter());

    }
}
