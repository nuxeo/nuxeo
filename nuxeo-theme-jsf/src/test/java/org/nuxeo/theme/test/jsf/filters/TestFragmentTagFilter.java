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

package org.nuxeo.theme.test.jsf.filters;

import java.net.URL;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.engines.EngineType;
import org.nuxeo.theme.jsf.filters.standalone.FragmentTag;
import org.nuxeo.theme.rendering.Filter;
import org.nuxeo.theme.services.ThemeService;
import org.nuxeo.theme.types.TypeRegistry;

public class TestFragmentTagFilter extends NXRuntimeTestCase {

    private ThemeService themeService;

    private TypeRegistry typeRegistry;

    DummyRenderingInfo info;

    Filter filter;

    PageElement page;

    EngineType engine;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Manager.initializeProtocols();
        deployContrib("org.nuxeo.theme.core", "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core", "OSGI-INF/nxthemes-core-contrib.xml");

        // create the elements to render
        page = new PageElement();
        page.setUid(1);

        // register test engine
        engine = new EngineType();
        engine.setName("engine");

        themeService = (ThemeService) Framework.getRuntime().getComponent(
                ThemeService.ID);
        typeRegistry = (TypeRegistry) themeService.getRegistry("types");

        typeRegistry.register(engine);
        info = new DummyRenderingInfo(page, new URL(
                "nxtheme://element/engine/mode/1234"));
        filter = new FragmentTag();
    }

    public void testFilter1() {
        info.setMarkup("<div>orginal markup</div>");
        info.setDirty(true);
        filter.process(info, true);
        assertEquals(
                "<nxthemes:fragment xmlns:nxthemes=\"http://nuxeo.org/nxthemes\" "
                    + "uid=\"1\" engine=\"engine\" mode=\"mode\" />",
                info.getMarkup());
    }

    public void testFilter2() {
        info.setMarkup("<div>orginal markup</div>");
        info.setDirty(false);
        filter.process(info, true);
        assertEquals("<div>orginal markup</div>", info.getMarkup());
    }

}
