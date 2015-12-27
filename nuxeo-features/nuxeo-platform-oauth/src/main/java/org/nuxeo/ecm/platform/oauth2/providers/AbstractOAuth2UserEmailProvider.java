/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
