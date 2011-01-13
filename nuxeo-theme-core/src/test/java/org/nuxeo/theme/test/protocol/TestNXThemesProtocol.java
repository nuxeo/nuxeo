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

import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.ElementFactory;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.engines.EngineType;
import org.nuxeo.theme.nodes.NodeException;
import org.nuxeo.theme.perspectives.PerspectiveType;
import org.nuxeo.theme.templates.TemplateEngineType;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.types.TypeRegistry;

public class TestNXThemesProtocol extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-contrib.xml");

    }

    public void testProtocol() throws MalformedURLException {
        URL url = new URL("nxtheme://element/engine/123");
        // make sure that the URL got created
        assertEquals("nxtheme", url.getProtocol());
    }

    public void testGetElementByUrl() throws MalformedURLException {
        Element element = ElementFactory.create("page");
        URL url = new URL("nxtheme://element/engine/" + element.getUid());
        assertSame(element, ThemeManager.getElementByUrl(url));
    }

    public void testGetEngineByUrl() throws MalformedURLException {
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        EngineType engine = new EngineType();
        engine.setName("engine");
        typeRegistry.register(engine);
        URL themeUrl = new URL("nxtheme://theme/engine/mode/templateEngine");
        assertSame(engine, ThemeManager.getEngineByUrl(themeUrl));
        URL elementUrl = new URL(
                "nxtheme://element/engine/mode/templateEngine/123");
        assertSame(engine, ThemeManager.getEngineByUrl(elementUrl));
    }

    public void testGetTemplateEngineByUrl() throws MalformedURLException {
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        TemplateEngineType templateEngine = new TemplateEngineType();
        templateEngine.setName("templateEngine");
        typeRegistry.register(templateEngine);
        URL themeUrl = new URL("nxtheme://theme/engine/mode/templateEngine");
        assertSame(templateEngine,
                ThemeManager.getTemplateEngineByUrl(themeUrl));
        URL elementUrl = new URL(
                "nxtheme://element/engine/mode/templateEngine/123");
        assertSame(templateEngine,
                ThemeManager.getTemplateEngineByUrl(elementUrl));
    }

    public void testGetThemePageByUrl() throws MalformedURLException,
            NodeException {
        ThemeManager themeManager = Manager.getThemeManager();
        ThemeElement theme = (ThemeElement) ElementFactory.create("theme");
        PageElement page = (PageElement) ElementFactory.create("page");
        theme.setName("theme1");
        page.setName("page1");
        theme.addChild(page);
        themeManager.registerTheme(theme);
        URL url = new URL(
                "nxtheme://theme/engine/mode/templateEngine/theme1/page1");
        assertSame(page, themeManager.getThemePageByUrl(url));
        assertSame(page, themeManager.getPageByPath("theme1/page1"));
    }

    public void testGetThemeByUrl() throws MalformedURLException {
        ThemeManager themeManager = Manager.getThemeManager();
        ThemeElement theme = (ThemeElement) ElementFactory.create("theme");
        theme.setName("theme1");
        themeManager.registerTheme(theme);
        URL url = new URL(
                "nxtheme://theme/engine/mode/templateEngine/theme1/page1");
        assertSame(theme, themeManager.getThemeByUrl(url));
    }

    public void testGetPagePathByUrl() throws MalformedURLException {
        ThemeManager themeManager = Manager.getThemeManager();
        URL url = new URL(
                "nxtheme://theme/engine/mode/templateEngine/theme1/page1");
        assertEquals("theme1/page1", themeManager.getPagePathByUrl(url));
    }

    public void testGetPerspectiveByUrl() throws MalformedURLException {
        PerspectiveType perspective = new PerspectiveType("perspective",
                "Perspective");
        Manager.getTypeRegistry().register(perspective);
        URL url = new URL(
                "nxtheme://theme/engine/mode/templateEngine/theme/page/perspective");
        assertSame(perspective, ThemeManager.getPerspectiveByUrl(url));

        url = new URL("nxtheme://element/engine/mode/templateEngine/12345");
        assertNull(ThemeManager.getPerspectiveByUrl(url));
    }

    public void testGetViewModeByUrl() throws MalformedURLException {
        URL url = new URL(
                "nxtheme://theme/engine/mode/templateEngine//theme/page/view");
        assertEquals("mode", ThemeManager.getViewModeByUrl(url));
    }

    public void testReset() {
        Manager.resetProtocols();
        boolean caught = false;
        try {
            new URL("nxtheme:foo");
        } catch (MalformedURLException e) {
            caught = true;
        }
        assertTrue(caught);
    }

}
