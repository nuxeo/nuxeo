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
package org.nuxeo.ecm.platform.ui.web.contentview.test;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.el.ELException;
import javax.faces.context.FacesContext;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PageProvider;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.ui.web.contentview.ContentView;
import org.nuxeo.ecm.platform.ui.web.contentview.ContentViewService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Anahide Tchertchian
 */
public class TestDefaultPageProviders extends SQLRepositoryTestCase {

    ContentViewService service;

    MockFacesContext facesContext;

    @Override
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        super.setUp();

        deployContrib("org.nuxeo.ecm.platform.ui",
                "OSGI-INF/contentview-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.ui.test",
                "test-contentview-contrib.xml");

        // set mock faces context for needed properties resolution
        facesContext = new MockFacesContext() {
            public Object evaluateExpressionGet(FacesContext context,
                    String expression, Class expectedType) throws ELException {
                if ("#{documentManager}".equals(expression)) {
                    return session;
                }
                return null;
            }
        };
        facesContext.setCurrent();
        assertNotNull(FacesContext.getCurrentInstance());

        service = Framework.getService(ContentViewService.class);
        assertNotNull(service);

        openSession();

        createTestDocuments();
    }

    protected void createTestDocuments() throws ClientException {
        DocumentModel root = session.getRootDocument();
        for (int i = 0; i < 5; i++) {
            DocumentModel doc = session.createDocumentModel("Folder");
            doc.setPropertyValue("dc:title", "Document number " + i);
            doc.setPathInfo(root.getPathAsString(), "doc_" + i);
            session.createDocument(doc);
        }
        session.save();
    }

    @SuppressWarnings("unchecked")
    public void testCoreQuery() throws Exception {
        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN");
        assertNotNull(contentView);

        String param = session.getRootDocument().getId();
        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) contentView.getPageProvider(param);
        assertNotNull(pp);

        assertEquals(-1, pp.getResultsCount());
        assertEquals(0, pp.getNumberOfPages());

        // init results
        List<DocumentModel> docs = pp.getCurrentPage();

        assertEquals(5, pp.getResultsCount());
        assertEquals(3, pp.getNumberOfPages());
        assertFalse(pp.isPreviousPageAvailable());
        assertTrue(pp.isNextPageAvailable());

        assertNotNull(docs);
        assertEquals(2, docs.size());
        assertEquals("Document number 0", docs.get(0).getPropertyValue(
                "dc:title"));
        assertEquals("Document number 1", docs.get(1).getPropertyValue(
                "dc:title"));

        pp.nextPage();
        docs = pp.getCurrentPage();
        assertEquals(5, pp.getResultsCount());
        assertEquals(3, pp.getNumberOfPages());
        assertTrue(pp.isPreviousPageAvailable());
        assertTrue(pp.isNextPageAvailable());

        assertNotNull(docs);
        assertEquals(2, docs.size());
        assertEquals("Document number 2", docs.get(0).getPropertyValue(
                "dc:title"));
        assertEquals("Document number 3", docs.get(1).getPropertyValue(
                "dc:title"));
    }

    public void testCoreQueryAndFetch() throws Exception {
        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN_FETCH");
        assertNotNull(contentView);

        String param = session.getRootDocument().getId();
        PageProvider<Map<String, Serializable>> pp = (PageProvider<Map<String, Serializable>>) contentView.getPageProvider(param);
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
        assertEquals("Document number 0", docs.get(0).get("dc:title"));
        assertEquals(1, docs.get(1).size());
        assertEquals("Document number 1", docs.get(1).get("dc:title"));

        pp.nextPage();
        docs = pp.getCurrentPage();
        assertEquals(5, pp.getResultsCount());
        assertEquals(3, pp.getNumberOfPages());
        assertTrue(pp.isPreviousPageAvailable());
        assertTrue(pp.isNextPageAvailable());

        assertNotNull(docs);
        assertEquals(2, docs.size());
        assertEquals(1, docs.get(0).size());
        assertEquals("Document number 2", docs.get(0).get("dc:title"));
        assertEquals(1, docs.get(1).size());
        assertEquals("Document number 3", docs.get(1).get("dc:title"));
    }

    public void testCoreQueryWithSearchDocument() throws Exception {
        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT");
        assertNotNull(contentView);

    }

}
