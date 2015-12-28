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
 *
 * Contributors:
 *     Benoit Delbosc
 */
package org.nuxeo.elasticsearch.http.readonly;

import java.io.IOException;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * Http client that handle GET request with a body.
 *
 * @since 7.3
 */
public class HttpClient {
    private final static String UTF8_CHARSET = "UTF-8";

    private static class HttpGetWithEntity extends HttpPost {
        public final static String METHOD_NAME = "GET";

        public HttpGetWithEntity(String url) {
            super(url);
        }

        @Override
        public String getMethod() {
            return METHOD_NAME;
        }
    }

    public static String get(String url) throws IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpget = new HttpGet(url);
        try (CloseableHttpResponse response = client.execute(httpget)) {
            HttpEntity entity = response.getEntity();
            return entity != null ? EntityUtils.toString(entity) : null;
        }
    }

    public static String get(String url, String payload) throws IOException {
        if (payload == null) {
            return get(url);
        }
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGetWithEntity e = new HttpGetWithEntity(url);
        StringEntity myEntity = new StringEntity(payload, ContentType.create(MediaType.APPLICATION_FORM_URLENCODED,
                UTF8_CHARSET));
        e.setEntity(myEntity);
        try (CloseableHttpResponse response = client.execute(e)) {
            HttpEntity entity = response.getEntity();
            return entity != null ? EntityUtils.toString(entity) : null;
        }
    }
}
