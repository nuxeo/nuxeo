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

import org.nuxeo.ecm.platform.gwt.client.ui.login.LoginCommand;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class LoginWidget extends Composite implements ClickListener {

    protected TextBox  userBox;
    protected PasswordTextBox  passBox;

    public LoginWidget() {
        userBox = new TextBox();
        passBox = new PasswordTextBox();
        Button submit = new Button("Login");
        submit.addClickListener(this);
        VerticalPanel panel = new VerticalPanel();
        panel.add(new Label("Username:"));
        panel.add(userBox);
        panel.add(new Label("Password:"));
        panel.add(passBox);
        panel.add(submit);
        panel.setSpacing(2);
        initWidget(panel);

        userBox.setText("Administrator");
        passBox.setText("Administrator");
    }

    public void onClick(Widget sender) {
        new LoginCommand(userBox.getText().trim(), passBox.getText()).execute();
    }

}
