/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.shibboleth.service;

import com.google.common.collect.BiMap;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public interface ShibbolethAuthenticationService {

    /**
     * Returns the computed login URL to Shibboleth , or {@code null} if no login URL is configured.
     *
     * @param redirect URL
     * @return login URL
     */
    String getLoginURL(String redirectURL);

    /**
     * Returns the computed logout URL to Shibboleth, or {@code null} if no logout URL is configured.
     *
     * @param redirect URL
     * @return logout URL
     */
    String getLogoutURL(String redirectURL);

    /**
     * Returns the computed login URL to Shibboleth , or {@code null} if no login URL is configured.
     *
     * @param HTTP request
     * @return login URL
     */
    String getLoginURL(HttpServletRequest request);

    /**
     * Returns the computed logout URL to Shibboleth, or {@code null} if no logout URL is configured.
     *
     * @param HTTP request
     * @return logout URL
     */
    String getLogoutURL(HttpServletRequest request);

    /**
     * Returns the user ID based on the source IdP. In the configuration is defined which HTTP header is used for each
     * registered IdP.
     *
     * @param HTTP request
     * @return user ID
     */
    String getUserID(HttpServletRequest httpRequest);

    /**
     * Returns a map of the user metadata based on the configuration. Keys are the field names and values coming from
     * the HTTP headers.
     *
     * @param HTTP request
     * @return metadata map
     */
    Map<String, Object> getUserMetadata(String idField, HttpServletRequest httpRequest);

    /**
     * Returns a bi-map of the user metadata {response headers, values} based on the configuration.
     *
     * @since 8.3
     */
    BiMap<String, String> getUserMetadata();

}
