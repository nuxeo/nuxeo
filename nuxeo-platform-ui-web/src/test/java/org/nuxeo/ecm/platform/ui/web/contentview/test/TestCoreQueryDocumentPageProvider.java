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

import java.util.List;

import javax.el.ELException;
import javax.faces.context.FacesContext;

import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.ui.web.contentview.ContentView;
import org.nuxeo.ecm.platform.ui.web.contentview.ContentViewService;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Anahide Tchertchian
 */
public class TestCoreQueryDocumentPageProvider extends SQLRepositoryTestCase {

    MockFacesContext facesContext;

    @Override
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
    }

    public void testExtensionPoint() throws Exception {
        ContentViewService service = Framework.getService(ContentViewService.class);
        assertNotNull(service);

        ContentView contentView = service.getContentView("document_children");
        // check content view attributes
        assertEquals("document_children", contentView.getName());
        assertEquals("CURRENT_SELECTION_LIST",
                contentView.getActionsCategories().get(0));
        assertEquals("CURRENT_SELECTION_LIST_2",
                contentView.getActionsCategories().get(1));
        assertEquals("numbered", contentView.getPagination());
        assertEquals("document_listing", contentView.getResultLayoutName());
        assertEquals("search_layout", contentView.getSearchLayoutName());
        assertEquals("CURRENT_SELECTION", contentView.getSelectionListName());
        List<String> eventNames = contentView.getRefreshEventNames();
        assertNotNull(eventNames);
        assertEquals(2, eventNames.size());
        assertEquals("foo", eventNames.get(0));
        assertEquals("bar", eventNames.get(1));

        assertEquals(session, ComponentTagUtils.resolveElExpression(
                facesContext, "#{documentManager}"));

        // TODO: test provider behavior

    }
}
