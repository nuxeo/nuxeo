/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.ecm.platform.oauth2.tokens;

import java.util.List;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.oauth2.enums.NuxeoOAuth2TokenType;

/**
 * Manages oAuth2 tokens. A token can be:
 * <ul>
 * <li>Provided by Nuxeo, it's the oAuth2 server provider.</li>
 * <li>Consumed by Nuxeo, it's a client of another server provider.</li>
 * </ul>
 *
 * @since 11.1
 */
public interface OAuth2TokenService {

    /**
     * Gets the oAuth2 tokens as the given principal.
     *
     * @return the oAuth2 tokens
     * @throws NuxeoException with 403 status code if the given principal doesn't have access to the oAuth2 tokens
     */
    List<NuxeoOAuth2Token> getTokens(NuxeoPrincipal principal);

    /**
     * Gets the OAuth2 tokens for the given user.
     *
     * @param nxuser the nuxeo user
     * @return the oAuth2 tokens that match the given user
     * @throws NullPointerException if {@code nxuser} is {@code null}
     */
    List<NuxeoOAuth2Token> getTokens(String nxuser);

    /**
     * Gets the oAuth2 tokens for a given type as the given principal.
     **
     * @param type the token type {@link NuxeoOAuth2TokenType}
     * @return the oAuth2 tokens that match the type
     * @throws NullPointerException if {@code type} is {@code null}
     * @throws NuxeoException with 403 status code if the given principal doesn't have access to the oAuth2 tokens
     */
    List<NuxeoOAuth2Token> getTokens(NuxeoOAuth2TokenType type, NuxeoPrincipal principal);

    /**
     * Gets the OAuth2 tokens from a given user name and a type.
     *
     * @param nxuser the nuxeo user
     * @param type the token type {@link NuxeoOAuth2TokenType}
     * @return the oAuth2 tokens
     * @throws NullPointerException if {@code nxuser} or {@code type} is {@code null}
     */
    List<NuxeoOAuth2Token> getTokens(String nxuser, NuxeoOAuth2TokenType type);

    /**
     * Finds the oAuth2 tokens that match the given {@code query} as the given principal. To be retrieved a token should
     * match the {@code query} anywhere in the string value of the fields {@link NuxeoOAuth2Token#KEY_NUXEO_LOGIN},
     * {@link NuxeoOAuth2Token#KEY_SERVICE_NAME}
     *
     * @param query the query to match
     * @return the oAuth2 tokens that match the {@code query}
     * @throws NuxeoException with 403 status code if the given principal doesn't have access to the oAuth2 tokens
     */
    List<NuxeoOAuth2Token> search(String query, NuxeoPrincipal principal);

}
