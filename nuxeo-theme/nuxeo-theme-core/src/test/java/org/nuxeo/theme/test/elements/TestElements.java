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

package org.nuxeo.theme.test.elements;

import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.ElementFactory;
import org.nuxeo.theme.nodes.NodeException;

public class TestElements extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-contrib.xml");
    }

    public void testTheme() {
        Element theme = ElementFactory.create("theme");
        assertEquals("theme", theme.getElementType().getTypeName());
        assertFalse(theme.isLeaf());
    }

    public void testPage() {
        Element page = ElementFactory.create("page");
        assertEquals("page", page.getElementType().getTypeName());
        assertFalse(page.isLeaf());
    }

    public void testSection() {
        Element section = ElementFactory.create("section");
        assertEquals("section", section.getElementType().getTypeName());
        assertFalse(section.isLeaf());
    }

    public void testCell() {
        Element cell = ElementFactory.create("cell");
        assertEquals("cell", cell.getElementType().getTypeName());
        assertFalse(cell.isLeaf());
    }

    public void testXPath() throws NodeException {
        Element theme = ElementFactory.create("theme");
        Element page = ElementFactory.create("page");
        Element section1 = ElementFactory.create("section");
        Element section2 = ElementFactory.create("section");
        Element cell1 = ElementFactory.create("cell");
        Element cell2 = ElementFactory.create("cell");
        Element cell3 = ElementFactory.create("cell");
        theme.addChild(page);
        page.addChild(section1);
        page.addChild(section2);
        section1.addChild(cell1);
        section1.addChild(cell2);
        section2.addChild(cell3);

        assertEquals("", theme.computeXPath());
        assertEquals("page[1]", page.computeXPath());
        assertEquals("page[1]/section[1]", section1.computeXPath());
        assertEquals("page[1]/section[2]", section2.computeXPath());
        assertEquals("page[1]/section[1]/cell[1]", cell1.computeXPath());
        assertEquals("page[1]/section[1]/cell[2]", cell2.computeXPath());
        assertEquals("page[1]/section[2]/cell[1]", cell3.computeXPath());
    }

}
