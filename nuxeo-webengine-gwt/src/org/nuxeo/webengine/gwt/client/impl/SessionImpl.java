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

package org.nuxeo.webengine.gwt.client.impl;

import org.nuxeo.webengine.gwt.client.Session;

import com.google.gwt.core.client.GWT;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SessionImpl implements Session {

    protected String username;
    protected Object input;
    
    public boolean setInput(String url) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean login(String username, String password) {
        this.username = username;
        GWT.log("setting username "+username, null);
        return true;
    }

    public boolean logout() {
        username = null; // TODO temp code 
        return true;
    }

    public Object getInput() {
        return input;
    }
    
    public String getUsername() {
        GWT.log("getting username "+username, null);
        return username;
    }
    
    public boolean isAuthenticated() {
        return username != null;
    }
    
}
