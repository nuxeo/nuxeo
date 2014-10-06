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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import javax.faces.context.FacesContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewCache;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewLayout;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewLayoutImpl;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewService;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.ui.web.jsf.MockFacesContext;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Anahide Tchertchian
 */
public class TestContentViewCache extends SQLRepositoryTestCase {

    ContentViewService service;

    MockFacesContext facesContext;

    DocumentModel currentDocument;

    DocumentModel container1;

    DocumentModel container2;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        deployContrib("org.nuxeo.ecm.platform.query.api",
                "OSGI-INF/pageprovider-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.contentview.jsf",
                "OSGI-INF/contentview-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.contentview.jsf.test",
                "test-contentview-contrib.xml");

        // set mock faces context for needed properties resolution
        facesContext = new MockFacesContext();
        facesContext.mapExpression("#{dummy.param}",
                UUID.randomUUID().toString());
        facesContext.setCurrent();
        assertNotNull(FacesContext.getCurrentInstance());

        service = Framework.getService(ContentViewService.class);
        assertNotNull(service);

        openSession();
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
    public void tearDown() throws Exception {
        closeSession();
        if (facesContext != null) {
            facesContext.relieveCurrent();
        }
        super.tearDown();
    }

    protected void createTestDocuments(DocumentModel container)
            throws ClientException {
        // create docs in descending order so that docs are not ordered by
        // title by default
        for (int i = 4; i >= 0; i--) {
            DocumentModel doc = session.createDocumentModel("Folder");
            doc.setPropertyValue("dc:title", container.getTitle()
                    + ": Document number " + i);
            doc.setPathInfo(container.getPathAsString(), "doc_" + i);
            session.createDocument(doc);
        }
        session.save();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testContentViewCache() throws Exception {
        ContentViewCache cache = new ContentViewCache();

        this.currentDocument = container1;
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
        assertEquals("document_listing",
                contentView.getCurrentResultLayout().getName());
        ContentViewLayout layout = new ContentViewLayoutImpl(
                "document_listing_2", null, false, null, false);
        contentView.setCurrentResultLayout(layout);

        cache.add(contentView);

        contentView = cache.get("CURRENT_DOCUMENT_CHILDREN");
        assertNotNull(contentView);
        pp = (PageProvider<DocumentModel>) contentView.getCurrentPageProvider();
        assertNotNull(pp);
        assertEquals(1, pp.getCurrentPageIndex());
        assertEquals(5, pp.getResultsCount());
        assertEquals("document_listing_2",
                contentView.getCurrentResultLayout().getName());

        this.currentDocument = container2;
        facesContext.mapVariable("currentDocument", currentDocument);
        contentView = cache.get("CURRENT_DOCUMENT_CHILDREN");
        assertNull(contentView);

        this.currentDocument = container1;
        facesContext.mapVariable("currentDocument", currentDocument);
        contentView = cache.get("CURRENT_DOCUMENT_CHILDREN");
        assertNotNull(contentView);
        pp = (PageProvider<DocumentModel>) contentView.getCurrentPageProvider();
        assertNotNull(pp);
        assertEquals(1, pp.getCurrentPageIndex());
        assertEquals(5, pp.getResultsCount());
        assertEquals("document_listing_2",
                contentView.getCurrentResultLayout().getName());

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
     * Non-regression test for NXP-13604: check that a page provider triggering
     * a call to {@link PageProvider#getCurrentPage()} on refresh is not
     * refreshed more than once.
     *
     * @since 5.9.2
     */
    @Test
    public void testContentViewCacheRefreshAndRewind() throws Exception {
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
        assertEquals(
                1,
                ((MockDAMPageProvider) cv.getCurrentPageProvider()).getGetCounter());

        cache.refreshAndRewindAll();

        cv.getCurrentPageProvider().getCurrentPage();
        assertEquals(
                2,
                ((MockDAMPageProvider) cv.getCurrentPageProvider()).getGetCounter());

    }
}
