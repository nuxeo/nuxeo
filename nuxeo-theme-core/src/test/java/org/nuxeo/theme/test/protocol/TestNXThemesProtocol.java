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

package org.nuxeo.theme.test.protocol;

import java.net.MalformedURLException;
import java.net.URL;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.ElementFactory;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.engines.EngineType;
import org.nuxeo.theme.perspectives.PerspectiveType;
import org.nuxeo.theme.services.ThemeService;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.types.TypeRegistry;

public class TestNXThemesProtocol extends NXRuntimeTestCase {

    private TypeRegistry typeRegistry;

    private ThemeManager themeManager;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Manager.initializeProtocols();
        deploy("nxthemes-core-service.xml");
        deploy("nxthemes-core-contrib.xml");
        ThemeService themeService = (ThemeService) Framework.getRuntime().getComponent(
                ThemeService.ID);
        typeRegistry = (TypeRegistry) themeService.getRegistry("types");
        themeManager = (ThemeManager) themeService.getRegistry("themes");
    }

    public void testProtocol() throws MalformedURLException {
        URL url = new URL("nxtheme://element/engine/123");
        // make sure that the URL got created
        assertEquals("nxtheme", url.getProtocol());
    }

    public void testGetElementByUrl() throws MalformedURLException {
        Element element = ElementFactory.create("page");
        URL url = new URL("nxtheme://element/engine/" + element.getUid());
        assertSame(element, themeManager.getElementByUrl(url));
    }

    public void testGetEngineByUrl() throws MalformedURLException {
        EngineType engine = new EngineType();
        engine.setName("engine");
        typeRegistry.register(engine);
        URL themeUrl = new URL("nxtheme://theme/engine/a/b");
        assertSame(engine, themeManager.getEngineByUrl(themeUrl));
        URL elementUrl = new URL("nxtheme://element/engine/123");
        assertSame(engine, themeManager.getEngineByUrl(elementUrl));
    }

    public void testGetThemePageByUrl() throws MalformedURLException {
        ThemeElement theme = (ThemeElement) ElementFactory.create("theme");
        PageElement page = (PageElement) ElementFactory.create("page");
        theme.setName("theme1");
        page.setName("page1");
        theme.addChild(page);
        themeManager.registerTheme(theme);
        URL url = new URL("nxtheme://theme/engine/mode/theme1/page1");
        assertSame(page, themeManager.getThemePageByUrl(url));
        assertSame(page, themeManager.getPageByPath("theme1/page1"));
    }

    public void testGetThemeByUrl() throws MalformedURLException {
        ThemeElement theme = (ThemeElement) ElementFactory.create("theme");
        theme.setName("theme1");
        themeManager.registerTheme(theme);
        URL url = new URL("nxtheme://theme/engine/mode/theme1/page1");
        assertSame(theme, themeManager.getThemeByUrl(url));
    }

    public void testGetPerspectiveByUrl() throws MalformedURLException {
        PerspectiveType perspective = new PerspectiveType("view", "View");
        Manager.getTypeRegistry().register(perspective);
        URL url = new URL("nxtheme://theme/engine/mode/theme/page/view");
        assertSame(perspective, themeManager.getPerspectiveByUrl(url));

        // element urls have no perspective
        url = new URL("nxtheme://element/engine/12345");
        assertNull(themeManager.getPerspectiveByUrl(url));
    }

    public void testGetViewModeByUrl() throws MalformedURLException {
        URL url = new URL("nxtheme://theme/engine/view mode/theme/page/view");
        assertEquals("view mode", themeManager.getViewModeByUrl(url));
    }

    @Override
    public void tearDown() throws Exception {
        typeRegistry = null;
        themeManager = null;
        super.tearDown();
    }

}
