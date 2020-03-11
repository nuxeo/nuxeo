/*
 * (C) Copyright 2010-2019 Nuxeo (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.shibboleth.service;

import com.google.common.collect.BiMap;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public interface ShibbolethAuthenticationService {

    /**
     * Returns the computed login URL to Shibboleth , or {@code null} if no login URL is configured.
     */
    String getLoginURL(String redirectURL);

    /**
     * Returns the computed logout URL to Shibboleth, or {@code null} if no logout URL is configured.
     */
    String getLogoutURL(String redirectURL);

    /**
     * Returns the computed login URL to Shibboleth , or {@code null} if no login URL is configured.
     */
    String getLoginURL(HttpServletRequest request);

    /**
     * Returns the computed logout URL to Shibboleth, or {@code null} if no logout URL is configured.
     */
    String getLogoutURL(HttpServletRequest request);

    /**
     * Returns the user ID based on the source IdP. In the configuration is defined which HTTP header is used for each
     * registered IdP.
     */
    String getUserID(HttpServletRequest httpRequest);

    /**
     * Returns a map of the user metadata based on the configuration. Keys are the field names and values coming from
     * the HTTP headers.
     */
    Map<String, Object> getUserMetadata(String idField, HttpServletRequest httpRequest);

    /**
     * Returns a bi-map of the user metadata {response headers, values} based on the configuration.
     *
     * @since 8.3
     */
    BiMap<String, String> getUserMetadata();

}
