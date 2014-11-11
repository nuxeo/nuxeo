/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.client;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultLoginHandler implements LoginHandler {

    private LoginContext lc;

    private String username;
    private char[] password;

    public DefaultLoginHandler() {
    }

    public DefaultLoginHandler(String username, String password) {
        this (username, password.toCharArray());
    }

    public DefaultLoginHandler(String username, char[] password) {
        this.username = username;
        this.password = password;
    }

    /**
     * @param username the username to set.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @param password the password to set.
     */
    public void setPassword(char[] password) {
        this.password = password;
    }

    public void setPassword(String password) {
        this.password = password.toCharArray();
    }

    public char[] getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public synchronized LoginContext getLoginContext() {
        return lc;
    }

    public synchronized LoginContext login() throws LoginException {
        if (username == null) {
            lc = Framework.login();
        } else {
            lc = Framework.login(username, password);
        }
        return lc;
    }

    public synchronized void logout() throws LoginException {
        if (lc != null) {
            lc.logout();
        }
    }

    public synchronized void retryLogin() throws LoginException {
        if (lc != null) {
            lc.logout();
        }
        login();
    }

}
