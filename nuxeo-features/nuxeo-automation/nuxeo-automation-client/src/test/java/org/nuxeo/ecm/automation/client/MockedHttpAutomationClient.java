/*
 * (C) Copyright 2013-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.client;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HttpContext;
import org.mockito.Mockito;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpConnector;
import org.nuxeo.ecm.automation.client.jaxrs.spi.AbstractAutomationClient;
import org.nuxeo.ecm.automation.client.jaxrs.spi.Connector;
import org.nuxeo.ecm.automation.client.model.OperationDocumentation;
import org.nuxeo.ecm.automation.client.model.OperationRegistry;

/**
 * @author dmetzler
 */
public class MockedHttpAutomationClient extends AbstractAutomationClient {

    private HttpClient http;

    public MockedHttpAutomationClient(String url) {
        super(url);
        init();
    }

    private void init() {

        registry = mock(OperationRegistry.class);
        http = mock(HttpClient.class);

    }

    public void setResponse(String contentType, String responseBody) {

        http = mock(HttpClient.class);

        try {
            Mockito.when(http.execute(any(HttpUriRequest.class), any(HttpContext.class))).thenReturn(
                    prepareResponse(200, responseBody, contentType));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private HttpResponse prepareResponse(int expectedResponseStatus, String expectedResponseBody, String contentType) {
        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1),
                expectedResponseStatus, ""));
        response.setStatusCode(expectedResponseStatus);
        response.setHeader("Content-Type", contentType);
        try {
            StringEntity entity = new StringEntity(expectedResponseBody);
            entity.setContentType(contentType);
            response.setEntity(entity);

        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
        return response;
    }

    @Override
    protected Session login(Connector connector) {
        return createSession(connector, new LoginInfo("Administrator"));
    }

    @Override
    public synchronized void shutdown() {
        http = null;
    }

    class MockedOperationDocumentation extends OperationDocumentation {

        private static final long serialVersionUID = 1L;

        public MockedOperationDocumentation(String operationId) {
            super(operationId);
        }

        public void withParams(String... paramIds) {
            for (String paramId : paramIds) {
                Param p = new Param();
                p.name = paramId;
                params.add(p);
            }
        }
    }

    public MockedOperationDocumentation addOperation(String opeationId) {
        MockedOperationDocumentation opDoc = new MockedOperationDocumentation(opeationId);
        when(registry.getOperation(opeationId)).thenReturn(opDoc);
        return opDoc;
    }

    @Override
    protected Connector newConnector() {
        return new HttpConnector(http, 0);
    }
}
