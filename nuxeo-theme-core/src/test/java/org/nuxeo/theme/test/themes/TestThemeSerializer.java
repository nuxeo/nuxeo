/*
 * (C) Copyright 2006-2014 Nuxeo SA <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 */

package org.nuxeo.theme.test.themes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;

import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.Utils;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.ElementFactory;
import org.nuxeo.theme.elements.ElementFormatter;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.formats.FormatFactory;
import org.nuxeo.theme.formats.layouts.Layout;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.formats.widgets.Widget;
import org.nuxeo.theme.fragments.FragmentFactory;
import org.nuxeo.theme.nodes.NodeException;
import org.nuxeo.theme.perspectives.PerspectiveType;
import org.nuxeo.theme.test.DummyFragment;
import org.nuxeo.theme.themes.ThemeDescriptor;
import org.nuxeo.theme.themes.ThemeException;
import org.nuxeo.theme.themes.ThemeIOException;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.themes.ThemeSerializer;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy({ "org.nuxeo.theme.core:OSGI-INF/nxthemes-core-service.xml",
        "org.nuxeo.theme.core:OSGI-INF/nxthemes-core-contrib.xml" })
@LocalDeploy({ "org.nuxeo.theme.core.tests:fragment-config.xml",
        "org.nuxeo.theme.core.tests:view-config.xml" })
public class TestThemeSerializer {
    @Test
    public void testSerializeTheme() throws ThemeException, NodeException,
            ThemeIOException, IOException {
        ThemeElement theme = (ThemeElement) ElementFactory.create("theme");
        theme.setName("default");
        PageElement page = (PageElement) ElementFactory.create("page");
        page.setName("default");
        page.setDescription("The default page");
        Element section = ElementFactory.create("section");
        Element cell = ElementFactory.create("cell");
        DummyFragment fragment1 = (DummyFragment) FragmentFactory.create("dummy fragment");
        fragment1.setField1("value 1");
        fragment1.setField2("value 2");
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("one, two, three");
        fragment1.setField3(list);
        fragment1.setDescription("A dummy fragment");
        PerspectiveType perspective1 = new PerspectiveType("view", "View mode");
        PerspectiveType perspective2 = new PerspectiveType("edit", "Edit mode");
        fragment1.setVisibleInPerspective(perspective1);
        fragment1.setVisibleInPerspective(perspective2);

        DummyFragment fragment2 = (DummyFragment) FragmentFactory.create("dummy fragment");
        fragment2.setField1("value 3");

        // format the elements (e.g. with widgets)
        Widget widget0 = (Widget) FormatFactory.create("widget");
        Widget widget1 = (Widget) FormatFactory.create("widget");
        Widget widget2 = (Widget) FormatFactory.create("widget");
        Widget widget3 = (Widget) FormatFactory.create("widget");
        Widget widget4 = (Widget) FormatFactory.create("widget");

        widget0.setName("theme view");
        widget1.setName("page frame");
        widget2.setName("section frame");
        widget3.setName("cell frame");
        widget4.setName("vertical menu");

        widget0.setDescription("The theme headers");
        widget1.setDescription("This is the main page");
        widget4.setDescription("A vertical menu");

        widget0.setProperty("charset", "utf-8");
        widget0.setProperty("icon", "/nuxeo/icon.png");

        ThemeManager themeManager = Manager.getThemeManager();
        themeManager.registerFormat(widget0);
        themeManager.registerFormat(widget1);
        themeManager.registerFormat(widget2);
        themeManager.registerFormat(widget3);
        themeManager.registerFormat(widget4);

        ElementFormatter.setFormat(theme, widget0);
        ElementFormatter.setFormat(page, widget1);
        ElementFormatter.setFormat(section, widget2);
        ElementFormatter.setFormat(cell, widget3);
        ElementFormatter.setFormat(fragment1, widget4);
        ElementFormatter.setFormat(fragment2, widget4);

        Layout sectionLayout = (Layout) FormatFactory.create("layout");
        sectionLayout.setProperty("width", "100%");
        sectionLayout.setProperty("height", "300px");
        ElementFormatter.setFormat(section, sectionLayout);
        themeManager.registerFormat(sectionLayout);

        Layout cellLayout = (Layout) FormatFactory.create("layout");
        cellLayout.setProperty("width", "100px");
        ElementFormatter.setFormat(cell, cellLayout);
        themeManager.registerFormat(cellLayout);

        Style commonStyle = (Style) FormatFactory.create("style");
        commonStyle.setName("common styles");
        commonStyle.setDescription("Common styles");
        Properties a = new Properties();
        a.setProperty("color", "blue");
        a.setProperty("text-decoration", "none");
        commonStyle.setPropertiesFor("*", "a", a);
        themeManager.registerFormat(commonStyle);
        themeManager.setNamedObject("default", "style", commonStyle);

        Style sectionStyle = (Style) FormatFactory.create("style");
        themeManager.makeFormatInherit(sectionStyle, commonStyle);
        sectionStyle.setDescription("The section's style");
        Properties h1 = new Properties();
        h1.setProperty("color", "red");
        h1.setProperty("font-size", "1.2em");
        h1.setProperty("background-image", "url(\"bg.png\")");
        sectionStyle.setPropertiesFor("section frame", "h1", h1);
        themeManager.registerFormat(sectionStyle);
        ElementFormatter.setFormat(section, sectionStyle);

        theme.addChild(page).addChild(section).addChild(cell);
        cell.addChild(fragment1);
        cell.addChild(fragment2);

        themeManager.registerTheme(theme);
        themeManager.registerPage(theme, page);
        ThemeDescriptor themeDef = new ThemeDescriptor();
        themeDef.setName("default");
        themeDef.setSrc("test-default.xml");
        themeDef.setLastLoaded(new Date());
        Manager.getTypeRegistry().register(themeDef);
        assertTrue(FileUtils.areFilesContentEquals(Utils.readResourceAsString("themeSerializerOutput.xml"),
                new ThemeSerializer().serializeToXml("test-default.xml", 4)));
    }

}
