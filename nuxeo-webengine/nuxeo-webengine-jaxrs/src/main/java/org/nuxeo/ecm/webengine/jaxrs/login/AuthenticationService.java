/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.webengine.jaxrs.login;

import java.util.ArrayList;
import java.util.Map;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class AuthenticationService {

    protected Map<String, AuthenticationHandler> handlers;


    public void addHander(AuthenticationHandlerDescriptor desc) throws Exception {
        handlers.put(desc.name, desc.newInstance());
    }

    public void removeHander(AuthenticationHandlerDescriptor desc) {
        handlers.remove(desc.name);
    }

    public void addHandler(String name, AuthenticationHandler handler) {
        handlers.put(name, handler);
    }

    public AuthenticationHandler removeHandler(String key) {
        return handlers.remove(key);
    }

    public AuthenticationHandler getHandler(String name) {
        return handlers.get(name);
    }

    /**
     * Create a handler instance for the given comma separated list of handler names.
     * @param names
     * @return
     */
    public AuthenticationHandler createHandler(String names) {
        int i = names.indexOf(',');
        if (i == -1) {
            AuthenticationHandler handler = handlers.get(names.trim());
            return handler;
        }
        int s = 0;
        ArrayList<AuthenticationHandler> result = new ArrayList<AuthenticationHandler>();
        do {
            String name = names.substring(s, i).trim();
            AuthenticationHandler handler = getHandler(name);
            result.add(handler);
            s = i+1;
            i = names.indexOf(',', s);
        } while (i > -1);
        if (s < names.length()) {
            String name = names.substring(s).trim();
            if (name.length() > 0) {
                AuthenticationHandler handler = getHandler(name);
                result.add(handler);
            }
        }
        return new CompositeAuthenticationHandler(result.toArray(new AuthenticationHandler[result.size()]));
    }

}
