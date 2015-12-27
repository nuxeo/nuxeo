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

package org.nuxeo.ecm.platform.gwt.client.ui.login;

import org.nuxeo.ecm.platform.gwt.client.http.HttpResponse;
import org.nuxeo.ecm.platform.gwt.client.ui.HttpCommand;
import org.nuxeo.ecm.platform.gwt.client.ui.View;

import com.google.gwt.user.client.Window;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class LoginCommand extends HttpCommand {

    protected String username;
    protected String password;

    public LoginCommand(String username, String password) {
        this (null, username, password);
    }

    public LoginCommand(View view, String username, String password) {
        super(view, 100);
        this.username = username;
        this.password = password;
    }

    @Override
    protected void doExecute() throws Throwable {
        post("/login/")
            .setHeader("Content-Type", "application/x-www-form-urlencoded")
            .setRequestData("caller=login&username="+username+"&password="+password)
            .send();
    }

    @Override
    public void onSuccess(HttpResponse response) {
        Window.Location.reload();
    }

    @Override
    public void onFailure(Throwable cause) {
        Window.alert("login failed!");
    }

}
