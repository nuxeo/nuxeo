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

import javax.el.ELException;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    private static final Log log = LogFactory.getLog(TestDefaultPageProviders.class);

    ContentViewService service;

    MockFacesContext facesContext;

    DocumentModel currentDocument;

    DocumentModel container1;

    DocumentModel container2;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployContrib("org.nuxeo.ecm.platform.query.api",
                "OSGI-INF/pageprovider-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.contentview.jsf",
                "OSGI-INF/contentview-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.contentview.jsf.test",
                "test-contentview-contrib.xml");

        // set mock faces context for needed properties resolution
        facesContext = new MockFacesContext() {
            @Override
            public Object evaluateExpressionGet(FacesContext context,
                    String expression, Class expectedType) throws ELException {
                if ("#{documentManager}".equals(expression)) {
                    return session;
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

        service = Framework.getService(ContentViewService.class);
        assertNotNull(service);

        openSession();

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

    @Override
    public void tearDown() throws Exception {
        facesContext.relieveCurrent();
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

    @SuppressWarnings("unchecked")
    public void testContentViewCache() throws Exception {
        ContentViewCache cache = new ContentViewCache();

        this.currentDocument = container1;
        ContentView contentView = service.getContentView(
                "CURRENT_DOCUMENT_CHILDREN", session);
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
        assertEquals(-1, pp.getResultsCount());
        // init results
        pp.getCurrentPage();
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
        contentView = cache.get("CURRENT_DOCUMENT_CHILDREN");
        assertNull(contentView);

        this.currentDocument = container1;
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

}
