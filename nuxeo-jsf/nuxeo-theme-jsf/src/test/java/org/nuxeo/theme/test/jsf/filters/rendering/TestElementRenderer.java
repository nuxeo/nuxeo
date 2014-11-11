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

package org.nuxeo.theme.test.jsf.filters.rendering;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.ElementFactory;
import org.nuxeo.theme.elements.ElementFormatter;
import org.nuxeo.theme.elements.ElementRenderer;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.formats.Format;
import org.nuxeo.theme.fragments.Fragment;
import org.nuxeo.theme.fragments.FragmentFactory;
import org.nuxeo.theme.nodes.NodeException;
import org.nuxeo.theme.rendering.RenderingInfo;
import org.nuxeo.theme.themes.ThemeManager;

public class TestElementRenderer extends NXRuntimeTestCase {

    private static final Log log = LogFactory.getLog(TestElementRenderer.class);

    private ThemeManager themeManager;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Manager.initializeProtocols();
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-contrib.xml");
        deployContrib("org.nuxeo.theme.html",
                "OSGI-INF/nxthemes-html-contrib.xml");

        deployContrib("org.nuxeo.theme.jsf.tests", "fragment-config.xml");
        themeManager = Manager.getThemeManager();
    }

    @Override
    public void tearDown() throws Exception {
        // clear relations that have been set through ElementFormatter
        Manager.getRelationStorage().clear();
        super.tearDown();
        themeManager = null;
    }

    public void testElement() throws MalformedURLException, NodeException {

        URL themeUrl = new URL(
                "nxtheme://theme/default/mode/jsf-facelets/theme/page");

        // create the elements to render
        ThemeElement theme = (ThemeElement) ElementFactory.create("theme");
        PageElement page = (PageElement) ElementFactory.create("page");
        Element section = ElementFactory.create("section");
        Element cell = ElementFactory.create("cell");
        Fragment fragment = FragmentFactory.create("dummy fragment");

        theme.setName("theme");
        page.setName("page");

        // override the fragment's uid for the test
        fragment.setUid(1);

        // format the elements (e.g. with widgets)
        Format widget1 = themeManager.createWidget();
        Format widget2 = themeManager.createWidget();
        Format widget3 = themeManager.createWidget();
        Format widget4 = themeManager.createWidget();

        widget1.setName("page frame");
        widget2.setName("section frame");
        widget3.setName("cell frame");
        widget4.setName("dummy vertical menu");

        ElementFormatter.setFormat(page, widget1);
        ElementFormatter.setFormat(section, widget2);
        ElementFormatter.setFormat(cell, widget3);
        ElementFormatter.setFormat(fragment, widget4);

        Format sectionLayout = themeManager.createLayout();
        sectionLayout.setProperty("width", "100%");
        sectionLayout.setProperty("height", "300px");
        ElementFormatter.setFormat(section, sectionLayout);

        Format cellLayout = themeManager.createLayout();
        cellLayout.setProperty("width", "100px");
        ElementFormatter.setFormat(cell, cellLayout);

        // create the element tree
        theme.addChild(page).addChild(section).addChild(cell).addChild(fragment);

        Manager.getThemeManager().registerTheme(theme);

        // render the tree
        RenderingInfo info = new RenderingInfo(theme, themeUrl);
        assertEquals(getFileContent("elementRendererOutput.xml"),
                ElementRenderer.render(info).getMarkup());
    }

    private String getFileContent(String name) {
        InputStream is = null;
        StringBuilder content = new StringBuilder();
        try {
            is = getClass().getClassLoader().getResourceAsStream(name);
            if (is != null) {
                Reader in = null;
                try {
                    in = new BufferedReader(new InputStreamReader(is));
                    int ch;
                    while ((ch = in.read()) > -1) {
                        content.append((char) ch);
                    }
                } catch (IOException e) {
                    log.error(e, e);
                } finally {
                    if (in != null) {
                        in.close();
                    }
                }
            }
        } catch (IOException e) {
            log.error(e, e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    log.error(e, e);
                } finally {
                    is = null;
                }
            }
        }
        return content.toString().trim();
    }
}
