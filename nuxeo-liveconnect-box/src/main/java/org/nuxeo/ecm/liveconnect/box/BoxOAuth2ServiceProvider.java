/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.liveconnect.box;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.oauth2.providers.AbstractOAuth2UserEmailProvider;
import org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonObjectParser;

/**
 * @since 8.1
 */
public class BoxOAuth2ServiceProvider extends AbstractOAuth2UserEmailProvider {

    protected static final Log log = LogFactory.getLog(BoxOAuth2ServiceProvider.class);

    private static final String ACCOUNT_INFO_URL = "https://api.box.com/2.0/users/me";

    private static final HttpRequestFactory requestFactory =
            HTTP_TRANSPORT.createRequestFactory(request -> request.setParser(new JsonObjectParser(JSON_FACTORY)));

    @Override
    protected String getUserEmail(String accessToken) throws IOException {
        HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(ACCOUNT_INFO_URL));
        request.setHeaders(new HttpHeaders().setAuthorization("Bearer " + accessToken));

        HttpResponse response = request.execute();
        GenericJson json = response.parseAs(GenericJson.class);
        return json.get("login").toString();
    }

    public String getServiceUser(String username) {
        Map<String, Serializable> filter = new HashMap<>();
        filter.put("serviceName", serviceName);
        filter.put(NuxeoOAuth2Token.KEY_NUXEO_LOGIN, username);
        List<DocumentModel> entries = getCredentialDataStore().query(filter);
        if (entries == null || entries.size() == 0) {
            return null;
        }
        if (entries.size() > 1) {
            log.error("Found multiple " + serviceName + " accounts for " + username);
        }
        return (String) entries.get(0).getProperty(NuxeoOAuth2Token.SCHEMA, NuxeoOAuth2Token.KEY_SERVICE_LOGIN);
    }

    protected HttpRequestFactory getRequestFactory() {
        return requestFactory;
    }

}
