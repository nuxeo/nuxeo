/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */

package org.nuxeo.ecm.platform.oauth2.providers;

import org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProvider} that relies on the user's email as key.
 *
 * @since 7.3
 */
public abstract class AbstractOAuth2UserEmailProvider extends NuxeoOAuth2ServiceProvider {

    @Override
    protected String getOrCreateServiceUser(HttpServletRequest request, String accessToken) throws IOException {
        String email = getUserEmail(accessToken);
        String userId = getServiceUserId(email);
        if (userId == null) {
            String nuxeoLogin = request.getUserPrincipal().getName();
            Map<String, Object> fields = new HashMap<>();
            fields.put(NuxeoOAuth2Token.KEY_SERVICE_LOGIN, email);
            userId = getServiceUserStore().store(nuxeoLogin, fields);
        }
        return userId;
    }

    @Override
    protected String getServiceUserId(String email) {
        Map<String, Serializable> filter = new HashMap<>();
        filter.put(NuxeoOAuth2Token.KEY_SERVICE_LOGIN, email);
        return getServiceUserStore().find(filter);
    }

    protected abstract String getUserEmail(String accessToken) throws IOException;
}
