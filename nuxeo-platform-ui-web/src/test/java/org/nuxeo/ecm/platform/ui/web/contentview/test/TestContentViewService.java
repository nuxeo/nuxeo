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

import org.nuxeo.ecm.platform.ui.web.contentview.ContentView;
import org.nuxeo.ecm.platform.ui.web.contentview.ContentViewService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Anahide Tchertchian
 */
public class TestContentViewService extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployContrib("org.nuxeo.ecm.platform.ui",
                "OSGI-INF/contentview-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.ui.test",
                "test-contentview-contrib.xml");
    }

    public void testRegistration() throws Exception {
        ContentViewService service = Framework.getService(ContentViewService.class);
        assertNotNull(service);

        assertNull(service.getContentView("foo"));

        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN");
        // check content view attributes
        assertEquals("CURRENT_DOCUMENT_CHILDREN", contentView.getName());
        assertEquals("CURRENT_SELECTION_LIST",
                contentView.getActionsCategories().get(0));
        assertEquals("simple", contentView.getPagination());
        assertEquals("document_listing", contentView.getResultLayoutName());
        assertEquals("search_layout", contentView.getSearchLayoutName());
        assertEquals("CURRENT_SELECTION", contentView.getSelectionListName());
        List<String> eventNames = contentView.getRefreshEventNames();
        assertNotNull(eventNames);
        assertEquals(1, eventNames.size());
        assertEquals("documentChildrenChanged", eventNames.get(0));
    }

    public void testOverride() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.ui.test",
                "test-contentview-override-contrib.xml");

        ContentViewService service = Framework.getService(ContentViewService.class);
        assertNotNull(service);

        assertNull(service.getContentView("foo"));

        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN");
        // check content view attributes
        assertEquals("CURRENT_DOCUMENT_CHILDREN", contentView.getName());
        assertEquals("CURRENT_SELECTION_LIST_2",
                contentView.getActionsCategories().get(0));
        assertEquals("simple_2", contentView.getPagination());
        assertEquals("document_listing_2", contentView.getResultLayoutName());
        assertEquals("search_layout", contentView.getSearchLayoutName());
        assertEquals("CURRENT_SELECTION_2", contentView.getSelectionListName());
        List<String> eventNames = contentView.getRefreshEventNames();
        assertNotNull(eventNames);
        assertEquals(1, eventNames.size());
        assertEquals("documentChildrenChanged", eventNames.get(0));
    }

}
