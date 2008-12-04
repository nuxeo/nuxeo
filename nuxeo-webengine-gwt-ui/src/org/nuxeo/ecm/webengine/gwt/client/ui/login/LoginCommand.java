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

package org.nuxeo.ecm.webengine.gwt.client.ui.login;

import org.nuxeo.ecm.webengine.gwt.client.UI;
import org.nuxeo.ecm.webengine.gwt.client.http.HttpResponse;
import org.nuxeo.ecm.webengine.gwt.client.ui.ContextListener;
import org.nuxeo.ecm.webengine.gwt.client.ui.HttpCommand;
import org.nuxeo.ecm.webengine.gwt.client.ui.View;

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
        UI.getContext().setUsername(username);
        UI.fireEvent(ContextListener.LOGIN);
    }

    @Override
    public void onFailure(Throwable cause) {
        Window.alert("login fail!");
    }

}
