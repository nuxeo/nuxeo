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

package org.nuxeo.ecm.webengine.gwt.client;

import java.util.HashMap;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Context extends HashMap<String,String> {

    private static final long serialVersionUID = 1L;

    protected String username;
    protected Object input;
    
    public String getUsername() {
        return username;
    }
    
    /**
     * @param username the username to set.
     */
    public void setUsername(String username) {
        if (this.username != username) {
            this.username = username;
            Application.fireEvent(username == null ? ContextListener.LOGOUT : ContextListener.LOGIN);
        }
    }
    
    public Object getInputObject() {
        return input;
    }
    
    /**
     * @param object the object to set.
     */
    public void setInputObject(Object object) {
        if (this.input != object) {
            this.input = object;
            Application.fireEvent(ContextListener.INPUT);
        }
    }
        
}
