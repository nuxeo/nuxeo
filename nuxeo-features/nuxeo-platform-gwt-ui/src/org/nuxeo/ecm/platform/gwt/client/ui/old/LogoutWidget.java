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

package org.nuxeo.ecm.platform.gwt.client.ui.old;

import org.nuxeo.ecm.platform.gwt.client.Framework;
import org.nuxeo.ecm.platform.gwt.client.ui.login.LogoutCommand;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class LogoutWidget extends Composite implements ClickListener {

    protected Label username;
    
    public LogoutWidget() {
        Hyperlink submit = new Hyperlink();
        submit.setText("Logout");
        submit.addClickListener(this);
        VerticalPanel panel =  new VerticalPanel();
        panel.setSpacing(2);
        String uname = Framework.getUserName();
        if (uname == null) {
            uname = "...";
        }
        username = new Label("You are logged in as "+uname);
        panel.add(username);
        panel.add(submit);
        initWidget(panel);
    }
    
    public void onClick(Widget sender) {
        new LogoutCommand().execute();
    }
    
    public void refresh() {
        username.setText("You are logged in as "+Framework.getUserName());
    }
    
}
