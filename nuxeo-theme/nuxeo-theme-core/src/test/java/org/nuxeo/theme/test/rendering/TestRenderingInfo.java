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

package org.nuxeo.theme.test.rendering;

import java.net.MalformedURLException;
import java.net.URL;

import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.ElementFactory;
import org.nuxeo.theme.models.InfoPool;
import org.nuxeo.theme.rendering.RenderingInfo;
import org.nuxeo.theme.test.DummyHtml;

public class TestRenderingInfo extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-contrib.xml");
        deployContrib("org.nuxeo.theme.core.tests", "engine-config.xml");
        deployContrib("org.nuxeo.theme.core.tests",
                "template-engine-config.xml");
    }

    public void testInfoUid() {
        Element page = ElementFactory.create("page");
        RenderingInfo info = new RenderingInfo(page, null);
        assertNotNull(info.getUid());
    }

    public void testInfoClone() throws MalformedURLException {
        Element page = ElementFactory.create("page");
        URL themeUrl = new URL(
                "nxtheme://theme/test-engine/mode/theme/page/perspective/test-template-engine");
        RenderingInfo info = new RenderingInfo(page, themeUrl);

        assertEquals("test-engine", info.getEngine().getName());

        info.setModel(new DummyHtml("some data"));
        info.setMarkup("some markup");

        // clone the info structure
        RenderingInfo copy = info.createCopy();

        // make sure that a new object is created
        assertNotSame(copy, info);

        // make sure that the settings are preserved
        assertEquals(copy.getUid(), info.getUid());
        assertEquals(copy.getEngine().getName(), info.getEngine().getName());
        assertEquals(copy.getThemeUrl(), info.getThemeUrl());

        // the model and markup are not copied
        assertNull(copy.getModel());
        assertEquals("", copy.getMarkup());
    }

    public void testInfoPool() {
        Element page = ElementFactory.create("page");
        RenderingInfo info = new RenderingInfo(page, null);

        String infoId = InfoPool.computeInfoId(info);
        InfoPool.register(info);
        assertSame(info, InfoPool.get(infoId));
        assertTrue(InfoPool.getInfoMap().containsValue(info));
    }

}
