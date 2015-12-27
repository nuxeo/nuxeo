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

/**
 * Initiate an authentication for the given HTTP request. Implementations are responsible to detect whether the request
 * contains any known authentication data and perform the authentication if needed.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface AuthenticationHandler {

    /**
     * Initialize this handler given a property map.
     *
     * @param properties
     */
    void init(Map<String, String> properties);

    /**
     * Handle the authentication if the request contains any known authentication data. If authentication was done
     * returns the resulting LoginContext otherwise returns null. If authentication failed throws {@link LoginException}
     * and the implementation <b>must</b> finish the request by correctly responding to the client or redirecting to
     * another page - through the given response object.
     *
     * @param request the http request
     * @param response the http response
     * @return the loginc context if successful, or null if login was not handled.
     * @throws LoginException if authentication failed.
     */
    LoginContext handleAuthentication(HttpServletRequest request, HttpServletResponse response) throws LoginException;

}
