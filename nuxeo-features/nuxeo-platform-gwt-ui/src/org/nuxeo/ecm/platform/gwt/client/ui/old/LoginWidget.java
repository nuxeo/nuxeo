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
