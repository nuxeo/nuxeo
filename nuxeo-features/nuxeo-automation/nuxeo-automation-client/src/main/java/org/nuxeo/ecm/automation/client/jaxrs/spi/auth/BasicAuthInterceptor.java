/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.jaxrs.spi.auth;

import static java.nio.charset.StandardCharsets.UTF_8;

import javax.ws.rs.core.HttpHeaders;

import org.apache.commons.codec.binary.Base64;
import org.nuxeo.ecm.automation.client.jaxrs.spi.Connector;
import org.nuxeo.ecm.automation.client.jaxrs.spi.Request;
import org.nuxeo.ecm.automation.client.jaxrs.spi.RequestInterceptor;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;

/**
 * Inject the basic authentication header in the request.
 *
 * @author matic
 */
public class BasicAuthInterceptor extends RequestInterceptor {

    protected String token;

    public BasicAuthInterceptor(String username, String password) {
        setAuth(username, password);
    }

    public void setAuth(String username, String password) {
        String info = username + ":" + password;
        token = "Basic " + Base64.encodeBase64String(info.getBytes(UTF_8));
    }

    @Override
    public void processRequest(Request request, Connector connector) {
        request.put(HttpHeaders.AUTHORIZATION, token);
    }

    @Override
    public ClientResponse handle(ClientRequest cr) throws ClientHandlerException {
        if (!cr.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            cr.getHeaders().add(HttpHeaders.AUTHORIZATION, token);
        }
        return getNext().handle(cr);
    }
}
