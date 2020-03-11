/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.tokenauth.service;

import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.ui.web.auth.token.TokenAuthenticator;
import org.nuxeo.ecm.tokenauth.TokenAuthenticationException;
import org.nuxeo.ecm.tokenauth.servlet.TokenAuthenticationServlet;

/**
 * Service to manage generation and storage of authentication tokens. Each token must be unique and persisted in the
 * back-end with the user information it is bound to: user name, application name, device name, device description,
 * permission.
 * <p>
 * Typically, the service is called by the {@link TokenAuthenticationServlet} to get a token from the user information
 * passed as request parameters, and it allows the {@link TokenAuthenticator} to check for a valid identity given a
 * token passed as a request header.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.7
 */
public interface TokenAuthenticationService extends Serializable {

    /**
     * Acquires a unique token for the specified user, application, and device.
     * <p>
     * If such a token exist in the back-end for the specified (userName, applicationName, deviceId) triplet, just
     * returns it, else generates it and stores it in the back-end with the triplet attributes, the specified device
     * description and permission.
     *
     * @throws TokenAuthenticationException if one of the required parameters is null or empty (all parameters are
     *             required except for the device description)
     * @throws NuxeoException if multiple tokens are found for the same triplet
     */
    String acquireToken(String userName, String applicationName, String deviceId, String deviceDescription,
            String permission) throws TokenAuthenticationException;

    /**
     * Acquires a unique token for the specified request.
     * <p>
     * Parameters needed (applicationName, deviceId, deviceDescription, permission) to acquire the token are extracted
     * from the request itself.
     * <p>
     * If such a token exist in the back-end for the specified (userName, applicationName, deviceId) triplet, just
     * returns it, else generates it and stores it in the back-end with the triplet attributes, the specified device
     * description and permission.
     *
     * @return a token or null for no principal or for anonymous principal unless 'allowAnonymous' parameter is
     *         explicitly set to true in the authentication plugin configuration.
     * @throws TokenAuthenticationException if one of the required parameters is null or empty (all parameters are
     *             required except for the device description)
     * @throws NuxeoException if multiple tokens are found for the same triplet
     * @since 8.3
     */
    String acquireToken(HttpServletRequest request) throws TokenAuthenticationException;

    /**
     * Gets the token for the specified user, application, and device.
     *
     * @return null if such a token doesn't exist
     * @throws TokenAuthenticationException if one of the required parameters is null or empty (all parameters are
     *             required except for the device description)
     * @throws NuxeoException if multiple tokens are found for the same (userName, applicationName, deviceId) triplet
     */
    String getToken(String userName, String applicationName, String deviceId) throws TokenAuthenticationException;

    /**
     * Gets the user name bound to the specified token.
     *
     * @return The user name bound to the specified token, or null if the token does not exist in the back-end.
     */
    String getUserName(String token);

    /**
     * Removes the token from the back-end.
     */
    void revokeToken(String token);

    /**
     * Gets the token bindings for the specified user.
     */
    DocumentModelList getTokenBindings(String userName);

    /**
     * Gets the token bindings for the specified user and application.
     * @since 8.3
     */
    DocumentModelList getTokenBindings(String userName, String applicationName);

}
