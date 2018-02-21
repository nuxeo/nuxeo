/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.web.common.requestcontroller.service;

import java.util.Map;

import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;

import com.thetransactioncompany.cors.CORSFilter;

/**
 * Interface for the service used to manage what {@link HttpServletRequest} must be protected by the Filter.
 *
 * @author tiry
 */
public interface RequestControllerManager {

    RequestFilterConfig getConfigForRequest(HttpServletRequest request);

    /**
     * Get contributed CORS Filter for an HttpServletRequest.
     *
     * @return the CORS filter if there is a matching request, otherwise {@code null}
     * @since 10.1
     */
    CORSFilter getCorsFilterForRequest(HttpServletRequest request);

    /**
     * Get contributed FilterConfig for an HttpServletRequest.
     *
     * @return filter config to init CorsFilter if there is a matching request, null otherwise.
     * @since 5.7.2
     * @deprecated since 10.1, unused, use {@link getCorsFilterForRequest} instead
     */
    @Deprecated
    FilterConfig getCorsConfigForRequest(HttpServletRequest request);

    /**
     * @since 6.0
     * @return a map with the header names to add to the HTTP response with their values
     */
    Map<String, String> getResponseHeaders();
}
