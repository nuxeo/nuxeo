/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.tokenauth;

import java.io.IOException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;

/**
 * Helper for token authentication test cases.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.7
 */
public final class TokenAuthenticationTestHelper {

    /**
     * Executes the specified HTTP method on the specified HTTP client with a
     * basic authentication header given the specified credentials.
     */
    public static final int executeGetMethod(HttpClient httpClient,
            HttpMethod httpMethod, String userName, String password)
            throws HttpException, IOException {

        String authString = userName + ":" + password;
        String basicAuthHeader = "Basic "
                + new String(Base64.encodeBase64(authString.getBytes()));
        httpMethod.setRequestHeader("Authorization", basicAuthHeader);
        return httpClient.executeMethod(httpMethod);
    }
}
