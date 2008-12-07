/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.gwt.test.client;

import org.nuxeo.ecm.webengine.gwt.client.UI;
import org.nuxeo.ecm.webengine.gwt.client.ui.View;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TestView extends View {

    public TestView() {
        super("dummy");
        setTitle("Dummy");
    }

    @Override
    protected Widget createContent() {
        Button button2 = new Button("Open some text");
        button2.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                HTML w = new HTML("<h1>My Content</h1>Some html text!");
                UI.openInEditor(w);
                // switch to "login" view
                UI.showView("login");
            }
        });
        return button2;
    }

    @Override
    public Image getIcon() {
        return Main.getImages().mailgroup().createImage();
    }


}
