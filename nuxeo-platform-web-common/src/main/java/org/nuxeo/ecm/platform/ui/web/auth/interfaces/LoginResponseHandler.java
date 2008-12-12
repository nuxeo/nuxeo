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

package org.nuxeo.ecm.platform.ui.web.auth.interfaces;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Interface for services that knows how to handle login responses.
 * This was specially introduced to extend {@link NuxeoAuthenticationPlugin} interface
 * to add login response handling capabilities to existing authenticators.
 * <p>
 * This interface should be implemented by {@link NuxeoAuthenticationPlugin} instances that needs
 * full control over the login response.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface LoginResponseHandler {

    /**
     * Handles the login error response.
     *
     * @param request the http request
     * @param response the http response
     * @return true if error was handled, false otherwise
     */
    boolean onError(HttpServletRequest request, HttpServletResponse response);

    /**
     * Handles login success response.
     *
     * @param request
     * @param response
     * @return true if response was handled, false otherwise
     */
    boolean onSuccess(HttpServletRequest request, HttpServletResponse response);

}
