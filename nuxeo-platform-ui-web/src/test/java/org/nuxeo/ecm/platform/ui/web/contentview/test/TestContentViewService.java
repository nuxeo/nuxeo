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
import org.nuxeo.ecm.platform.ui.web.contentview.ContentViewLayout;
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
        assertNotNull(contentView);
        // check content view attributes
        assertEquals("CURRENT_DOCUMENT_CHILDREN", contentView.getName());
        assertEquals("current document children", contentView.getTitle());
        assertFalse(contentView.getTranslateTitle());
        assertEquals("/icons/document_listing_icon.png",
                contentView.getIconPath());
        assertEquals("CURRENT_SELECTION_LIST",
                contentView.getActionsCategories().get(0));
        assertEquals("simple", contentView.getPagination());

        List<ContentViewLayout> resultLayouts = contentView.getResultLayouts();
        assertNotNull(resultLayouts);
        assertEquals(1, resultLayouts.size());
        assertEquals("document_listing", resultLayouts.get(0).getName());
        assertEquals("label.document_listing.layout",
                resultLayouts.get(0).getTitle());
        assertTrue(resultLayouts.get(0).getTranslateTitle());
        assertEquals("/icons/myicon.png", resultLayouts.get(0).getIconPath());

        assertEquals("search_layout", contentView.getSearchLayout().getName());
        assertEquals(null, contentView.getSearchLayout().getTitle());
        assertFalse(contentView.getSearchLayout().getTranslateTitle());
        assertEquals(null, contentView.getSearchLayout().getIconPath());

        assertEquals("CURRENT_SELECTION", contentView.getSelectionListName());
        List<String> eventNames = contentView.getRefreshEventNames();
        assertNotNull(eventNames);
        assertEquals(1, eventNames.size());
        assertEquals("documentChildrenChanged", eventNames.get(0));
        assertFalse(contentView.getUseGlobalPageSize());
    }

    public void testOverride() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.ui.test",
                "test-contentview-override-contrib.xml");

        ContentViewService service = Framework.getService(ContentViewService.class);
        assertNotNull(service);

        assertNull(service.getContentView("foo"));

        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN");
        assertNotNull(contentView);
        // check content view attributes
        assertEquals("CURRENT_DOCUMENT_CHILDREN", contentView.getName());
        assertEquals("current document children overriden",
                contentView.getTitle());
        assertFalse(contentView.getTranslateTitle());
        assertEquals("/icons/document_listing_icon.png",
                contentView.getIconPath());
        assertEquals("CURRENT_SELECTION_LIST_2",
                contentView.getActionsCategories().get(0));
        assertEquals("simple_2", contentView.getPagination());

        List<ContentViewLayout> resultLayouts = contentView.getResultLayouts();
        assertNotNull(resultLayouts);
        assertEquals(2, resultLayouts.size());
        assertEquals("document_listing", resultLayouts.get(0).getName());
        assertEquals("label.document_listing.layout",
                resultLayouts.get(0).getTitle());
        assertTrue(resultLayouts.get(0).getTranslateTitle());
        assertEquals("/icons/myicon.png", resultLayouts.get(0).getIconPath());
        assertEquals("document_listing_2", resultLayouts.get(1).getName());
        assertEquals("label.document_listing.layout_2",
                resultLayouts.get(1).getTitle());
        assertTrue(resultLayouts.get(1).getTranslateTitle());
        assertNull(resultLayouts.get(1).getIconPath());

        assertEquals("search_layout_2", contentView.getSearchLayout().getName());
        assertEquals(null, contentView.getSearchLayout().getTitle());
        assertFalse(contentView.getSearchLayout().getTranslateTitle());
        assertEquals(null, contentView.getSearchLayout().getIconPath());

        assertEquals("CURRENT_SELECTION_2", contentView.getSelectionListName());
        List<String> eventNames = contentView.getRefreshEventNames();
        assertNotNull(eventNames);
        assertEquals(1, eventNames.size());
        assertEquals("documentChildrenChanged", eventNames.get(0));
        assertTrue(contentView.getUseGlobalPageSize());
    }

}
