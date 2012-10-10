/*
 * (C) Copyright 2006-20012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.tokenauth.service;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.ui.web.auth.token.TokenAuthenticator;
import org.nuxeo.ecm.tokenauth.TokenAuthenticationException;
import org.nuxeo.ecm.tokenauth.servlet.TokenAuthenticationServlet;

/**
 * Service to manage generation and storage of authentication tokens. Each token
 * must be unique and persisted in the back-end with the user information it is
 * bound to: user name, application name, device name, permission.
 * <p>
 * Typically, the service is called by the {@link TokenAuthenticationServlet} to
 * get a token from the user information passed as request parameters, and it
 * allows the {@link TokenAuthenticator} to check for a valid identity given a
 * token passed as a request header.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.7
 */
public interface TokenAuthenticationService extends Serializable {

    /**
     * Gets a unique token for the specified user, application, and device.
     * <p>
     * If such a token exist in the back-end for the specified (userName,
     * applicationName, deviceName) triplet, just returns it, else generates it
     * and stores it in the back-end with the triplet attributes and the
     * specified permission.
     *
     * @throws TokenAuthenticationException if one of the parameters is null or
     *             empty as they are all mandatory
     * @throws ClientRuntimeException if multiple tokens are found for the same
     *             triplet
     *
     */
    String getToken(String userName, String applicationName, String deviceName,
            String permission) throws TokenAuthenticationException;

    /**
     * Gets the user name bound to the specified token.
     *
     * @return The user name bound to the specified token, or null if the token
     *         does not exist in the back-end.
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

}