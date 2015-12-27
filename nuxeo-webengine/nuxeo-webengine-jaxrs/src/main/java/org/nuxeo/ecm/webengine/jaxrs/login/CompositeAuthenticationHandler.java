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

import java.util.Map;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.webengine.jaxrs.BundleNotFoundException;
import org.nuxeo.ecm.webengine.jaxrs.Utils;

/**
 * An authentication handlers that delegate the authentication to the first registered handler that knows how to
 * authenticate.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class CompositeAuthenticationHandler implements AuthenticationHandler {

    protected AuthenticationHandler[] handlers;

    public CompositeAuthenticationHandler(String classRefs) throws ReflectiveOperationException,
            BundleNotFoundException {
        handlers = Utils.newInstances(AuthenticationHandler.class, classRefs);
    }

    public CompositeAuthenticationHandler(AuthenticationHandler[] handlers) {
        this.handlers = handlers;
    }

    @Override
    public void init(Map<String, String> properties) {
        // do nothing
    }

    @Override
    public LoginContext handleAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws LoginException {
        for (AuthenticationHandler handler : handlers) {
            LoginContext lc = handler.handleAuthentication(request, response);
            if (lc != null) {
                return lc;
            }
        }
        return null;
    }

}
