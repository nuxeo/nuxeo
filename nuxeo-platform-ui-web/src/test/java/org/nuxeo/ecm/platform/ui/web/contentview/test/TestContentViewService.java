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

    public void testExtensionPoint() throws Exception {
        ContentViewService service = Framework.getService(ContentViewService.class);
        assertNotNull(service);

        ContentView contentView = service.getContentView("document_children");
        // check content view attributes
        assertEquals("document_children", contentView.getName());
        assertEquals("CURRENT_SELECTION_LIST",
                contentView.getAvailableActionsCategory());
        assertEquals("default", contentView.getCategory());
        assertEquals("numbered", contentView.getPagination());
        assertEquals("document_listing", contentView.getResultLayout());
        assertEquals("CURRENT_DOC_CHILDREN", contentView.getResultProvider());
        assertEquals("search_layout", contentView.getSearchLayout());
        assertEquals("CURRENT_SELECTION", contentView.getSelectionList());
        assertEquals((Integer) 0, contentView.getMax());

    }

}
