/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
