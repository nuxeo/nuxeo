/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.webengine.jaxrs.login;

import java.util.ArrayList;
import java.util.Map;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class AuthenticationService {

    protected Map<String, AuthenticationHandler> handlers;

    public void addHander(AuthenticationHandlerDescriptor desc) throws ReflectiveOperationException {
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
     *
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
            s = i + 1;
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
