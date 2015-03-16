package org.nuxeo.elasticsearch.http.readonly;/*
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
 *
 * Contributors:
 *     Benoit Delbosc
 */

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * @since 7.3
 */
public class HttpClient {
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
        org.apache.http.client.methods.HttpGet httpget = new org.apache.http.client.methods.HttpGet(url);
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
        StringEntity myEntity = new StringEntity(payload, ContentType.create("application/x-www-form-urlencoded",
                "UTF-8"));
        e.setEntity(myEntity);
        try (CloseableHttpResponse response = client.execute(e)) {
            HttpEntity entity = response.getEntity();
            return entity != null ? EntityUtils.toString(entity) : null;
        }
    }
}
