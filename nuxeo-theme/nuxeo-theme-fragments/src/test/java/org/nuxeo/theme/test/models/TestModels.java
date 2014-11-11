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

package org.nuxeo.theme.test.models;

import junit.framework.TestCase;

import org.nuxeo.theme.models.Html;
import org.nuxeo.theme.models.MenuItem;
import org.nuxeo.theme.models.Text;

public class TestModels extends TestCase {

    public void testText() {
        Text text = new Text("content here");
        assertEquals("text", text.getModelTypeName());
        assertEquals("content here", text.getBody());
    }

    public void testHTML() {
        Html html = new Html("<p>content here</p>");
        assertEquals("html", html.getModelTypeName());
        assertEquals("<p>content here</p>", html.getBody());
    }

    public void testMenuItem() {
        MenuItem menuitem = new MenuItem("title", "description", "url", true,
                "icon.png");
        assertEquals("menu item", menuitem.getModelTypeName());
        assertEquals("title", menuitem.getTitle());
        assertEquals("description", menuitem.getDescription());
        assertEquals("url", menuitem.getUrl());
        assertTrue(menuitem.isSelected());
        assertEquals("icon.png", menuitem.getIcon());

        MenuItem submenuitem = new MenuItem("title sub-menu",
                "description sub-menu", "url sub-menu", false, "icon.png");
        menuitem.addChild(submenuitem);
        assertSame(submenuitem, menuitem.getChildren().iterator().next());
    }

}
