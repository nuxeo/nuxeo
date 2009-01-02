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
import org.nuxeo.theme.models.Menu;
import org.nuxeo.theme.models.MenuItem;
import org.nuxeo.theme.models.ModelException;
import org.nuxeo.theme.models.Text;

public class TestModels extends TestCase {

    public void testText() {
        Text text = new Text("content here");
        assertEquals("content here", text.getBody());
    }

    public void testHTML() {
        Html html = new Html("<p>content here</p>");
        assertEquals("<p>content here</p>", html.getBody());
    }

    public void Menu() throws ModelException {
        Menu menu = new Menu();
        MenuItem menuitem = new MenuItem("title sub-menu",
                "description sub-menu", "url sub-menu", false, "icon.png");
        menu.addItem(menuitem);
        assertSame(menuitem, menu.getItems().iterator().next());
    }

}
