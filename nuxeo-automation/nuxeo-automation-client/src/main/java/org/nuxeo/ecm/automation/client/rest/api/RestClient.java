/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.automation.client.rest.api;

import org.apache.http.impl.client.BasicCookieStore;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;

import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.client.apache4.ApacheHttpClient4;
import com.sun.jersey.client.apache4.ApacheHttpClient4Handler;

/**
 * REST client used to to requests on the Nuxeo REST API.
 *
 * @since 5.8
 */
public class RestClient {

    protected static final String SITE_AUTOMATION_PATH = "/site/automation";

    protected static final String API_PATH = "/api/v1";

    protected WebResource service;

    public RestClient(HttpAutomationClient httpAutomationClient) {
        ApacheHttpClient4Handler handler = new ApacheHttpClient4Handler(
                httpAutomationClient.http(), new BasicCookieStore(), false);
        ApacheHttpClient4 client = new ApacheHttpClient4(handler);
        client.addFilter(httpAutomationClient.getRequestInterceptor());

        String apiURL = httpAutomationClient.getBaseUrl();
        apiURL = apiURL.replace(SITE_AUTOMATION_PATH, API_PATH);
        service = client.resource(apiURL);
    }

    public RestRequest newRequest(String path) {
        return new RestRequest(service, path);
    }

}
