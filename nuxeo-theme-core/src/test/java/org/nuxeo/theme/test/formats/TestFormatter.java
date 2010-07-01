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

package org.nuxeo.theme.test.formats;

import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.elements.ElementFormatter;
import org.nuxeo.theme.formats.Format;
import org.nuxeo.theme.formats.FormatFactory;
import org.nuxeo.theme.test.DummyFragment;
import org.nuxeo.theme.themes.ThemeException;

public class TestFormatter extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-contrib.xml");
    }

    public void testFormatter() throws ThemeException {
        DummyFragment fragment = new DummyFragment();
        Format widget = FormatFactory.create("widget");
        widget.setName("vertical menu");

        // Make the objects identifiable
        fragment.setUid(1);
        widget.setUid(2);

        assertTrue(ElementFormatter.getFormatsFor(fragment).isEmpty());
        assertNull(ElementFormatter.getFormatFor(fragment, "widget"));
        assertNull(ElementFormatter.getFormatByType(fragment,
                widget.getFormatType()));
        assertFalse(ElementFormatter.getElementsFor(widget).contains(fragment));

        // Add format
        ElementFormatter.setFormat(fragment, widget);

        assertTrue(ElementFormatter.getFormatsFor(fragment).contains(widget));
        assertSame(widget, ElementFormatter.getFormatFor(fragment, "widget"));
        assertEquals(
                ElementFormatter.getFormatByType(fragment,
                        widget.getFormatType()), widget);
        assertTrue(ElementFormatter.getElementsFor(widget).contains(fragment));

        // Replace format
        Format widget2 = FormatFactory.create("widget");
        widget2.setName("horizontal menu");
        widget2.setUid(3);
        assertSame(widget, ElementFormatter.getFormatFor(fragment, "widget"));
        ElementFormatter.setFormat(fragment, widget2);
        assertTrue(ElementFormatter.getElementsFor(widget2).contains(fragment));
        assertTrue(ElementFormatter.getElementsFor(widget).isEmpty());
        assertSame(widget2, ElementFormatter.getFormatFor(fragment, "widget"));

        // Remove format
        ElementFormatter.removeFormat(fragment, widget2);

        assertTrue(ElementFormatter.getFormatsFor(fragment).isEmpty());
        assertNull(ElementFormatter.getFormatFor(fragment, "widget"));
        assertNull(ElementFormatter.getFormatByType(fragment,
                widget2.getFormatType()));
        assertFalse(ElementFormatter.getElementsFor(widget2).contains(fragment));
    }

}
