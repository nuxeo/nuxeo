/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *      Andre Justo
 */
package org.nuxeo.ecm.liveconnect.dropbox;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.liveconnect.core.AbstractLiveConnectOAuth2ServiceProvider;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonObjectParser;

/**
 * @since 7.3
 */
public class DropboxOAuth2ServiceProvider extends AbstractLiveConnectOAuth2ServiceProvider {

    private static final String ACCOUNT_INFO_URL = "https://api.dropbox.com/1/account/info";

    private static final HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(
            request -> request.setParser(new JsonObjectParser(JSON_FACTORY)));

    @Override
    protected String getUserEmail(String accessToken) throws IOException {
        GenericUrl url = new GenericUrl(ACCOUNT_INFO_URL);
        url.set("access_token", accessToken);

        HttpResponse response = requestFactory.buildGetRequest(url).execute();
        GenericJson json = response.parseAs(GenericJson.class);
        return json.get("email").toString();
    }

    protected HttpRequestFactory getRequestFactory() {
        return requestFactory;
    }
}
