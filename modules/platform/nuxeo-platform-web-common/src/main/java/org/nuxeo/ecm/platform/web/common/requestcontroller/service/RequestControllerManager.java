/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
