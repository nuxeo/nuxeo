/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.test.html;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.html.HTMLView;
import org.nuxeo.theme.views.ViewType;

public class TestHTMLView extends NXRuntimeTestCase {
    DummyRenderingInfo info;

    PageElement page;

    HTMLView view;

    ViewType viewType;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-contrib.xml");

        info = new DummyRenderingInfo(new PageElement(), null);

        view = new HTMLView();
        viewType = new ViewType();

        List<String> resources = new ArrayList<String>();
        viewType.setResources(resources);
        view.setViewType(viewType);
    }

    public void test1() {
        info.setUid(1);
        info.setMarkup("<div>content</div>");
        viewType.setTemplate("test-template.xml");
        String result = view.render(info).replaceAll("\r?\n", "");
        assertEquals(
                "<div>#{nxthemesInfo.element.uid}#{nxthemesInfo.format.properties.charset}#{ (nxthemesInfo.test) ? \"on\" : \"off\"}<div>content</div></div>",
                result);
    }

    @Override
    public void tearDown() throws Exception {
        view = null;
        info = null;
        page = null;
        super.tearDown();
    }

}
