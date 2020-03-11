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
 */

package org.nuxeo.ecm.platform.ui.web.auth.interfaces;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Interface for services that knows how to handle login responses. This was specially introduced to extend
 * {@link NuxeoAuthenticationPlugin} interface to add login response handling capabilities to existing authenticators.
 * <p>
 * This interface should be implemented by {@link NuxeoAuthenticationPlugin} instances that needs full control over the
 * login response.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface LoginResponseHandler {

    /**
     * Handles the login error response.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @return true if error was handled, false otherwise
     */
    boolean onError(HttpServletRequest request, HttpServletResponse response);

    /**
     * Handles login success response.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @return true if response was handled, false otherwise
     */
    boolean onSuccess(HttpServletRequest request, HttpServletResponse response);

}
